from __future__ import annotations

import json
from collections.abc import Iterable
from pathlib import Path

import pytest

from nota.extraction import KeluaranModel, ModelGagal, ModelSibuk, ModelTimeout, Pemakaian

AKAR_REPO = Path(__file__).resolve().parents[2]


@pytest.fixture(scope="session")
def vektor() -> dict:
    return json.loads((AKAR_REPO / "shared" / "test-vectors.json").read_text(encoding="utf-8"))


class KlienSkrip:
    """Klien model palsu: mengembalikan keluaran (atau melempar galat) sesuai urutan skrip."""

    def __init__(self, langkah: Iterable[object], nama_model: str = "gemini-3.1-flash-lite"):
        self._langkah = list(langkah)
        self.nama_model = nama_model
        self.panggilan = 0

    def hasilkan(self, gambar: bytes, mime: str) -> KeluaranModel:
        self.panggilan += 1
        langkah = self._langkah.pop(0)
        if isinstance(langkah, Exception):
            raise langkah
        if isinstance(langkah, dict):
            return KeluaranModel(json.dumps(langkah), Pemakaian(1000, 200, 50))
        return KeluaranModel(str(langkah), Pemakaian(1000, 200, 50))


@pytest.fixture
def klien_skrip():
    return KlienSkrip


@pytest.fixture
def galat_model():
    return {"timeout": ModelTimeout("t"), "sibuk": ModelSibuk("s"), "gagal": ModelGagal("g")}


def nota_json(items: list[dict], **tingkat_nota: object) -> dict:
    """Bentuk keluaran model yang sah, lengkap dengan field null bawaan."""
    dasar = {
        "terbaca": True,
        "jenis": "cetak",
        "supplier": None,
        "tanggal": None,
        "items": [
            {"nama": "X", "qty": None, "satuan": None, "harga_satuan": None, "jumlah": None, "diskon": None}
            | it
            for it in items
        ],
        "subtotal": None,
        "diskon": None,
        "pajak": None,
        "biaya_lain": None,
        "total": None,
        "catatan": None,
    }
    dasar.update(tingkat_nota)
    return dasar
