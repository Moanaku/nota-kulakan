from __future__ import annotations

from decimal import Decimal

import pytest

from conftest import nota_json
from nota.extraction import (
    EkstraksiGagal,
    JenisGagal,
    KlienGemini,
    ModelGagal,
    ModelSibuk,
    ModelTimeout,
    baca_json,
    ekstrak,
    petakan,
)
from nota.pricing import biaya_usd
from nota.validator import Kode, Tingkat

GAMBAR = b"\xff\xd8\xff" + b"0" * 10


def _kode(hasil) -> list[str]:
    return [i.kode.value for i in hasil.isu]


def test_nota_rapi_lolos_dan_angka_ditafsirkan_kode(klien_skrip):
    keluaran = nota_json(
        [
            {
                "nama": "Mie Goreng",
                "qty": "2",
                "satuan": "dus",
                "harga_satuan": "110.000",
                "jumlah": "220.000",
            },
            {"nama": "Gula", "qty": "1,5", "satuan": "kg", "harga_satuan": "Rp 14.000", "jumlah": "21.000"},
        ],
        subtotal="241.000",
        total="Rp 241.000,-",
        supplier="Toko Contoh",
        tanggal="12/10/26",
    )
    hasil = ekstrak(GAMBAR, "image/jpeg", klien_skrip([keluaran]))
    assert hasil.lolos
    assert hasil.isu == ()
    assert hasil.nota.items[0].harga_satuan == 110000
    assert hasil.nota.items[1].qty == Decimal("1.5")
    assert hasil.nota.total == 241000
    assert hasil.nota.supplier == "Toko Contoh"
    assert hasil.nota.tanggal_tertulis == "12/10/26"
    assert hasil.jenis_nota == "cetak"
    assert hasil.percobaan == 1


def test_salah_baca_60_ditandai_validator(klien_skrip):
    keluaran = nota_json(
        [{"nama": "Gula", "qty": "2", "harga_satuan": "60", "jumlah": "120.000"}], total="120.000"
    )
    hasil = ekstrak(GAMBAR, "image/jpeg", klien_skrip([keluaran]))
    assert not hasil.lolos
    assert _kode(hasil) == ["baris_tidak_cocok"]
    assert hasil.isu[0].baris == 0


def test_angka_tak_terbaca_jadi_isu_format_bukan_tebakan(klien_skrip):
    keluaran = nota_json(
        [{"nama": "Gula", "qty": "2", "harga_satuan": "6O.OOO", "jumlah": "120.000"}], total="120.000"
    )
    hasil = ekstrak(GAMBAR, "image/jpeg", klien_skrip([keluaran]))
    assert hasil.nota.items[0].harga_satuan is None
    assert not hasil.nota.items[0].harga_dihitung  # teks tak terbaca tidak boleh "dihitung"
    assert _kode(hasil) == ["format_tidak_dikenali", "field_kosong"]
    assert "6O.OOO" in hasil.isu[0].pesan


def test_harga_tidak_tertulis_dihitung_bila_habis_dibagi(klien_skrip):
    keluaran = nota_json(
        [{"nama": "Air", "qty": "3", "harga_satuan": None, "jumlah": "135.000"}], total="135.000"
    )
    hasil = ekstrak(GAMBAR, "image/jpeg", klien_skrip([keluaran]))
    item = hasil.nota.items[0]
    assert item.harga_satuan == 45000 and item.harga_dihitung
    assert hasil.lolos
    assert [(i.kode, i.tingkat) for i in hasil.isu] == [(Kode.HARGA_DIHITUNG, Tingkat.INFO)]


def test_harga_tidak_dihitung_bila_tidak_habis_dibagi(klien_skrip):
    keluaran = nota_json(
        [{"nama": "Air", "qty": "3", "harga_satuan": None, "jumlah": "100.000"}], total="100.000"
    )
    hasil = ekstrak(GAMBAR, "image/jpeg", klien_skrip([keluaran]))
    assert hasil.nota.items[0].harga_satuan is None
    assert "field_kosong" in _kode(hasil)


def test_diskon_negatif_dijadikan_potongan_positif(klien_skrip):
    keluaran = nota_json(
        [{"nama": "Minyak", "qty": "3", "harga_satuan": "18.000", "jumlah": "52.000", "diskon": "-2.000"}],
        diskon="(3.000)",
        total="49.000",
    )
    hasil = ekstrak(GAMBAR, "image/jpeg", klien_skrip([keluaran]))
    assert hasil.nota.items[0].diskon_baris == 2000
    assert hasil.nota.diskon == 3000
    assert hasil.lolos


