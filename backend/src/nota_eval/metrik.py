"""Metrik evaluasi ekstraksi.

Definisi (ditulis ulang di README agar angka bisa dipahami):
- Baris disejajarkan antara prediksi dan kebenaran berdasarkan kemiripan nama (greedy,
  ambang 0,5). Baris kebenaran tanpa pasangan = terlewat; baris prediksi tanpa pasangan
  = tambahan.
- Akurasi field per baris (qty, harga_satuan, diskon_baris, jumlah) = nilai persis sama / jumlah
  baris kebenaran yang punya nilai itu. Baris terlewat dihitung salah. Pajak dinilai per nota.
- Nota sempurna = tidak gagal, jumlah baris sama, setiap baris cocok persis di qty, harga_satuan,
  diskon_baris, dan jumlah, serta diskon, pajak, biaya_lain, pembulatan, dan total tingkat nota
  sama. Subtotal tidak dibandingkan (kunci sering null karena nota tidak mencetaknya).
- Validator dinilai dengan matriks kebingungan, HANYA pada nota yang kebenarannya konsisten
  secara aritmetika (nota asli yang hitungannya memang salah dilaporkan terpisah):
    tertangkap  = tidak sempurna DAN ditandai validator      (bagus)
    terlewat    = tidak sempurna TAPI lolos validator         (berbahaya: data salah masuk)
    alarm palsu = sempurna TAPI ditandai validator            (mengganggu pengguna)
"""

from __future__ import annotations

import math
import re
import statistics
from dataclasses import dataclass, field
from decimal import Decimal
from difflib import SequenceMatcher

from nota.schema import ItemBaris, Nota
from nota.validator import bulatkan

AMBANG_KEMIRIPAN = 0.5


def normalisasi_nama(nama: str) -> str:
    return re.sub(r"\s+", " ", re.sub(r"[^0-9a-z]+", " ", nama.lower())).strip()


def sejajarkan(prediksi: tuple[ItemBaris, ...], benar: tuple[ItemBaris, ...]) -> list[tuple[int, int]]:
    """Pasangan (indeks_prediksi, indeks_benar), greedy menurut kemiripan nama."""
    kandidat: list[tuple[float, int, int, int]] = []
    for i, p in enumerate(prediksi):
        for j, b in enumerate(benar):
            skor = SequenceMatcher(None, normalisasi_nama(p.nama), normalisasi_nama(b.nama)).ratio()
            if skor >= AMBANG_KEMIRIPAN:
                kandidat.append((skor, -abs(i - j), i, j))
    kandidat.sort(reverse=True)
    terpakai_p: set[int] = set()
    terpakai_b: set[int] = set()
    pasangan: list[tuple[int, int]] = []
    for _, _, i, j in kandidat:
        if i in terpakai_p or j in terpakai_b:
            continue
        terpakai_p.add(i)
        terpakai_b.add(j)
        pasangan.append((i, j))
    return sorted(pasangan, key=lambda x: x[1])


def konsisten(nota: Nota) -> bool:
    """Apakah hitungan di nota (kebenaran) sendiri konsisten.

    Baris tanpa harga satuan dilewati untuk aturan per baris (umum di nota dan di CORD).
    """
    for it in nota.items:
        if it.qty is not None and it.harga_satuan is not None and it.jumlah is not None:
            if bulatkan(it.qty * Decimal(it.harga_satuan)) - it.diskon_baris != it.jumlah:
                return False
    if nota.total is not None and nota.items and all(it.jumlah is not None for it in nota.items):
        jumlah = sum(it.jumlah for it in nota.items if it.jumlah is not None)
        if nota.subtotal is not None and nota.subtotal != jumlah:
            return False
        if jumlah - nota.diskon + nota.pajak + nota.biaya_lain + nota.pembulatan != nota.total:
            return False
    return True


@dataclass(frozen=True)
class HasilKasus:
    id: str
    jenis: str
    konfigurasi: str
    gagal: str | None  # jenis kegagalan ekstraksi, atau None
    lolos_validator: bool | None
    kebenaran_konsisten: bool
    baris_benar: int
    baris_prediksi: int
    baris_terlewat: int
    baris_tambahan: int
    qty_benar: int
    qty_n: int
    harga_benar: int
    harga_n: int
    jumlah_benar: int
    jumlah_n: int
    diskon_baris_benar: int
    diskon_baris_n: int
    total_benar: bool
    pajak_benar: bool
    sempurna: bool
    biaya_usd: float | None
    latensi_ms: int | None
    ukuran_byte: int

    @property
    def kategori_validator(self) -> str | None:
        if self.gagal is not None or self.lolos_validator is None or not self.kebenaran_konsisten:
            return None
        if self.sempurna:
            return "benar_lolos" if self.lolos_validator else "alarm_palsu"
        return "terlewat" if self.lolos_validator else "tertangkap"


