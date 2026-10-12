"""Ekstraksi nota: foto -> keluaran model -> Nota + daftar isu.

Alur:
  1. Klien model menghasilkan JSON (lihat prompt.py). Klien bisa ditiru untuk tes.
  2. JSON dipetakan ke `Nota`. Teks angka ditafsirkan oleh `parse_rupiah`/`parse_qty`.
     Angka yang tidak bisa ditafsirkan menjadi None plus isu FORMAT_TIDAK_DIKENALI;
     tidak ada yang ditebak.
  3. `validasi` memeriksa konsistensi hitungan.

Modul ini dipakai oleh API (api.py) DAN harness evaluasi (nota_eval), sehingga yang diukur
evaluasi adalah kode yang sama persis dengan yang melayani aplikasi.
"""

from __future__ import annotations

import json
import logging
import re
import time
from collections.abc import Callable
from dataclasses import dataclass
from decimal import Decimal
from enum import StrEnum
from typing import Any, Protocol

from .money import QtyParseError, RupiahParseError, parse_qty, parse_rupiah
from .pricing import biaya_usd
from .prompt import PROMPT, SKEMA_KELUARAN, VERSI_PROMPT
from .schema import ItemBaris, Nota
from .validator import Isu, Kode, Tingkat, validasi

log = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Kontrak klien model
# ---------------------------------------------------------------------------


@dataclass(frozen=True)
class Pemakaian:
    token_input: int = 0
    token_output: int = 0
    token_thinking: int = 0

    def __add__(self, lain: Pemakaian) -> Pemakaian:
        return Pemakaian(
            self.token_input + lain.token_input,
            self.token_output + lain.token_output,
            self.token_thinking + lain.token_thinking,
        )


@dataclass(frozen=True)
class KeluaranModel:
    teks: str
    pemakaian: Pemakaian


class ModelTimeout(Exception):
    """Model tidak menjawab dalam batas waktu."""


class ModelSibuk(Exception):
    """Kuota habis (429) atau server model sedang bermasalah (5xx)."""


class ModelGagal(Exception):
    """Galat lain dari layanan model (misalnya permintaan ditolak)."""


class KlienModel(Protocol):
    nama_model: str

    def hasilkan(self, gambar: bytes, mime: str) -> KeluaranModel:
        """Kirim gambar dan prompt, kembalikan teks JSON beserta pemakaian token.

        Wajib melempar ModelTimeout / ModelSibuk / ModelGagal untuk kegagalan layanan.
        """
        ...


# ---------------------------------------------------------------------------
# Hasil dan kegagalan
# ---------------------------------------------------------------------------


class JenisGagal(StrEnum):
    WAKTU_HABIS = "waktu_habis"
    LAYANAN_SIBUK = "layanan_sibuk"
    GAGAL_LAYANAN_AI = "gagal_layanan_ai"
    KELUARAN_TIDAK_VALID = "keluaran_ai_tidak_valid"
    BUKAN_NOTA = "bukan_nota"


class EkstraksiGagal(Exception):
    def __init__(self, jenis: JenisGagal, pesan: str, pemakaian: Pemakaian, model: str):
        super().__init__(pesan)
        self.jenis = jenis
        self.pesan = pesan
        self.pemakaian = pemakaian  # tetap dicatat: kegagalan juga ada biayanya
        self.model = model


@dataclass(frozen=True)
class HasilEkstraksi:
    nota: Nota
    isu: tuple[Isu, ...]
    jenis_nota: str | None  # "cetak" | "tulis" | "campuran" menurut model
    catatan: str | None
    model: str
    pemakaian: Pemakaian
    biaya_usd: float | None
    latensi_ms: int
    percobaan: int
    versi_prompt: str = VERSI_PROMPT

    @property
    def lolos(self) -> bool:
        return not any(i.tingkat is Tingkat.GALAT for i in self.isu)


