from __future__ import annotations

import io
import json
from dataclasses import replace
from decimal import Decimal

import pytest

from conftest import nota_json
from nota.extraction import EkstraksiGagal, ModelGagal, ModelSibuk
from nota.schema import ItemBaris, Nota
from nota.serialize import nota_ke_dict
from nota_eval.cord import CordTidakBisa, petakan_cord
from nota_eval.dataset import Kasus, baca_manifest, bagi_split, tulis_manifest
from nota_eval.jalankan import Konfigurasi, ekstrak_tahan_kuota, jalankan_evaluasi, pralabel, siapkan_gambar
from nota_eval.metrik import konsisten, nilai_kasus, ringkas, sejajarkan, tabel_markdown, wilson


def _item(nama, qty="1", harga=1000, jumlah=1000):
    return ItemBaris(nama, None if qty is None else Decimal(qty), None, harga, jumlah)


BENAR = Nota(
    items=(_item("Indomie Goreng", "2", 110000, 220000), _item("Aqua 600ml", "1", 45000, 45000)), total=265000
)


# --- penyejajaran dan metrik ---------------------------------------------------


def test_sejajarkan_toleran_urutan_dan_singkatan():
    prediksi = (_item("Aqua 600 ml"), _item("Indomie Grg"))
    assert sejajarkan(prediksi, BENAR.items) == [(1, 0), (0, 1)]


def test_sejajarkan_nama_berbeda_jauh_tidak_dipasangkan():
    assert sejajarkan((_item("Rokok"),), BENAR.items) == []


def test_kasus_sempurna_dan_lolos_validator():
    h = nilai_kasus("a", "cetak", "k", BENAR, BENAR, True, None, 0.001, 900, 300_000)
    assert h.sempurna and h.kategori_validator == "benar_lolos"
    assert (h.qty_benar, h.harga_benar, h.jumlah_benar) == (2, 2, 2)


def test_salah_harga_tertangkap_validator():
    salah = Nota(items=(_item("Indomie Goreng", "2", 110, 220000), BENAR.items[1]), total=265000)
    h = nilai_kasus("a", "cetak", "k", BENAR, salah, False, None, None, None, 1)
    assert not h.sempurna and h.harga_benar == 1
    assert h.kategori_validator == "tertangkap"


def test_salah_tapi_lolos_dihitung_terlewat():
    salah = Nota(items=(BENAR.items[0],), total=220000)  # satu baris hilang, total ikut salah
    h = nilai_kasus("a", "cetak", "k", BENAR, salah, True, None, None, None, 1)
    assert h.baris_terlewat == 1 and h.kategori_validator == "terlewat"


def test_alarm_palsu():
    h = nilai_kasus("a", "cetak", "k", BENAR, BENAR, False, None, None, None, 1)
    assert h.kategori_validator == "alarm_palsu"


def test_nota_asli_tidak_konsisten_dikeluarkan_dari_matriks_validator():
    nota_salah_hitung = Nota(items=(_item("A", "2", 1000, 2500),), total=2500)
    assert not konsisten(nota_salah_hitung)
    h = nilai_kasus("a", "tulis", "k", nota_salah_hitung, nota_salah_hitung, False, None, None, None, 1)
    assert h.sempurna and h.kategori_validator is None


def test_kebenaran_tanpa_harga_satuan_tetap_konsisten():
    assert konsisten(Nota(items=(_item("A", "2", None, 2000),), total=2000))


def test_kasus_gagal():
    h = nilai_kasus("a", "cetak", "k", BENAR, None, None, "waktu_habis", None, None, 1)
    assert not h.sempurna and h.baris_terlewat == 2 and h.kategori_validator is None


# --- definisi nota sempurna (diperketat 2026-10-19) ------------------------------

DISKON_PAJAK = Nota(
    items=(
        ItemBaris("Indomie Goreng", Decimal("2"), "dus", 110000, 219000, diskon_baris=1000),
        _item("Aqua 600ml", "1", 45000, 45000),
    ),
    pajak=26400,
    total=290400,
)


def _nilai(benar, prediksi):
    return nilai_kasus("a", "cetak", "k", benar, prediksi, True, None, None, None, 1)