def nilai_kasus(
    id: str,
    jenis: str,
    konfigurasi: str,
    benar: Nota,
    prediksi: Nota | None,
    lolos_validator: bool | None,
    gagal: str | None,
    biaya: float | None,
    latensi_ms: int | None,
    ukuran_byte: int,
) -> HasilKasus:
    qty_n = sum(1 for b in benar.items if b.qty is not None)
    harga_n = sum(1 for b in benar.items if b.harga_satuan is not None)
    jumlah_n = sum(1 for b in benar.items if b.jumlah is not None)
    diskon_baris_n = len(benar.items)  # diskon_baris tidak pernah null (bawaan 0)

    if prediksi is None:
        return HasilKasus(
            id, jenis, konfigurasi, gagal, None, konsisten(benar), len(benar.items), 0,
            len(benar.items), 0, 0, qty_n, 0, harga_n, 0, jumlah_n, 0, diskon_baris_n,
            False, False, False, biaya, latensi_ms, ukuran_byte,
        )  # fmt: skip

    pasangan = sejajarkan(prediksi.items, benar.items)
    qty_benar = harga_benar = jumlah_benar = diskon_baris_benar = baris_sempurna = 0
    for i, j in pasangan:
        p, b = prediksi.items[i], benar.items[j]
        cocok_qty = b.qty is None or p.qty == b.qty
        cocok_harga = b.harga_satuan is None or p.harga_satuan == b.harga_satuan
        cocok_jumlah = b.jumlah is None or p.jumlah == b.jumlah
        cocok_diskon = p.diskon_baris == b.diskon_baris
        qty_benar += b.qty is not None and p.qty == b.qty
        harga_benar += b.harga_satuan is not None and p.harga_satuan == b.harga_satuan
        jumlah_benar += b.jumlah is not None and p.jumlah == b.jumlah
        diskon_baris_benar += cocok_diskon
        baris_sempurna += cocok_qty and cocok_harga and cocok_jumlah and cocok_diskon

    total_benar = benar.total is None or prediksi.total == benar.total
    pajak_benar = prediksi.pajak == benar.pajak
    # Subtotal sengaja tidak dibandingkan: kunci sering null karena nota tidak mencetaknya.
    tingkat_nota_cocok = (
        prediksi.diskon == benar.diskon
        and pajak_benar
        and prediksi.biaya_lain == benar.biaya_lain
        and prediksi.pembulatan == benar.pembulatan
    )
    sempurna = (
        gagal is None
        and len(prediksi.items) == len(benar.items)
        and baris_sempurna == len(benar.items)
        and total_benar
        and tingkat_nota_cocok
    )
    return HasilKasus(
        id=id,
        jenis=jenis,
        konfigurasi=konfigurasi,
        gagal=gagal,
        lolos_validator=lolos_validator,
        kebenaran_konsisten=konsisten(benar),
        baris_benar=len(benar.items),
        baris_prediksi=len(prediksi.items),
        baris_terlewat=len(benar.items) - len(pasangan),
        baris_tambahan=len(prediksi.items) - len(pasangan),
        qty_benar=qty_benar,
        qty_n=qty_n,
        harga_benar=harga_benar,
        harga_n=harga_n,
        jumlah_benar=jumlah_benar,
        jumlah_n=jumlah_n,
        diskon_baris_benar=diskon_baris_benar,
        diskon_baris_n=diskon_baris_n,
        total_benar=total_benar,
        pajak_benar=pajak_benar,
        sempurna=sempurna,
        biaya_usd=biaya,
        latensi_ms=latensi_ms,
        ukuran_byte=ukuran_byte,
    )


def wilson(k: int, n: int, z: float = 1.96) -> tuple[float, float]:
    """Interval kepercayaan 95% Wilson untuk proporsi k/n. Jujur untuk n kecil."""
    if n == 0:
        return (0.0, 0.0)
    p = k / n
    penyebut = 1 + z * z / n
    tengah = (p + z * z / (2 * n)) / penyebut
    lebar = z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / penyebut
    return (max(0.0, tengah - lebar), min(1.0, tengah + lebar))


def _rasio(a: int, b: int) -> float | None:
    return a / b if b else None


def _persentil(data: list[int], p: float) -> int | None:
    if not data:
        return None
    urut = sorted(data)
    indeks = min(len(urut) - 1, max(0, math.ceil(p * len(urut)) - 1))
    return urut[indeks]