def test_angka_numerik_dari_model_diterima_bool_ditolak(klien_skrip):
    keluaran = nota_json([{"nama": "A", "qty": 2, "harga_satuan": 500, "jumlah": 1000.0}], total=True)
    hasil = ekstrak(GAMBAR, "image/jpeg", klien_skrip([keluaran]))
    assert hasil.nota.items[0].jumlah == 1000
    assert hasil.nota.total is None
    assert _kode(hasil)[0] == "format_tidak_dikenali"


def test_keluaran_rusak_dicoba_ulang_dan_pemakaian_dijumlah(klien_skrip):
    bagus = nota_json([{"nama": "A", "qty": "1", "harga_satuan": "1.000", "jumlah": "1.000"}], total="1.000")
    klien = klien_skrip(["ini bukan json", bagus])
    hasil = ekstrak(GAMBAR, "image/jpeg", klien)
    assert hasil.percobaan == 2 and klien.panggilan == 2
    assert hasil.pemakaian.token_input == 2000  # dua panggilan dibayar


def test_keluaran_rusak_dua_kali_gagal_dengan_pemakaian(klien_skrip):
    with pytest.raises(EkstraksiGagal) as e:
        ekstrak(GAMBAR, "image/jpeg", klien_skrip(["{", '{"items": "bukan daftar"}']))
    assert e.value.jenis is JenisGagal.KELUARAN_TIDAK_VALID
    assert e.value.pemakaian.token_input == 2000


def test_pagar_kode_markdown_dibuang():
    data = baca_json('```json\n{"items": []}\n```')
    assert data["items"] == []


@pytest.mark.parametrize(
    "keluaran",
    [
        {"terbaca": False, "items": [], "total": None},
        {"terbaca": True, "items": [], "total": None},
    ],
)
def test_bukan_nota(klien_skrip, keluaran):
    with pytest.raises(EkstraksiGagal) as e:
        ekstrak(GAMBAR, "image/jpeg", klien_skrip([keluaran]))
    assert e.value.jenis is JenisGagal.BUKAN_NOTA


@pytest.mark.parametrize(
    ("nama_galat", "jenis"),
    [
        ("timeout", JenisGagal.WAKTU_HABIS),
        ("sibuk", JenisGagal.LAYANAN_SIBUK),
        ("gagal", JenisGagal.GAGAL_LAYANAN_AI),
    ],
)
def test_galat_layanan_tidak_dicoba_ulang(klien_skrip, galat_model, nama_galat, jenis):
    klien = klien_skrip([galat_model[nama_galat]])
    with pytest.raises(EkstraksiGagal) as e:
        ekstrak(GAMBAR, "image/jpeg", klien)
    assert e.value.jenis is jenis
    assert klien.panggilan == 1


def test_biaya_dihitung_dari_token_nyata(klien_skrip):
    keluaran = nota_json(
        [{"nama": "A", "qty": "1", "harga_satuan": "1.000", "jumlah": "1.000"}], total="1.000"
    )
    hasil = ekstrak(GAMBAR, "image/jpeg", klien_skrip([keluaran], nama_model="gemini-3.1-flash-lite"))
    # 1000 token input x 0,25 + (200 output + 50 thinking) x 1,50, per sejuta token
    assert hasil.biaya_usd == pytest.approx((1000 * 0.25 + 250 * 1.50) / 1_000_000)


def test_biaya_none_untuk_model_tanpa_harga():
    assert biaya_usd("model-entah", 1000, 100, 0) is None


def test_latensi_diukur_dengan_jam(klien_skrip):
    waktu = iter([10.0, 10.75])
    keluaran = nota_json(
        [{"nama": "A", "qty": "1", "harga_satuan": "1.000", "jumlah": "1.000"}], total="1.000"
    )
    hasil = ekstrak(GAMBAR, "image/jpeg", klien_skrip([keluaran]), jam=lambda: next(waktu))
    assert hasil.latensi_ms == 750


def test_petakan_nama_kosong_diberi_penanda():
    nota, _ = petakan(
        {"items": [{"nama": "  ", "qty": "1", "harga_satuan": "1", "jumlah": "1"}], "total": "1"}
    )
    assert nota.items[0].nama == "(tanpa nama)"


@pytest.mark.parametrize(
    ("kode", "diharapkan"),
    [(504, ModelTimeout), (408, ModelTimeout), (503, ModelSibuk), (429, ModelSibuk), (400, ModelGagal)],
)
def test_klien_gemini_klasifikasi_galat_http(kode, diharapkan):
    from google.genai import errors

    class Palsu:
        class models:  # noqa: N801
            @staticmethod
            def generate_content(**_):
                raise (errors.ServerError if kode >= 500 else errors.ClientError)(
                    kode, {"error": {"message": "x", "status": "X"}}
                )

    klien = KlienGemini(api_key="k", nama_model="m")
    klien._klien = Palsu()
    with pytest.raises(diharapkan):
        klien.hasilkan(b"x", "image/jpeg")
