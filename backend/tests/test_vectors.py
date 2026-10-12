"""Menjalankan shared/test-vectors.json. Versi Kotlin menjalankan file yang sama."""

from __future__ import annotations

from decimal import Decimal

import pytest

from nota.money import QtyParseError, RupiahParseError, parse_qty, parse_rupiah
from nota.serialize import dict_ke_nota
from nota.validator import validasi


def test_vektor_rupiah(vektor):
    for kasus in vektor["rupiah"]:
        if kasus.get("galat"):
            with pytest.raises(RupiahParseError):
                parse_rupiah(kasus["teks"])
        else:
            assert parse_rupiah(kasus["teks"]) == kasus["hasil"], kasus["teks"]


def test_vektor_qty(vektor):
    for kasus in vektor["qty"]:
        if kasus.get("galat"):
            with pytest.raises(QtyParseError):
                parse_qty(kasus["teks"])
        else:
            assert parse_qty(kasus["teks"]) == Decimal(kasus["hasil"]), kasus["teks"]


def test_vektor_validasi(vektor):
    for kasus in vektor["validasi"]:
        hasil = validasi(dict_ke_nota(kasus["nota"]))
        aktual = [
            {
                "kode": i.kode.value,
                "baris": i.baris,
                "diharapkan": i.diharapkan,
                "ditemukan": i.ditemukan,
                "tingkat": i.tingkat.value,
            }
            for i in hasil.isu
        ]
        assert aktual == kasus["harapan"]["isu"], kasus["nama"]
        assert hasil.lolos == kasus["harapan"]["lolos"], kasus["nama"]


def test_vektor_tidak_kosong(vektor):
    # Penjaga: vektor yang terhapus tidak boleh membuat tes "lolos" diam-diam.
    assert len(vektor["rupiah"]) >= 20
    assert len(vektor["qty"]) >= 10
    assert len(vektor["validasi"]) >= 10