def test_sempurna_a_dua_nota_sama_persis():
    h = _nilai(DISKON_PAJAK, DISKON_PAJAK)
    assert h.sempurna and h.pajak_benar
    assert (h.diskon_baris_benar, h.diskon_baris_n) == (2, 2)
    tabel = tabel_markdown(ringkas([h]))
    assert "| Diskon baris | Pajak |" in tabel and "| 100% (2/2) | 100% (1/1) |" in tabel


def test_sempurna_b_beda_hanya_diskon_baris_satu_baris():
    geser = replace(
        DISKON_PAJAK, items=(replace(DISKON_PAJAK.items[0], diskon_baris=0), DISKON_PAJAK.items[1])
    )
    h = _nilai(DISKON_PAJAK, geser)
    assert not h.sempurna and h.diskon_baris_benar == 1
    assert (h.qty_benar, h.harga_benar, h.jumlah_benar) == (2, 2, 2) and h.total_benar


def test_sempurna_c_beda_hanya_pajak():
    h = _nilai(DISKON_PAJAK, replace(DISKON_PAJAK, pajak=0))
    assert not h.sempurna and not h.pajak_benar and h.total_benar


def test_sempurna_d_kunci_subtotal_null_model_mengisi():
    assert DISKON_PAJAK.subtotal is None
    assert _nilai(DISKON_PAJAK, replace(DISKON_PAJAK, subtotal=264000)).sempurna


def test_sempurna_e_subtotal_beda_tidak_dibandingkan():
    benar = replace(DISKON_PAJAK, subtotal=264000)
    assert _nilai(benar, replace(benar, subtotal=999)).sempurna


def test_sempurna_f_jumlah_baris_beda():
    h = _nilai(DISKON_PAJAK, replace(DISKON_PAJAK, items=DISKON_PAJAK.items[:1]))
    assert not h.sempurna and h.baris_terlewat == 1


def test_wilson_masuk_akal():
    bawah, atas = wilson(45, 50)
    assert 0.78 < bawah < 0.9 < atas < 0.97
    assert wilson(0, 0) == (0.0, 0.0)


def test_ringkasan_dan_tabel():
    hasil = [
        nilai_kasus("a", "cetak", "m@1600px", BENAR, BENAR, True, None, 0.0004, 800, 200_000),
        nilai_kasus("b", "tulis", "m@1600px", BENAR, None, None, "bukan_nota", 0.0001, None, 100_000),
    ]
    r = ringkas(hasil)
    semua = next(x for x in r if x.jenis == "semua")
    assert (semua.n, semua.sempurna, semua.gagal) == (2, 1, 1)
    tabel = tabel_markdown(r, kurs_idr=16000)
    assert "m@1600px" in tabel and "| semua | 2 |" in tabel and "Rp" in tabel


# --- dataset ---------------------------------------------------------------------


def test_bagi_split_seimbang_dan_tidak_mengubah_yang_sudah_dibagi(tmp_path):
    kasus = [Kasus(f"c{i}", tmp_path / "x", tmp_path / "y", "cetak", None) for i in range(10)]
    kasus += [Kasus(f"t{i}", tmp_path / "x", tmp_path / "y", "tulis", None) for i in range(10)]
    kasus.append(Kasus("lama", tmp_path / "x", tmp_path / "y", "tulis", "uji"))
    hasil = bagi_split(kasus, jumlah_dev=6, seed=1)
    dev = [k for k in hasil if k.split == "dev"]
    assert len(dev) == 6
    assert sum(k.jenis == "cetak" for k in dev) == 3
    assert next(k for k in hasil if k.id == "lama").split == "uji"
    assert bagi_split(kasus, 6, 1) == hasil  # bisa diulang


def test_manifest_bolak_balik_dan_validasi(tmp_path):
    m = tmp_path / "manifest.jsonl"
    m.write_text('{"id": "a", "gambar": "a.jpg", "kebenaran": "a.json", "jenis": "cetak", "split": "dev"}\n')
    kasus = baca_manifest(m)
    assert kasus[0].gambar == tmp_path / "a.jpg"
    tulis_manifest(m, kasus)
    assert baca_manifest(m) == kasus
    m.write_text('{"id": "a", "gambar": "a.jpg", "kebenaran": "a.json", "jenis": "foto", "split": "dev"}\n')
    with pytest.raises(ValueError):
        baca_manifest(m)


