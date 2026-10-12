"""Validator aritmetika nota. Murni: tanpa jaringan, tanpa AI, tanpa I/O.

Ini lapisan yang membuat hasil AI bisa diperiksa tanpa manusia mengecek semuanya.
Tiga aturan:
  1. Per baris : qty x harga_satuan - diskon_baris == jumlah
  2. Subtotal  : jumlah semua baris == subtotal tertulis (bila ada)
  3. Total     : jumlah semua baris - diskon + pajak + biaya_lain + pembulatan == total tertulis

Data buruk dari AI bukan error program, jadi hasilnya berupa daftar `Isu`, bukan exception.

Batas yang harus ditulis jujur di README: lolos validator berarti angka-angkanya konsisten
satu sama lain, BUKAN berarti benar. Dua angka yang salah bisa saling menutupi, dan nama
barang yang salah baca tidak tertangkap sama sekali. Karena itu akurasi tetap diukur
terhadap jawaban benar di set evaluasi.

Aturan ini dicerminkan di Kotlin (app/.../domain/Validator.kt) agar sorotan di layar koreksi
berubah seketika. Kedua versi dijaga oleh shared/test-vectors.json.
"""

from __future__ import annotations

from dataclasses import dataclass
from decimal import ROUND_HALF_UP, Decimal
from enum import StrEnum

from .money import format_qty
from .money import format_rupiah as rp
from .schema import ItemBaris, Nota


class Kode(StrEnum):
    TIDAK_ADA_ITEM = "tidak_ada_item"
    FIELD_KOSONG = "field_kosong"
    FORMAT_TIDAK_DIKENALI = "format_tidak_dikenali"  # dipakai saat pemetaan keluaran AI
    QTY_TIDAK_VALID = "qty_tidak_valid"
    BARIS_TIDAK_COCOK = "baris_tidak_cocok"
    HARGA_DIHITUNG = "harga_dihitung"
    SUBTOTAL_TIDAK_COCOK = "subtotal_tidak_cocok"
    TOTAL_TIDAK_COCOK = "total_tidak_cocok"
    TOTAL_TIDAK_BISA_DIHITUNG = "total_tidak_bisa_dihitung"


class Tingkat(StrEnum):
    GALAT = "galat"  # menghalangi simpan sampai diperbaiki atau dikonfirmasi
    INFO = "info"  # hanya pemberitahuan


@dataclass(frozen=True)
class Isu:
    kode: Kode
    pesan: str
    baris: int | None = None  # indeks 0-based; None = isu tingkat nota
    diharapkan: int | None = None
    ditemukan: int | None = None
    tingkat: Tingkat = Tingkat.GALAT

    @property
    def selisih(self) -> int | None:
        if self.diharapkan is None or self.ditemukan is None:
            return None
        return self.ditemukan - self.diharapkan


@dataclass(frozen=True)
class HasilValidasi:
    isu: tuple[Isu, ...]

    @property
    def lolos(self) -> bool:
        return not any(i.tingkat is Tingkat.GALAT for i in self.isu)

    @property
    def baris_bermasalah(self) -> frozenset[int]:
        """Indeks baris yang perlu disorot merah di layar koreksi."""
        return frozenset(i.baris for i in self.isu if i.baris is not None and i.tingkat is Tingkat.GALAT)


def bulatkan(nilai: Decimal) -> int:
    return int(nilai.to_integral_value(rounding=ROUND_HALF_UP))


def periksa_baris(i: int, item: ItemBaris) -> list[Isu]:
    isu: list[Isu] = []
    kosong = [
        nama
        for nama, nilai in (
            ("qty", item.qty),
            ("harga_satuan", item.harga_satuan),
            ("jumlah", item.jumlah),
        )
        if nilai is None
    ]
    if kosong:
        isu.append(
            Isu(
                Kode.FIELD_KOSONG,
                f"Baris {i + 1} ({item.nama}): belum terisi: {', '.join(kosong)}",
                baris=i,
            )
        )
    qty_buruk = item.qty is not None and item.qty <= 0
    if qty_buruk:
        isu.append(Isu(Kode.QTY_TIDAK_VALID, f"Baris {i + 1} ({item.nama}): qty harus lebih dari 0", baris=i))
    if kosong or qty_buruk:
        return isu
    assert item.qty is not None and item.harga_satuan is not None and item.jumlah is not None

    if item.harga_dihitung:
        isu.append(
            Isu(
                Kode.HARGA_DIHITUNG,
                f"Baris {i + 1} ({item.nama}): harga satuan tidak tertulis, dihitung dari jumlah dibagi qty",
                baris=i,
                tingkat=Tingkat.INFO,
            )
        )
        return isu

    diharapkan = bulatkan(item.qty * Decimal(item.harga_satuan)) - item.diskon_baris
    if diharapkan != item.jumlah:
        isu.append(
            Isu(
                Kode.BARIS_TIDAK_COCOK,
                f"Baris {i + 1} ({item.nama}): {format_qty(item.qty)} x {rp(item.harga_satuan)}"
                f" = {rp(diharapkan)}, tapi tertulis {rp(item.jumlah)}",
                baris=i,
                diharapkan=diharapkan,
                ditemukan=item.jumlah,
            )
        )
    return isu


def validasi(nota: Nota) -> HasilValidasi:
    isu: list[Isu] = []

    if not nota.items:
        isu.append(Isu(Kode.TIDAK_ADA_ITEM, "Tidak ada baris barang yang terbaca"))

    for i, item in enumerate(nota.items):
        isu.extend(periksa_baris(i, item))

    if nota.total is None:
        isu.append(Isu(Kode.FIELD_KOSONG, "Total nota belum terisi"))

    if not nota.items:
        return HasilValidasi(tuple(isu))

    # Aturan 2 dan 3 butuh jumlah semua baris. Kalau ada yang kosong, jangan menebak.
    if any(item.jumlah is None for item in nota.items):
        isu.append(
            Isu(
                Kode.TOTAL_TIDAK_BISA_DIHITUNG,
                "Ada baris tanpa jumlah, jadi subtotal dan total belum bisa diperiksa",
            )
        )
        return HasilValidasi(tuple(isu))

    jumlah_baris = sum(item.jumlah for item in nota.items if item.jumlah is not None)

    if nota.subtotal is not None and nota.subtotal != jumlah_baris:
        isu.append(
            Isu(
                Kode.SUBTOTAL_TIDAK_COCOK,
                f"Jumlah semua baris {rp(jumlah_baris)}, tapi subtotal tertulis {rp(nota.subtotal)}",
                diharapkan=jumlah_baris,
                ditemukan=nota.subtotal,
            )
        )

    if nota.total is not None:
        total_hitung = jumlah_baris - nota.diskon + nota.pajak + nota.biaya_lain + nota.pembulatan
        if total_hitung != nota.total:
            isu.append(
                Isu(
                    Kode.TOTAL_TIDAK_COCOK,
                    f"Hasil hitung {rp(total_hitung)}, tapi total tertulis {rp(nota.total)}",
                    diharapkan=total_hitung,
                    ditemukan=nota.total,
                )
            )

    return HasilValidasi(tuple(isu))
