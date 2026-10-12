"""Pengaturan backend dari environment variable. Tidak ada rahasia di kode."""

from __future__ import annotations

import os
from dataclasses import dataclass


def _bool(nama: str, bawaan: bool) -> bool:
    nilai = os.environ.get(nama)
    if nilai is None:
        return bawaan
    return nilai.strip().lower() in ("1", "true", "ya", "yes", "on")


def _opsional(nama: str) -> str | None:
    nilai = os.environ.get(nama, "").strip()
    return nilai or None


@dataclass(frozen=True)
class Pengaturan:
    klien: str  # "gemini" | "tiruan"
    gemini_api_key: str | None
    model: str
    timeout_detik: float
    thinking_level: str | None
    media_resolution: str | None
    temperature: float | None
    pakai_skema: bool
    maks_ukuran_byte: int
    app_check: str  # "enforce" | "off"
    app_check_consume: bool
    tampilkan_docs: bool

    @staticmethod
    def dari_env() -> Pengaturan:
        suhu = _opsional("NOTA_TEMPERATURE")
        p = Pengaturan(
            klien=os.environ.get("NOTA_KLIEN", "gemini").strip().lower(),
            gemini_api_key=_opsional("GEMINI_API_KEY"),
            model=os.environ.get("NOTA_MODEL", "gemini-3.1-flash-lite").strip(),
            timeout_detik=float(os.environ.get("NOTA_TIMEOUT_DETIK", "30")),
            thinking_level=_opsional("NOTA_THINKING_LEVEL"),
            media_resolution=_opsional("NOTA_MEDIA_RESOLUTION"),
            temperature=float(suhu) if suhu else None,
            pakai_skema=_bool("NOTA_PAKAI_SKEMA", True),
            maks_ukuran_byte=int(float(os.environ.get("NOTA_MAKS_UKURAN_MB", "4")) * 1024 * 1024),
            app_check=os.environ.get("APP_CHECK", "enforce").strip().lower(),
            app_check_consume=_bool("APP_CHECK_CONSUME", True),
            tampilkan_docs=_bool("NOTA_DOCS", False),
        )
        p.periksa()
        return p

    def periksa(self) -> None:
        if self.klien not in ("gemini", "tiruan"):
            raise ValueError(f"NOTA_KLIEN harus 'gemini' atau 'tiruan', bukan {self.klien!r}")
        if self.klien == "gemini" and not self.gemini_api_key:
            raise ValueError("GEMINI_API_KEY wajib diisi bila NOTA_KLIEN=gemini")
        if self.app_check not in ("enforce", "off"):
            raise ValueError(f"APP_CHECK harus 'enforce' atau 'off', bukan {self.app_check!r}")
