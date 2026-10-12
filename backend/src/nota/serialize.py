"""Konversi Nota/Isu ke JSON dan sebaliknya.

Format ini dipakai di tiga tempat sekaligus, sehingga hanya ada satu bentuk data:
respons API ke aplikasi, file jawaban benar evaluasi, dan shared/test-vectors.json.
Uang berupa bilangan bulat, qty berupa teks desimal ("1.5") agar tidak pernah lewat float.
"""

from __future__ import annotations

from decimal import Decimal
from typing import Any

from .schema import ItemBaris, Nota
from .validator import Isu


def _qty_ke_teks(qty: Decimal | None) -> str | None:
    return None if qty is None else f"{qty.normalize():f}"


def _teks_ke_qty(nilai: Any) -> Decimal | None:
    if nilai is None:
        return None
    if isinstance(nilai, (int, str)) and not isinstance(nilai, bool):
        return Decimal(str(nilai))
    raise ValueError(f"qty harus teks desimal atau bilangan bulat: {nilai!r}")


def _int_opsional(nilai: Any, nama: str) -> int | None:
    if nilai is None:
        return None
    if isinstance(nilai, bool) or not isinstance(nilai, int):
        raise ValueError(f"{nama} harus bilangan bulat rupiah: {nilai!r}")
    return nilai


def nota_ke_dict(nota: Nota) -> dict[str, Any]:
    return {
        "items": [
            {
                "nama": it.nama,
                "qty": _qty_ke_teks(it.qty),
                "satuan": it.satuan,
                "harga_satuan": it.harga_satuan,
                "jumlah": it.jumlah,
                "diskon_baris": it.diskon_baris,
                "harga_dihitung": it.harga_dihitung,
            }
            for it in nota.items
        ],
        "subtotal": nota.subtotal,
        "diskon": nota.diskon,
        "pajak": nota.pajak,
        "biaya_lain": nota.biaya_lain,
        "pembulatan": nota.pembulatan,
        "total": nota.total,
        "supplier": nota.supplier,
        "tanggal_tertulis": nota.tanggal_tertulis,
    }


def dict_ke_nota(data: dict[str, Any]) -> Nota:
    """Kebalikan nota_ke_dict. Ketat: dipakai untuk jawaban benar dan vektor uji."""
    items = tuple(
        ItemBaris(
            nama=str(it["nama"]),
            qty=_teks_ke_qty(it.get("qty")),
            satuan=it.get("satuan"),
            harga_satuan=_int_opsional(it.get("harga_satuan"), "harga_satuan"),
            jumlah=_int_opsional(it.get("jumlah"), "jumlah"),
            diskon_baris=_int_opsional(it.get("diskon_baris", 0), "diskon_baris") or 0,
            harga_dihitung=bool(it.get("harga_dihitung", False)),
        )
        for it in data.get("items", [])
    )
    return Nota(
        items=items,
        total=_int_opsional(data.get("total"), "total"),
        subtotal=_int_opsional(data.get("subtotal"), "subtotal"),
        diskon=_int_opsional(data.get("diskon", 0), "diskon") or 0,
        pajak=_int_opsional(data.get("pajak", 0), "pajak") or 0,
        biaya_lain=_int_opsional(data.get("biaya_lain", 0), "biaya_lain") or 0,
        pembulatan=_int_opsional(data.get("pembulatan", 0), "pembulatan") or 0,
        supplier=data.get("supplier"),
        tanggal_tertulis=data.get("tanggal_tertulis"),
    )


def isu_ke_dict(isu: Isu) -> dict[str, Any]:
    return {
        "kode": isu.kode.value,
        "tingkat": isu.tingkat.value,
        "pesan": isu.pesan,
        "baris": isu.baris,
        "diharapkan": isu.diharapkan,
        "ditemukan": isu.ditemukan,
    }
