"""Harga model untuk menghitung biaya per nota.

Biaya dihitung dari token yang DILAPORKAN API di setiap respons, bukan dari perkiraan.
Harga diambil dari halaman resmi dan HARUS diperbarui sebelum pengukuran di README.

Sumber   : https://ai.google.dev/gemini-api/docs/pricing
Dibaca   : 2026-10-20
Catatan  : halaman menyebut harga berbayar ini berlaku sampai 31 Desember 2026, dan harga
           gemini-3.8-flash dan gemini-3.6-flash naik per 1 Januari 2027 (1,50/7,50).
           Harga output sudah termasuk token thinking. Tidak ada tarif gambar terpisah;
           gambar memakai tarif masuk.
"""

from __future__ import annotations

from dataclasses import dataclass

SUMBER_HARGA = "https://ai.google.dev/gemini-api/docs/pricing"
TANGGAL_HARGA = "2026-10-20"


@dataclass(frozen=True)
class HargaModel:
    input_per_juta_usd: float  # teks/gambar
    output_per_juta_usd: float  # termasuk thinking


HARGA: dict[str, HargaModel] = {
    "gemini-3.8-flash": HargaModel(0.75, 3.75),
    "gemini-3.6-flash": HargaModel(0.75, 3.75),
    "gemini-3.5-flash-lite": HargaModel(0.30, 2.50),
    "gemini-3.1-flash-lite": HargaModel(0.25, 1.50),
    "gemini-3-flash-preview": HargaModel(0.50, 3.00),
}


def biaya_usd(model: str, token_input: int, token_output: int, token_thinking: int) -> float | None:
    """Biaya satu panggilan dalam USD, atau None kalau harga model tidak diketahui."""
    harga = HARGA.get(model)
    if harga is None:
        return None
    return (
        token_input * harga.input_per_juta_usd + (token_output + token_thinking) * harga.output_per_juta_usd
    ) / 1_000_000
