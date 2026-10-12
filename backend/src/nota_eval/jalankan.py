"""Menjalankan evaluasi dan pra-label."""

from __future__ import annotations

import csv
import io
import json
import os
import time
from collections.abc import Callable
from dataclasses import asdict, dataclass
from datetime import UTC, datetime
from pathlib import Path

from nota.api import deteksi_mime
from nota.extraction import EkstraksiGagal, KlienGemini, KlienModel, ekstrak
from nota.pricing import SUMBER_HARGA, TANGGAL_HARGA, biaya_usd
from nota.prompt import VERSI_PROMPT
from nota.serialize import isu_ke_dict, nota_ke_dict

from .dataset import Kasus
from .metrik import HasilKasus, nilai_kasus, ringkas, tabel_markdown

PENANDA_PRALABEL = "_status"


@dataclass(frozen=True)
class Konfigurasi:
    model: str
    max_sisi: int  # 0 = foto asli
    kualitas: int = 85
    thinking_level: str | None = None
    media_resolution: str | None = None

    @property
    def label(self) -> str:
        bagian = [self.model, f"{self.max_sisi}px" if self.max_sisi else "asli"]
        if self.thinking_level:
            bagian.append(f"think-{self.thinking_level.lower()}")
        if self.media_resolution:
            bagian.append(self.media_resolution.lower().replace("media_resolution_", "res-"))
        return "@".join(bagian)


def muat_env(path: Path) -> None:
    """Muat KEY=VALUE dari backend/.env tanpa menimpa environment yang sudah ada."""
    if not path.exists():
        return
    for baris in path.read_text(encoding="utf-8").splitlines():
        baris = baris.split("#", 1)[0].strip()
        if "=" in baris:
            kunci, nilai = baris.split("=", 1)
            os.environ.setdefault(kunci.strip(), nilai.strip())


def siapkan_gambar(path: Path, max_sisi: int, kualitas: int) -> tuple[bytes, str]:
    """Kecilkan foto seperti yang dilakukan aplikasi (tanpa memperbesar), atau kirim asli."""
    data = path.read_bytes()
    if not max_sisi:
        mime = deteksi_mime(data)
        if mime is None:
            raise ValueError(f"{path}: bukan JPEG/PNG/WebP")
        return data, mime
    from PIL import Image, ImageOps

    with Image.open(io.BytesIO(data)) as asli:
        gambar = ImageOps.exif_transpose(asli).convert("RGB")
        gambar.thumbnail((max_sisi, max_sisi), Image.Resampling.LANCZOS)
        buf = io.BytesIO()
        gambar.save(buf, format="JPEG", quality=kualitas, optimize=True)
    return buf.getvalue(), "image/jpeg"


def klien_gemini(konfig: Konfigurasi) -> KlienModel:
    kunci = os.environ.get("GEMINI_API_KEY")
    if not kunci:
        raise SystemExit("GEMINI_API_KEY belum diisi (backend/.env atau environment)")
    return KlienGemini(
        api_key=kunci,
        nama_model=konfig.model,
        thinking_level=konfig.thinking_level,
        media_resolution=konfig.media_resolution,
    )


def periksa_kebenaran_siap(kasus: list[Kasus]) -> None:
    belum: list[str] = []
    for k in kasus:
        if not k.kebenaran.exists():
            belum.append(f"{k.id} (file kebenaran tidak ada)")
            continue
        data = json.loads(k.kebenaran.read_text(encoding="utf-8"))
        if PENANDA_PRALABEL in data:
            belum.append(f"{k.id} (masih pra-label, belum diperiksa)")
    if belum:
        raise SystemExit("Kebenaran belum siap:\n  " + "\n  ".join(belum))


