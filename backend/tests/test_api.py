from __future__ import annotations

import pytest
from fastapi.testclient import TestClient

from conftest import nota_json
from nota.api import buat_app, deteksi_mime
from nota.config import Pengaturan
from nota.tiruan import KlienTiruan

JPEG = b"\xff\xd8\xff\xe0" + b"0" * 100
PNG = b"\x89PNG\r\n\x1a\n" + b"0" * 100
WEBP = b"RIFF\x00\x00\x00\x00WEBPVP8 " + b"0" * 100


def _pengaturan(**ubah) -> Pengaturan:
    dasar = dict(
        klien="tiruan",
        gemini_api_key=None,
        model="tiruan",
        timeout_detik=5,
        thinking_level=None,
        media_resolution=None,
        temperature=None,
        pakai_skema=True,
        maks_ukuran_byte=1024,
        app_check="enforce",
        app_check_consume=True,
        tampilkan_docs=False,
    )
    dasar.update(ubah)
    return Pengaturan(**dasar)


class VerifikatorPalsu:
    def __init__(self, token_sah: str = "token-sah"):
        self.token_sah = token_sah
        self.diterima: list[str] = []

    def __call__(self, token: str) -> None:
        self.diterima.append(token)
        if token != self.token_sah:
            raise ValueError("token tidak sah")


@pytest.fixture
def verif():
    return VerifikatorPalsu()


def _klien(app_klien=None, verifikator=None, **ubah) -> TestClient:
    app = buat_app(_pengaturan(**ubah), klien=app_klien or KlienTiruan(), verifikator=verifikator)
    return TestClient(app)


def _kirim(tc: TestClient, data: bytes = JPEG, token: str | None = "token-sah"):
    header = {"X-Firebase-AppCheck": token} if token else {}
    return tc.post("/v1/ekstrak", files={"foto": ("nota.jpg", data, "image/jpeg")}, headers=header)


def test_sukses_bentuk_respons(verif):
    r = _kirim(_klien(verifikator=verif))
    assert r.status_code == 200
    body = r.json()
    assert set(body) == {"nota", "isu", "lolos", "jenis_nota", "catatan", "meta"}
    assert body["meta"]["model"] == "tiruan"
    assert body["meta"]["id_permintaan"]
    # Contoh tiruan sengaja berisi satu salah baca (gula 60) dan satu harga dihitung.
    kode = [(i["kode"], i["tingkat"], i["baris"]) for i in body["isu"]]
    assert ("baris_tidak_cocok", "galat", 2) in kode
    assert ("harga_dihitung", "info", 3) in kode
    assert body["lolos"] is False
    assert body["nota"]["items"][1]["qty"] == "2"
    assert verif.diterima == ["token-sah"]


def test_tanpa_token_app_check_ditolak(verif):
    r = _kirim(_klien(verifikator=verif), token=None)
    assert r.status_code == 401
    assert r.json()["galat"]["kode"] == "app_check_ditolak"


def test_token_salah_ditolak(verif):
    r = _kirim(_klien(verifikator=verif), token="palsu")
    assert r.status_code == 401


def test_app_check_off_tanpa_token():
    r = _kirim(_klien(app_check="off"), token=None)
    assert r.status_code == 200


def test_tanpa_field_foto_400(verif):
    r = _klien(verifikator=verif).post("/v1/ekstrak", headers={"X-Firebase-AppCheck": "token-sah"})
    assert r.status_code == 400
    assert r.json()["galat"]["kode"] == "permintaan_tidak_valid"


def test_bukan_gambar_400(verif):
    r = _kirim(_klien(verifikator=verif), data=b"%PDF-1.7 bukan foto")
    assert r.status_code == 400
    assert r.json()["galat"]["kode"] == "foto_tidak_valid"


def test_terlalu_besar_413(verif):
    r = _kirim(_klien(verifikator=verif), data=JPEG + b"0" * 2000)
    assert r.status_code == 413


@pytest.mark.parametrize(
    ("nama_galat", "status", "kode"),
    [("timeout", 504, "waktu_habis"), ("sibuk", 503, "layanan_sibuk"), ("gagal", 502, "gagal_layanan_ai")],
)
def test_galat_layanan(verif, klien_skrip, galat_model, nama_galat, status, kode):
    r = _kirim(_klien(app_klien=klien_skrip([galat_model[nama_galat]]), verifikator=verif))
    assert r.status_code == status
    assert r.json()["galat"]["kode"] == kode
    if kode == "layanan_sibuk":
        assert r.headers["Retry-After"] == "10"


def test_bukan_nota_422(verif, klien_skrip):
    r = _kirim(_klien(app_klien=klien_skrip([{"terbaca": False, "items": []}]), verifikator=verif))
    assert r.status_code == 422
    assert r.json()["galat"]["kode"] == "bukan_nota"


def test_keluaran_rusak_502(verif, klien_skrip):
    r = _kirim(_klien(app_klien=klien_skrip(["x", "y"]), verifikator=verif))
    assert r.status_code == 502
    assert r.json()["galat"]["kode"] == "keluaran_ai_tidak_valid"


def test_nota_lolos(verif, klien_skrip):
    keluaran = nota_json(
        [{"nama": "A", "qty": "1", "harga_satuan": "1.000", "jumlah": "1.000"}], total="1.000"
    )
    r = _kirim(_klien(app_klien=klien_skrip([keluaran]), verifikator=verif))
    assert r.status_code == 200 and r.json()["lolos"] is True


def test_healthz_dan_docs_mati(verif):
    tc = _klien(verifikator=verif)
    assert tc.get("/healthz").json()["status"] == "ok"
    assert tc.get("/docs").status_code == 404
    assert tc.get("/openapi.json").status_code == 404


@pytest.mark.parametrize(
    ("data", "mime"),
    [(JPEG, "image/jpeg"), (PNG, "image/png"), (WEBP, "image/webp"), (b"GIF89a", None), (b"", None)],
)
def test_deteksi_mime_dari_isi(data, mime):
    assert deteksi_mime(data) == mime


def test_pengaturan_menolak_gemini_tanpa_kunci():
    with pytest.raises(ValueError):
        _pengaturan(klien="gemini", gemini_api_key=None).periksa()
    with pytest.raises(ValueError):
        _pengaturan(app_check="mungkin").periksa()
