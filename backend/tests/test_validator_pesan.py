"""Teks pesan isu baris tidak cocok; harus sama persis dengan klien (Validator.kt), memakai tanda ×."""

from __future__ import annotations

from decimal import Decimal

from nota.schema import ItemBaris, Nota
from nota.validator import Kode, validasi


def test_pesan_baris_tidak_cocok_memakai_tanda_kali():
    nota = Nota(
        items=(ItemBaris(nama="Gula", qty=Decimal("2"), satuan="bal", harga_satuan=60, jumlah=120_000),),
        subtotal=120_000,
        total=120_000,
    )
    isu = [i for i in validasi(nota).isu if i.kode == Kode.BARIS_TIDAK_COCOK]
    assert [i.pesan for i in isu] == ["Baris 1 (Gula): 2 × Rp 60 = Rp 120, tapi tertulis Rp 120.000"]