def jalankan_evaluasi(
    kasus: list[Kasus],
    konfigurasi: list[Konfigurasi],
    keluar: Path,
    buat_klien: Callable[[Konfigurasi], KlienModel] = klien_gemini,
    jeda_detik: float = 0.0,
    kurs_idr: float | None = None,
    cetak: Callable[[str], None] = print,
) -> list[HasilKasus]:
    periksa_kebenaran_siap(kasus)
    keluar.mkdir(parents=True, exist_ok=True)
    semua: list[HasilKasus] = []

    for konfig in konfigurasi:
        klien = buat_klien(konfig)
        folder_prediksi = keluar / "prediksi" / konfig.label.replace("@", "_")
        folder_prediksi.mkdir(parents=True, exist_ok=True)
        for nomor, k in enumerate(kasus, start=1):
            benar = k.baca_kebenaran()
            gambar, mime = siapkan_gambar(k.gambar, konfig.max_sisi, konfig.kualitas)
            try:
                hasil = ekstrak(gambar, mime, klien)
                h = nilai_kasus(
                    k.id, k.jenis, konfig.label, benar, hasil.nota, hasil.lolos, None,
                    hasil.biaya_usd, hasil.latensi_ms, len(gambar),
                )  # fmt: skip
                simpan = {
                    "nota": nota_ke_dict(hasil.nota),
                    "isu": [isu_ke_dict(i) for i in hasil.isu],
                    "lolos": hasil.lolos,
                    "token": asdict(hasil.pemakaian),
                }
            except EkstraksiGagal as e:
                p = e.pemakaian
                biaya = biaya_usd(e.model, p.token_input, p.token_output, p.token_thinking)
                h = nilai_kasus(
                    k.id, k.jenis, konfig.label, benar, None, None, e.jenis.value, biaya, None, len(gambar)
                )
                simpan = {"gagal": e.jenis.value, "pesan": e.pesan, "token": asdict(p)}
            (folder_prediksi / f"{k.id}.json").write_text(
                json.dumps(simpan, ensure_ascii=False, indent=2), encoding="utf-8"
            )
            semua.append(h)
            status = "GAGAL " + h.gagal if h.gagal else ("sempurna" if h.sempurna else "ada salah")
            cetak(f"[{konfig.label}] {nomor}/{len(kasus)} {k.id}: {status}")
            if jeda_detik:
                time.sleep(jeda_detik)

    _tulis_csv(keluar / "hasil.csv", semua)
    ringkasan = ringkas(semua)
    meta = (
        f"# Hasil evaluasi\n\n"
        f"- Waktu: {datetime.now(UTC).isoformat(timespec='seconds')}\n"
        f"- Versi prompt: {VERSI_PROMPT}\n"
        f"- Split: {', '.join(sorted({k.split or '-' for k in kasus}))}; kasus: {len(kasus)}\n"
        f"- Harga model: {SUMBER_HARGA} (dibaca {TANGGAL_HARGA})\n"
        f"- Definisi metrik: lihat docs/EVALUASI.md\n\n"
    )
    tabel = tabel_markdown(ringkasan, kurs_idr)
    (keluar / "ringkasan.md").write_text(meta + tabel, encoding="utf-8")
    cetak("\n" + tabel)
    return semua


def _tulis_csv(path: Path, hasil: list[HasilKasus]) -> None:
    if not hasil:
        return
    kolom = list(asdict(hasil[0]).keys()) + ["kategori_validator"]
    with path.open("w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=kolom)
        w.writeheader()
        for h in hasil:
            w.writerow(asdict(h) | {"kategori_validator": h.kategori_validator})


def pralabel(
    kasus: list[Kasus],
    konfig: Konfigurasi,
    timpa: bool = False,
    buat_klien: Callable[[Konfigurasi], KlienModel] = klien_gemini,
    cetak: Callable[[str], None] = print,
) -> int:
    """Tulis draf kebenaran dari hasil model, ditandai BELUM DIPERIKSA.

    Mempercepat pelabelan, tapi berisiko bias jangkar: setiap angka WAJIB dicocokkan dengan
    foto. `jalankan` menolak file yang masih bertanda pra-label.
    """
    klien = buat_klien(konfig)
    ditulis = 0
    for k in kasus:
        if k.kebenaran.exists() and not timpa:
            continue
        gambar, mime = siapkan_gambar(k.gambar, konfig.max_sisi, konfig.kualitas)
        try:
            data = nota_ke_dict(ekstrak(gambar, mime, klien).nota)
        except EkstraksiGagal as e:
            cetak(f"{k.id}: ekstraksi gagal ({e.jenis.value}), templat kosong ditulis")
            data = {"items": [], "total": None}
        data[PENANDA_PRALABEL] = (
            "pralabel - BELUM DIPERIKSA. Cocokkan tiap angka dengan foto, lalu hapus baris ini."
        )
        k.kebenaran.parent.mkdir(parents=True, exist_ok=True)
        k.kebenaran.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
        ditulis += 1
        cetak(f"{k.id}: draf ditulis ke {k.kebenaran}")
    return ditulis
