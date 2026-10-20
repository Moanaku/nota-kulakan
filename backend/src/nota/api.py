"""API backend Nota Kulakan.

POST /v1/ekstrak   multipart, field "foto" (JPEG/PNG/WebP), header X-Firebase-AppCheck
GET  /healthz      cek hidup

Keputusan keamanan dan privasi:
- Kunci Gemini hanya ada di sini (environment / Secret Manager), tidak pernah di aplikasi.
- Setiap permintaan wajib membawa token App Check. Secara bawaan token bersifat sekali pakai
  (limited-use) dan dikonsumsi saat verifikasi, sehingga token yang disadap tidak bisa
  diputar ulang. Biayanya satu panggilan jaringan tambahan per permintaan.
- Kode tidak menulis foto ke disk dan log tidak memuat isi nota; log hanya berisi angka (token,
  latensi, jumlah baris, lolos/tidak). Unggahan multipart sementara dapat ditampung framework
  (Starlette) di berkas sementara bila > 1 MB.
- Jenis file ditentukan dari isi file (magic bytes), bukan dari header yang dikirim klien.

Jalankan lokal tanpa API key dan tanpa App Check:
  NOTA_KLIEN=tiruan APP_CHECK=off uvicorn nota.api:app --reload --port 8080
"""

from __future__ import annotations

import json
import logging
import sys
import uuid
from collections.abc import Callable
from typing import Any

from fastapi import Depends, FastAPI, File, Header, Request, UploadFile
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from .config import Pengaturan
from .extraction import EkstraksiGagal, JenisGagal, KlienGemini, KlienModel, ekstrak
from .prompt import VERSI_PROMPT
from .serialize import isu_ke_dict, nota_ke_dict
from .tiruan import KlienTiruan

log = logging.getLogger("nota.api")

Verifikator = Callable[[str], None]


class _FormatJson(logging.Formatter):
    """Satu baris JSON per log, dibaca Cloud Logging sebagai log terstruktur."""

    def format(self, record: logging.LogRecord) -> str:
        data: dict[str, Any] = {"severity": record.levelname, "message": record.getMessage()}
        tambahan = getattr(record, "data", None)
        if isinstance(tambahan, dict):
            data.update(tambahan)
        return json.dumps(data, ensure_ascii=False)


def _siapkan_log() -> None:
    akar = logging.getLogger()
    if not any(isinstance(h.formatter, _FormatJson) for h in akar.handlers):
        h = logging.StreamHandler(sys.stdout)
        h.setFormatter(_FormatJson())
        akar.handlers = [h]
        akar.setLevel(logging.INFO)


def deteksi_mime(data: bytes) -> str | None:
    if data.startswith(b"\xff\xd8\xff"):
        return "image/jpeg"
    if data.startswith(b"\x89PNG\r\n\x1a\n"):
        return "image/png"
    if len(data) >= 12 and data[:4] == b"RIFF" and data[8:12] == b"WEBP":
        return "image/webp"
    return None


def _galat(status: int, kode: str, pesan: str, header: dict[str, str] | None = None) -> JSONResponse:
    return JSONResponse(status_code=status, content={"galat": {"kode": kode, "pesan": pesan}}, headers=header)


_STATUS_GAGAL: dict[JenisGagal, int] = {
    JenisGagal.WAKTU_HABIS: 504,
    JenisGagal.LAYANAN_SIBUK: 503,
    JenisGagal.GAGAL_LAYANAN_AI: 502,
    JenisGagal.KELUARAN_TIDAK_VALID: 502,
    JenisGagal.BUKAN_NOTA: 422,
}


class _AppCheckDitolak(Exception):
    pass


def verifikator_firebase(consume: bool) -> Verifikator:
    """Verifikator token App Check memakai Firebase Admin SDK (kredensial bawaan Cloud Run)."""
    import firebase_admin
    from firebase_admin import app_check

    try:
        firebase_admin.get_app()
    except ValueError:
        firebase_admin.initialize_app()

    def verifikasi(token: str) -> None:
        klaim = app_check.verify_token(token, consume=consume)
        if consume and klaim.get("already_consumed"):
            raise _AppCheckDitolak("token sudah pernah dipakai")

    return verifikasi


def buat_klien(p: Pengaturan) -> KlienModel:
    if p.klien == "tiruan":
        return KlienTiruan()
    assert p.gemini_api_key
    return KlienGemini(
        api_key=p.gemini_api_key,
        nama_model=p.model,
        timeout_detik=p.timeout_detik,
        thinking_level=p.thinking_level,
        media_resolution=p.media_resolution,
        temperature=p.temperature,
        pakai_skema=p.pakai_skema,
    )