# ---------------------------------------------------------------------------
# Membaca keluaran model
# ---------------------------------------------------------------------------


class _KeluaranRusak(Exception):
    pass


_PAGAR_KODE = re.compile(r"^```(?:json)?\s*|\s*```$", re.IGNORECASE)


def baca_json(teks: str) -> dict[str, Any]:
    """Ambil objek JSON dari teks model. Toleran terhadap pagar ```json."""
    bersih = _PAGAR_KODE.sub("", (teks or "").strip())
    try:
        data = json.loads(bersih)
    except json.JSONDecodeError as e:
        raise _KeluaranRusak(f"bukan JSON: {e.msg}") from e
    if not isinstance(data, dict):
        raise _KeluaranRusak("JSON bukan objek")
    items = data.get("items", [])
    if items is None:
        items = []
    if not isinstance(items, list) or not all(isinstance(x, dict) for x in items):
        raise _KeluaranRusak("'items' bukan daftar objek")
    data["items"] = items
    return data


def _teks_tampil(nilai: Any) -> str:
    teks = str(nilai)
    return teks if len(teks) <= 30 else teks[:27] + "..."


def _baca_rupiah(nilai: Any, label: str, baris: int | None) -> tuple[int | None, Isu | None]:
    if nilai is None:
        return None, None
    if isinstance(nilai, bool):
        pass  # bool adalah subclass int di Python; tolak
    elif isinstance(nilai, int):
        return nilai, None
    elif isinstance(nilai, float) and nilai.is_integer():
        return int(nilai), None
    elif isinstance(nilai, str):
        if nilai.strip() in ("", "-"):
            return None, None
        try:
            return parse_rupiah(nilai), None
        except RupiahParseError:
            pass
    awalan = f"Baris {baris + 1}: " if baris is not None else ""
    return None, Isu(
        Kode.FORMAT_TIDAK_DIKENALI,
        f"{awalan}{label} '{_teks_tampil(nilai)}' tidak bisa dibaca sebagai angka",
        baris=baris,
    )


def _baca_qty(nilai: Any, baris: int) -> tuple[Decimal | None, Isu | None]:
    if nilai is None:
        return None, None
    if isinstance(nilai, bool):
        pass
    elif isinstance(nilai, (int, float)) and nilai > 0:
        teks = str(nilai)
        try:
            return parse_qty(teks), None
        except QtyParseError:
            pass
    elif isinstance(nilai, str):
        if nilai.strip() in ("", "-"):
            return None, None
        try:
            return parse_qty(nilai), None
        except QtyParseError:
            pass
    return None, Isu(
        Kode.FORMAT_TIDAK_DIKENALI,
        f"Baris {baris + 1}: qty '{_teks_tampil(nilai)}' tidak bisa dibaca sebagai jumlah barang",
        baris=baris,
    )


def _teks_opsional(nilai: Any) -> str | None:
    if nilai is None:
        return None
    teks = str(nilai).strip()
    return teks or None