@dataclass
class Ringkasan:
    konfigurasi: str
    jenis: str
    n: int = 0
    gagal: int = 0
    sempurna: int = 0
    total_benar: int = 0
    pajak_benar: int = 0
    qty: tuple[int, int] = (0, 0)
    harga: tuple[int, int] = (0, 0)
    jumlah: tuple[int, int] = (0, 0)
    diskon_baris: tuple[int, int] = (0, 0)
    validator: dict[str, int] = field(default_factory=dict)
    tidak_konsisten: int = 0
    biaya: list[float] = field(default_factory=list)
    latensi: list[int] = field(default_factory=list)
    ukuran: list[int] = field(default_factory=list)

    @property
    def interval_sempurna(self) -> tuple[float, float]:
        return wilson(self.sempurna, self.n)

    @property
    def tingkat_tangkap(self) -> float | None:
        v = self.validator
        return _rasio(v.get("tertangkap", 0), v.get("tertangkap", 0) + v.get("terlewat", 0))

    @property
    def tingkat_alarm_palsu(self) -> float | None:
        v = self.validator
        return _rasio(v.get("alarm_palsu", 0), v.get("alarm_palsu", 0) + v.get("benar_lolos", 0))


def ringkas(hasil: list[HasilKasus]) -> list[Ringkasan]:
    """Ringkasan per (konfigurasi, jenis), ditambah baris 'semua' per konfigurasi."""
    kelompok: dict[tuple[str, str], Ringkasan] = {}
    for h in hasil:
        for jenis in (h.jenis, "semua"):
            r = kelompok.setdefault((h.konfigurasi, jenis), Ringkasan(h.konfigurasi, jenis))
            r.n += 1
            r.gagal += h.gagal is not None
            r.sempurna += h.sempurna
            r.total_benar += h.total_benar and h.gagal is None
            r.pajak_benar += h.pajak_benar and h.gagal is None
            r.qty = (r.qty[0] + h.qty_benar, r.qty[1] + h.qty_n)
            r.harga = (r.harga[0] + h.harga_benar, r.harga[1] + h.harga_n)
            r.jumlah = (r.jumlah[0] + h.jumlah_benar, r.jumlah[1] + h.jumlah_n)
            r.diskon_baris = (r.diskon_baris[0] + h.diskon_baris_benar, r.diskon_baris[1] + h.diskon_baris_n)
            r.tidak_konsisten += not h.kebenaran_konsisten
            kat = h.kategori_validator
            if kat:
                r.validator[kat] = r.validator.get(kat, 0) + 1
            if h.biaya_usd is not None:
                r.biaya.append(h.biaya_usd)
            if h.latensi_ms is not None:
                r.latensi.append(h.latensi_ms)
            r.ukuran.append(h.ukuran_byte)
    return sorted(kelompok.values(), key=lambda r: (r.konfigurasi, r.jenis != "semua", r.jenis))


def _pct(x: float | None) -> str:
    return "-" if x is None else f"{x * 100:.0f}%"


def _frac(pasangan: tuple[int, int]) -> str:
    a, b = pasangan
    return "-" if b == 0 else f"{a / b * 100:.0f}% ({a}/{b})"


def tabel_markdown(ringkasan: list[Ringkasan], kurs_idr: float | None = None) -> str:
    kepala = (
        "| Konfigurasi | Jenis | n | Nota sempurna (95% CI) | Total benar | Qty | Harga satuan | Jumlah "
        "| Diskon baris | Pajak | Validator menangkap | Alarm palsu | Gagal | Biaya/nota | Latensi p50/p90 "
        "| Ukuran foto |"
    )
    garis = "|" + "---|" * 16
    baris = [kepala, garis]
    for r in ringkasan:
        bawah, atas = r.interval_sempurna
        sempurna = (
            f"{_pct(_rasio(r.sempurna, r.n))} ({r.sempurna}/{r.n}; {bawah * 100:.0f}-{atas * 100:.0f}%)"
        )
        tangkap = r.validator.get("tertangkap", 0)
        lewat = r.validator.get("terlewat", 0)
        alarm = r.validator.get("alarm_palsu", 0)
        lolos = r.validator.get("benar_lolos", 0)
        biaya_rata = statistics.fmean(r.biaya) if r.biaya else None
        if biaya_rata is None:
            biaya = "-"
        elif kurs_idr:
            biaya = f"${biaya_rata:.5f} (Rp {biaya_rata * kurs_idr:,.1f})"
        else:
            biaya = f"${biaya_rata:.5f}"
        latensi = f"{_persentil(r.latensi, 0.5)}/{_persentil(r.latensi, 0.9)} ms" if r.latensi else "-"
        ukuran = f"{statistics.fmean(r.ukuran) / 1024:.0f} KB" if r.ukuran else "-"
        baris.append(
            f"| {r.konfigurasi} | {r.jenis} | {r.n} | {sempurna} | {_pct(_rasio(r.total_benar, r.n))} "
            f"| {_frac(r.qty)} | {_frac(r.harga)} | {_frac(r.jumlah)} "
            f"| {_frac(r.diskon_baris)} | {_frac((r.pajak_benar, r.n))} "
            f"| {_pct(r.tingkat_tangkap)} ({tangkap}/{tangkap + lewat}) "
            f"| {_pct(r.tingkat_alarm_palsu)} ({alarm}/{alarm + lolos}) "
            f"| {r.gagal} | {biaya} | {latensi} | {ukuran} |"
        )
    return "\n".join(baris) + "\n"