def buat_app(
    pengaturan: Pengaturan | None = None,
    klien: KlienModel | None = None,
    verifikator: Verifikator | None = None,
) -> FastAPI:
    p = pengaturan or Pengaturan.dari_env()
    _siapkan_log()
    model = klien or buat_klien(p)

    if p.app_check == "off":
        log.warning("APP_CHECK=off: permintaan tidak diverifikasi. Hanya untuk pengembangan lokal.")
        verif: Verifikator | None = None
    else:
        verif = verifikator or verifikator_firebase(p.app_check_consume)

    app = FastAPI(
        title="Nota Kulakan API",
        docs_url="/docs" if p.tampilkan_docs else None,
        redoc_url=None,
        openapi_url="/openapi.json" if p.tampilkan_docs else None,
    )

    @app.exception_handler(RequestValidationError)
    async def _permintaan_tidak_valid(_: Request, __: RequestValidationError) -> JSONResponse:
        return _galat(400, "permintaan_tidak_valid", "Permintaan harus berisi field 'foto'")

    @app.exception_handler(_AppCheckDitolak)
    async def _app_check_ditolak(_: Request, e: _AppCheckDitolak) -> JSONResponse:
        return _galat(401, "app_check_ditolak", "Aplikasi tidak dikenali")

    def periksa_app_check(
        token: str | None = Header(default=None, alias="X-Firebase-AppCheck"),
    ) -> None:
        if verif is None:
            return
        if not token:
            raise _AppCheckDitolak("token kosong")
        try:
            verif(token)
        except _AppCheckDitolak:
            raise
        except Exception as e:  # ValueError, galat JWT, galat jaringan Firebase
            log.warning("app check gagal", extra={"data": {"jenis": type(e).__name__}})
            raise _AppCheckDitolak(type(e).__name__) from e

    @app.get("/healthz")
    def healthz() -> dict[str, str]:
        return {"status": "ok", "versi_prompt": VERSI_PROMPT}

    @app.post("/v1/ekstrak", dependencies=[Depends(periksa_app_check)])
    def ekstrak_nota(foto: UploadFile = File(...)) -> JSONResponse:
        # Endpoint sengaja sinkron: FastAPI menjalankannya di threadpool, karena panggilan
        # model memblokir. Satu instance Cloud Run tetap bisa melayani beberapa permintaan.
        id_permintaan = uuid.uuid4().hex[:12]
        data = foto.file.read(p.maks_ukuran_byte + 1)
        if len(data) > p.maks_ukuran_byte:
            return _galat(413, "foto_terlalu_besar", "Ukuran foto melebihi batas")
        mime = deteksi_mime(data)
        if mime is None:
            return _galat(400, "foto_tidak_valid", "File bukan foto JPEG, PNG, atau WebP")

        try:
            hasil = ekstrak(data, mime, model)
        except EkstraksiGagal as e:
            log.info(
                "ekstraksi gagal",
                extra={
                    "data": {
                        "id": id_permintaan,
                        "jenis": e.jenis.value,
                        "model": e.model,
                        "token_input": e.pemakaian.token_input,
                        "token_output": e.pemakaian.token_output,
                    }
                },
            )
            header = {"Retry-After": "10"} if e.jenis is JenisGagal.LAYANAN_SIBUK else None
            return _galat(_STATUS_GAGAL[e.jenis], e.jenis.value, e.pesan, header)

        log.info(
            "ekstraksi selesai",
            extra={
                "data": {
                    "id": id_permintaan,
                    "model": hasil.model,
                    "versi_prompt": hasil.versi_prompt,
                    "baris": len(hasil.nota.items),
                    "lolos": hasil.lolos,
                    "isu": len(hasil.isu),
                    "token_input": hasil.pemakaian.token_input,
                    "token_output": hasil.pemakaian.token_output,
                    "token_thinking": hasil.pemakaian.token_thinking,
                    "biaya_usd": hasil.biaya_usd,
                    "latensi_ms": hasil.latensi_ms,
                    "percobaan": hasil.percobaan,
                    "ukuran_byte": len(data),
                }
            },
        )
        return JSONResponse(
            {
                "nota": nota_ke_dict(hasil.nota),
                "isu": [isu_ke_dict(i) for i in hasil.isu],
                "lolos": hasil.lolos,
                "jenis_nota": hasil.jenis_nota,
                "catatan": hasil.catatan,
                "meta": {
                    "id_permintaan": id_permintaan,
                    "model": hasil.model,
                    "versi_prompt": hasil.versi_prompt,
                    "token_input": hasil.pemakaian.token_input,
                    "token_output": hasil.pemakaian.token_output,
                    "token_thinking": hasil.pemakaian.token_thinking,
                    "biaya_usd": hasil.biaya_usd,
                    "latensi_ms": hasil.latensi_ms,
                    "percobaan": hasil.percobaan,
                },
            }
        )

    return app


def __getattr__(nama: str) -> Any:
    # `uvicorn nota.api:app` membuat app dari environment saat pertama diakses,
    # sehingga import modul ini di tes tidak membutuhkan environment produksi.
    if nama == "app":
        aplikasi = buat_app()
        globals()["app"] = aplikasi
        return aplikasi
    raise AttributeError(nama)
