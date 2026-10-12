from __future__ import annotations

from decimal import Decimal

import pytest

from nota.money import QtyParseError, RupiahParseError, format_qty, format_rupiah, parse_qty, parse_rupiah


@pytest.mark.parametrize(
    ("n", "teks"),
    [(0, "Rp 0"), (60000, "Rp 60.000"), (1250000, "Rp 1.250.000"), (-5000, "-Rp 5.000"), (999, "Rp 999")],
)
def test_format_rupiah(n, teks):
    assert format_rupiah(n) == teks


@pytest.mark.parametrize(
    ("qty", "teks"),
    [(Decimal("2"), "2"), (Decimal("1.500"), "1,5"), (Decimal("0.125"), "0,125"), (Decimal("1000"), "1000")],
)
def test_format_qty(qty, teks):
    assert format_qty(qty) == teks


@pytest.mark.parametrize("nilai", [None, 60000, 1.5])
def test_parse_rupiah_menolak_bukan_teks(nilai):
    with pytest.raises(RupiahParseError):
        parse_rupiah(nilai)  # type: ignore[arg-type]


def test_parse_qty_menolak_bukan_teks():
    with pytest.raises(QtyParseError):
        parse_qty(2)  # type: ignore[arg-type]


def test_format_lalu_parse_kembali():
    for n in (0, 1, 999, 1000, 60000, 1250000, 987654321):
        assert parse_rupiah(format_rupiah(n)) == n
