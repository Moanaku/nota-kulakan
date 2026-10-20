from __future__ import annotations

import pytest

from nota.config import Pengaturan


def test_model_bawaan_adalah_konfigurasi_akhir_evaluasi(monkeypatch: pytest.MonkeyPatch):
    """Konfigurasi akhir docs/EVALUASI.md bagian 12; mengubahnya berarti mengubah variabel terukur."""
    monkeypatch.delenv("NOTA_MODEL", raising=False)
    monkeypatch.setenv("NOTA_KLIEN", "tiruan")
    assert Pengaturan.dari_env().model == "gemini-3.1-flash-lite"