# --- CORD (data contoh SINTETIS dalam format CORD, bukan sampel asli) ----------------


def test_petakan_cord_sintetis():
    gt = {
        "menu": [
            {"nm": "ES TEH", "cnt": "2", "unitprice": "5,000", "price": "10,000"},
            {"nm": ["NASI", "GORENG"], "cnt": "1 x", "price": "25.000"},
        ],
        "sub_total": {"subtotal_price": "35,000", "tax_price": "3,500", "service_price": "1,750"},
        "total": {"total_price": "40,250", "cashprice": "50,000"},
    }
    nota = petakan_cord(gt)
    assert [i.nama for i in nota.items] == ["ES TEH", "NASI GORENG"]
    assert nota.items[0].harga_satuan == 5000 and nota.items[1].harga_satuan is None
    assert nota.items[1].qty == Decimal("1")
    assert (nota.subtotal, nota.pajak, nota.biaya_lain, nota.total) == (35000, 3500, 1750, 40250)
    assert konsisten(nota)


@pytest.mark.parametrize(
    "gt",
    [
        {"menu": []},
        {"menu": [{"nm": "A", "cnt": "1", "price": "1.0.0"}], "total": {"total_price": "1"}},
        {"menu": [{"nm": "A", "cnt": "1", "price": "1", "sub": {"nm": "x", "price": "500"}}]},
        {"menu": [{"nm": "A", "price": "1"}], "total": {"total_price": ["1", "2"]}},
    ],
)
def test_petakan_cord_menolak_yang_ragu(gt):
    with pytest.raises(CordTidakBisa):
        petakan_cord(gt)


# --- ujung ke ujung dengan klien palsu ---------------------------------------------


def _foto(path):
    from PIL import Image

    buf = io.BytesIO()
    Image.new("RGB", (3000, 2000), "white").save(buf, format="JPEG")
    path.write_bytes(buf.getvalue())


def test_siapkan_gambar_mengecilkan_tanpa_memperbesar(tmp_path):
    from PIL import Image

    _foto(tmp_path / "a.jpg")
    data, mime = siapkan_gambar(tmp_path / "a.jpg", 1600, 85)
    assert mime == "image/jpeg"
    assert Image.open(io.BytesIO(data)).size == (1600, 1067)
    data, _ = siapkan_gambar(tmp_path / "a.jpg", 5000, 85)
    assert Image.open(io.BytesIO(data)).size == (3000, 2000)


def _set_kasus(tmp_path, kebenaran: dict | None):
    _foto(tmp_path / "a.jpg")
    if kebenaran is not None:
        (tmp_path / "a.json").write_text(json.dumps(kebenaran))
    return [Kasus("a", tmp_path / "a.jpg", tmp_path / "a.json", "cetak", "dev")]


def test_jalankan_evaluasi_ujung_ke_ujung(tmp_path, klien_skrip):
    benar = Nota(items=(_item("Gula", "2", 60000, 120000),), total=120000)
    kasus = _set_kasus(tmp_path, nota_ke_dict(benar))
    keluaran = nota_json(
        [{"nama": "Gula", "qty": "2", "harga_satuan": "60", "jumlah": "120.000"}], total="120.000"
    )
    hasil = jalankan_evaluasi(
        kasus,
        [Konfigurasi("gemini-3.1-flash-lite", 1600)],
        tmp_path / "keluar",
        buat_klien=lambda _: klien_skrip([keluaran]),
        cetak=lambda _: None,
    )
    assert hasil[0].kategori_validator == "tertangkap"
    assert (tmp_path / "keluar" / "hasil.csv").exists()
    assert "Versi prompt" in (tmp_path / "keluar" / "ringkasan.md").read_text()
    assert list((tmp_path / "keluar" / "prediksi").rglob("a.json"))


