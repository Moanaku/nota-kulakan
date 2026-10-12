"""Klien model tiruan untuk pengembangan aplikasi tanpa API key dan tanpa biaya.

Aktif dengan NOTA_KLIEN=tiruan. Selalu mengembalikan nota CONTOH (bukan data nyata) yang
sengaja berisi satu kesalahan baca klasik ("60" untuk "60.000") dan satu baris tanpa harga
satuan, supaya layar sorotan dan koreksi di aplikasi bisa diuji ujung ke ujung.
Jangan pernah dipakai untuk evaluasi.
"""

from __future__ import annotations

import json

from .extraction import KeluaranModel, Pemakaian

CONTOH = {
    "terbaca": True,
    "jenis": "cetak",
    "supplier": "Grosir Contoh",
    "tanggal": "12-10-2026",
    "items": [
        {
            "nama": "Mie Goreng",
            "qty": "2",
            "satuan": "dus",
            "harga_satuan": "110.000",
            "jumlah": "220.000",
            "diskon": None,
        },
        {
            "nama": "Minyak Goreng 2L",
            "qty": "2",
            "satuan": "karton",
            "harga_satuan": "210.000",
            "jumlah": "420.000",
            "diskon": None,
        },
        {
            "nama": "Gula Pasir 1kg",
            "qty": "2",
            "satuan": "bal",
            "harga_satuan": "60",
            "jumlah": "120.000",
            "diskon": None,
        },
        {
            "nama": "Air Mineral 600ml",
            "qty": "3",
            "satuan": "dus",
            "harga_satuan": None,
            "jumlah": "135.000",
            "diskon": None,
        },
    ],
    "subtotal": "895.000",
    "diskon": None,
    "pajak": None,
    "biaya_lain": None,
    "total": "895.000",
    "catatan": "Data contoh dari klien tiruan",
}


class KlienTiruan:
    nama_model = "tiruan"

    def hasilkan(self, gambar: bytes, mime: str) -> KeluaranModel:
        return KeluaranModel(teks=json.dumps(CONTOH), pemakaian=Pemakaian())