def petakan(data: dict[str, Any]) -> tuple[Nota, list[Isu]]:
    """Petakan JSON model ke Nota. Tidak pernah melempar karena isi angka yang buruk."""
    isu: list[Isu] = []
    items: list[ItemBaris] = []

    for i, mentah in enumerate(data["items"]):
        nama = _teks_opsional(mentah.get("nama")) or "(tanpa nama)"
        qty, e1 = _baca_qty(mentah.get("qty"), i)
        harga, e2 = _baca_rupiah(mentah.get("harga_satuan"), "harga satuan", i)
        jumlah, e3 = _baca_rupiah(mentah.get("jumlah"), "jumlah", i)
        diskon, e4 = _baca_rupiah(mentah.get("diskon"), "diskon", i)
        isu.extend(e for e in (e1, e2, e3, e4) if e is not None)

        harga_dihitung = False
        harga_tidak_tertulis = mentah.get("harga_satuan") is None and e2 is None
        if harga is None and harga_tidak_tertulis and qty is not None and jumlah is not None:
            hasil_bagi = Decimal(jumlah + abs(diskon or 0)) / qty
            if hasil_bagi == hasil_bagi.to_integral_value():
                harga, harga_dihitung = int(hasil_bagi), True

        items.append(
            ItemBaris(
                nama=nama,
                qty=qty,
                satuan=_teks_opsional(mentah.get("satuan")),
                harga_satuan=harga,
                jumlah=jumlah,
                diskon_baris=abs(diskon or 0),
                harga_dihitung=harga_dihitung,
            )
        )

    def tingkat_nota(kunci: str, label: str) -> int | None:
        nilai, e = _baca_rupiah(data.get(kunci), label, None)
        if e is not None:
            isu.append(e)
        return nilai

    subtotal = tingkat_nota("subtotal", "subtotal")
    diskon = tingkat_nota("diskon", "diskon nota")
    pajak = tingkat_nota("pajak", "pajak")
    biaya_lain = tingkat_nota("biaya_lain", "biaya lain")
    total = tingkat_nota("total", "total")

    nota = Nota(
        items=tuple(items),
        total=total,
        subtotal=subtotal,
        diskon=abs(diskon or 0),
        pajak=pajak or 0,
        biaya_lain=biaya_lain or 0,
        supplier=_teks_opsional(data.get("supplier")),
        tanggal_tertulis=_teks_opsional(data.get("tanggal")),
    )
    return nota, isu


# ---------------------------------------------------------------------------
# Orkestrasi
# ---------------------------------------------------------------------------


def ekstrak(
    gambar: bytes,
    mime: str,
    klien: KlienModel,
    maks_percobaan: int = 2,
    jam: Callable[[], float] = time.monotonic,
) -> HasilEkstraksi:
    """Jalankan ekstraksi penuh. Melempar EkstraksiGagal untuk kegagalan yang tidak bisa dipulihkan.

    Keluaran yang bukan JSON valid dicoba ulang (total `maks_percobaan` panggilan).
    Kegagalan layanan (timeout, 429, 5xx) TIDAK dicoba ulang di sini, karena klien SDK
    sudah punya retry sendiri dan percobaan berlapis melipatgandakan waktu tunggu pengguna.
    """
    mulai = jam()
    pemakaian = Pemakaian()
    model = klien.nama_model
    data: dict[str, Any] | None = None
    alasan_rusak = ""
    percobaan = 0

    for percobaan in range(1, maks_percobaan + 1):
        try:
            keluaran = klien.hasilkan(gambar, mime)
        except ModelTimeout as e:
            raise EkstraksiGagal(
                JenisGagal.WAKTU_HABIS, "Layanan AI tidak menjawab tepat waktu", pemakaian, model
            ) from e
        except ModelSibuk as e:
            raise EkstraksiGagal(JenisGagal.LAYANAN_SIBUK, "Layanan AI sedang sibuk", pemakaian, model) from e
        except ModelGagal as e:
            raise EkstraksiGagal(
                JenisGagal.GAGAL_LAYANAN_AI, "Layanan AI menolak permintaan", pemakaian, model
            ) from e

        pemakaian = pemakaian + keluaran.pemakaian
        try:
            data = baca_json(keluaran.teks)
            break
        except _KeluaranRusak as e:
            alasan_rusak = str(e)
            log.warning("keluaran model rusak (percobaan %d): %s", percobaan, alasan_rusak)

    if data is None:
        raise EkstraksiGagal(
            JenisGagal.KELUARAN_TIDAK_VALID,
            f"Keluaran AI tidak sesuai format ({alasan_rusak})",
            pemakaian,
            model,
        )

    if data.get("terbaca") is False or (not data["items"] and data.get("total") in (None, "")):
        raise EkstraksiGagal(JenisGagal.BUKAN_NOTA, "Foto tidak terbaca sebagai nota", pemakaian, model)

    nota, isu_format = petakan(data)
    hasil_validasi = validasi(nota)

    return HasilEkstraksi(
        nota=nota,
        isu=tuple(isu_format) + hasil_validasi.isu,
        jenis_nota=_teks_opsional(data.get("jenis")),
        catatan=_teks_opsional(data.get("catatan")),
        model=model,
        pemakaian=pemakaian,
        biaya_usd=biaya_usd(model, pemakaian.token_input, pemakaian.token_output, pemakaian.token_thinking),
        latensi_ms=int((jam() - mulai) * 1000),
        percobaan=percobaan,
    )