def test_pralabel_ditandai_dan_ditolak_saat_evaluasi(tmp_path, klien_skrip):
    kasus = _set_kasus(tmp_path, None)
    keluaran = nota_json(
        [{"nama": "Gula", "qty": "2", "harga_satuan": "60.000", "jumlah": "120.000"}], total="120.000"
    )
    n = pralabel(
        kasus, Konfigurasi("m", 0), buat_klien=lambda _: klien_skrip([keluaran]), cetak=lambda _: None
    )
    assert n == 1
    assert "_status" in json.loads((tmp_path / "a.json").read_text())
    with pytest.raises(SystemExit):
        jalankan_evaluasi(
            kasus, [Konfigurasi("m", 0)], tmp_path / "k", buat_klien=lambda _: None, cetak=lambda _: None
        )


def test_pralabel_mencetak_angka_tidak_terbaca_tanpa_mengubah_draf(tmp_path, klien_skrip):
    kasus = _set_kasus(tmp_path, None)
    keluaran = nota_json(
        [{"nama": "Gula", "qty": "2", "harga_satuan": "60.000", "jumlah": "120.000"}],
        pajak="13.2,5",
        total="120.000",
    )
    baris: list[str] = []
    pralabel(kasus, Konfigurasi("m", 0), buat_klien=lambda _: klien_skrip([keluaran]), cetak=baris.append)
    tak_terbaca = [b for b in baris if "13.2,5" in b]
    assert len(tak_terbaca) == 2  # per nota, lalu di ringkasan akhir
    assert tak_terbaca[0].startswith("a: angka tidak terbaca:") and "pajak" in tak_terbaca[0]
    assert any(b.startswith("Angka tidak terbaca di 1 nota") for b in baris)
    draf = json.loads((tmp_path / "a.json").read_text())
    assert draf["pajak"] == 0 and "isu" not in draf


# --- kuota tier gratis -------------------------------------------------------------

_GULA = nota_json(
    [{"nama": "Gula", "qty": "2", "harga_satuan": "60.000", "jumlah": "120.000"}], total="120.000"
)


def test_kuota_sibuk_ditunggu_berlipat_lalu_berhasil(klien_skrip):
    klien = klien_skrip([ModelSibuk("429"), ModelSibuk("429"), _GULA])
    tidur: list[float] = []
    hasil = ekstrak_tahan_kuota(b"x", "image/jpeg", klien, 3, 30.0, tidur.append, lambda _: None)
    assert hasil.lolos and klien.panggilan == 3
    assert tidur == [30.0, 60.0]


def test_kuota_tetap_habis_menghentikan_proses(klien_skrip):
    klien = klien_skrip([ModelSibuk("429")] * 3)
    with pytest.raises(SystemExit, match="kuota"):
        ekstrak_tahan_kuota(b"x", "image/jpeg", klien, 2, 1.0, lambda _: None, lambda _: None)
    assert klien.panggilan == 3


def test_galat_selain_kuota_tidak_diulang(klien_skrip):
    klien = klien_skrip([ModelGagal("400")])
    with pytest.raises(EkstraksiGagal):
        ekstrak_tahan_kuota(b"x", "image/jpeg", klien, 3, 1.0, lambda _: None, lambda _: None)
    assert klien.panggilan == 1


def test_pralabel_kuota_habis_tidak_menulis_templat_kosong(tmp_path, klien_skrip):
    kasus = _set_kasus(tmp_path, None)
    with pytest.raises(SystemExit):
        pralabel(
            kasus,
            Konfigurasi("m", 0),
            buat_klien=lambda _: klien_skrip([ModelSibuk("429")] * 2),
            cetak=lambda _: None,
            ulang_sibuk=1,
            tidur=lambda _: None,
        )
    assert not (tmp_path / "a.json").exists()


def test_jeda_dipakai_antar_kasus(tmp_path, klien_skrip):
    benar = Nota(items=(_item("Gula", "2", 60000, 120000),), total=120000)
    kasus = _set_kasus(tmp_path, nota_ke_dict(benar))
    tidur: list[float] = []
    jalankan_evaluasi(
        kasus,
        [Konfigurasi("gemini-3.1-flash-lite", 1600)],
        tmp_path / "keluar",
        buat_klien=lambda _: klien_skrip([ModelSibuk("429"), _GULA]),
        jeda_detik=5.0,
        cetak=lambda _: None,
        tidur=tidur.append,
    )
    assert tidur == [30.0, 5.0]
