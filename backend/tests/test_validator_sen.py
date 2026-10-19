"""Validator saat sen dibulatkan per field, bukan dari total (nota cetak "PPN Exclude").

Mendokumentasikan perilaku sekarang: validator tidak punya toleransi, jadi selisih 1 rupiah
akibat pembulatan terpisah ditandai sebagai total_tidak_cocok.
"""

from __future__ import annotations

from decimal import Decimal

from nota.money import parse_rupiah
from nota.schema import ItemBaris, Nota
from nota.validator import Kode, validasi


def _nota(jumlah: int, pajak: str, total: str) -> Nota:
    return Nota(
        items=(
            ItemBaris(
                nama="Minyak goreng", qty=Decimal("1"), satuan="drm", harga_satuan=jumlah, jumlah=jumlah
            ),
        ),
        subtotal=jumlah,
        pajak=parse_rupiah(pajak),
        total=parse_rupiah(total),
    )


def test_total_bersen_dibulatkan_naik_selisih_satu_rupiah_ditandai():
    # 4.058.556 + 446.441,16 = 4.504.997,16, tetapi total tercetak 4.504.997,56 -> 4.504.998.
    hasil = validasi(_nota(4058556, "446.441,16", "4.504.997,56"))
    assert [(i.kode, i.diharapkan, i.ditemukan) for i in hasil.isu] == [
        (Kode.TOTAL_TIDAK_COCOK, 4504997, 4504998)
    ]
    assert not hasil.lolos


def test_dua_field_sen_50_dibulatkan_naik_terpisah_ditandai():
    # Subtotal 1.000,50 dan pajak 1.100,50 naik masing-masing (1.001 + 1.101 = 2.102),
    # sedangkan total tercetak 2.101,00.
    hasil = validasi(_nota(parse_rupiah("1.000,50"), "1.100,50", "2.101,00"))
    assert [(i.kode, i.diharapkan, i.ditemukan) for i in hasil.isu] == [(Kode.TOTAL_TIDAK_COCOK, 2102, 2101)]


def test_pembulatan_yang_selaras_lolos():
    # Nota 022: 4.058.556 + 446.441,16 -> 446.441; total 4.504.997,16 -> 4.504.997.
    assert validasi(_nota(4058556, "446.441,16", "4.504.997,16")).lolos