# ---------------------------------------------------------------------------
# Klien Gemini
# ---------------------------------------------------------------------------


def _jenis_timeout() -> tuple[type[BaseException], ...]:
    jenis: list[type[BaseException]] = [TimeoutError]
    for modul in ("httpx", "httpx2"):
        try:
            mod = __import__(modul)
            jenis.append(mod.TimeoutException)
        except (ImportError, AttributeError):
            pass
    return tuple(jenis)


class KlienGemini:
    """Klien Gemini Developer API lewat SDK google-genai.

    Parameter yang memengaruhi biaya dan akurasi (thinking_level, media_resolution) sengaja
    bisa diatur, karena nilainya diputuskan dari evaluasi, bukan dari dugaan.
    Temperature tidak diatur secara default: panduan Gemini 3 menyarankan memakai nilai bawaan.
    """

    def __init__(
        self,
        api_key: str,
        nama_model: str,
        timeout_detik: float = 30.0,
        thinking_level: str | None = None,
        media_resolution: str | None = None,
        temperature: float | None = None,
        pakai_skema: bool = True,
    ) -> None:
        from google import genai
        from google.genai import types

        self.nama_model = nama_model
        self._types = types
        self._thinking_level = thinking_level
        self._media_resolution = media_resolution
        self._temperature = temperature
        self._pakai_skema = pakai_skema
        self._timeout = _jenis_timeout()
        self._klien = genai.Client(
            api_key=api_key,
            http_options=types.HttpOptions(
                timeout=int(timeout_detik * 1000),
                retry_options=types.HttpRetryOptions(attempts=2, http_status_codes=[429, 500, 502, 503, 504]),
            ),
        )

    def hasilkan(self, gambar: bytes, mime: str) -> KeluaranModel:
        from google.genai import errors

        types = self._types
        if self._media_resolution:
            bagian = types.Part.from_bytes(
                data=gambar, mime_type=mime, media_resolution=self._media_resolution
            )
        else:
            bagian = types.Part.from_bytes(data=gambar, mime_type=mime)

        konfig: dict[str, Any] = {"response_mime_type": "application/json"}
        if self._pakai_skema:
            konfig["response_json_schema"] = SKEMA_KELUARAN
        if self._thinking_level:
            konfig["thinking_config"] = types.ThinkingConfig(thinking_level=self._thinking_level)
        if self._temperature is not None:
            konfig["temperature"] = self._temperature

        try:
            respons = self._klien.models.generate_content(
                model=self.nama_model,
                contents=[bagian, PROMPT],
                config=types.GenerateContentConfig(**konfig),
            )
        except errors.APIError as e:
            if e.code == 429 or isinstance(e, errors.ServerError):
                raise ModelSibuk(str(e)) from e
            if e.code in (408, 504):
                raise ModelTimeout(str(e)) from e
            raise ModelGagal(str(e)) from e
        except self._timeout as e:
            raise ModelTimeout(str(e)) from e

        meta = respons.usage_metadata
        pemakaian = (
            Pemakaian(
                meta.prompt_token_count or 0,
                meta.candidates_token_count or 0,
                meta.thoughts_token_count or 0,
            )
            if meta
            else Pemakaian()
        )
        return KeluaranModel(teks=respons.text or "", pemakaian=pemakaian)
