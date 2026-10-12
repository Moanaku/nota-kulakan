"""Skema nota v1 (DRAFT, menunggu tinjauan Dani).

Skema ini adalah kontrak antara ekstraksi AI dan validator: keputusan di sini menentukan
apa yang bisa diperiksa.

Keputusan desain:
- Semua uang `int` rupiah, qty `Decimal` (ada barang curah seperti 1,5 kg).
- Field yang gagal dibaca boleh `None`. Validator yang menandainya, bukan konstruktor
  yang melempar error, karena data buruk dari AI adalah kejadian biasa.
- `nama` disimpan apa adanya dari nota. Singkatan grosir tidak dinormalkan di sini;
  pemetaan nama ke produk dilakukan pengguna di aplikasi dan diingat sebagai alias.
- `harga_dihitung`: nota tulisan tangan sering hanya menulis qty dan jumlah. Harga satuan
  lalu dihitung jumlah / qty (hanya bila habis dibagi). Baris seperti itu tidak bisa
  diperiksa aturan per baris, dan ditandai sebagai info, bukan galat.
- `biaya_lain`: ongkos kirim, service charge, dan biaya lain yang menambah total.
"""

from __future__ import annotations

from dataclasses import dataclass
from decimal import Decimal


@dataclass(frozen=True)
class ItemBaris:
    nama: str
    qty: Decimal | None
    satuan: str | None  # "dus", "pcs", "renceng", "slop", ...
    harga_satuan: int | None
    jumlah: int | None  # nilai baris sebagaimana tertulis di nota
    diskon_baris: int = 0
    harga_dihitung: bool = False


@dataclass(frozen=True)
class Nota:
    items: tuple[ItemBaris, ...]
    total: int | None  # total akhir tertulis
    subtotal: int | None = None  # jumlah semua baris tertulis, bila ada
    diskon: int = 0  # diskon tingkat nota (positif = mengurangi)
    pajak: int = 0
    biaya_lain: int = 0  # ongkir, service, dll. (positif = menambah)
    pembulatan: int = 0  # bisa negatif
    supplier: str | None = None
    tanggal_tertulis: str | None = None  # apa adanya dari nota, tidak diparse
