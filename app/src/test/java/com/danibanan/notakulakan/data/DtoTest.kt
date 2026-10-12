package com.danibanan.notakulakan.data

import com.danibanan.notakulakan.data.remote.EkstrakRespons
import com.danibanan.notakulakan.data.remote.jenisGagalDari
import com.danibanan.notakulakan.data.remote.keDomain
import com.danibanan.notakulakan.domain.model.KodeIsu
import com.danibanan.notakulakan.domain.model.Tingkat
import com.danibanan.notakulakan.domain.repo.JenisGagal
import java.math.BigDecimal
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DtoTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    // Bentuk persis respons backend (lihat backend/tests/test_api.py), ditambah field asing.
    private val contoh = """
        {
          "nota": {
            "items": [
              {"nama": "Gula", "qty": "1.5", "satuan": "kg", "harga_satuan": 14000, "jumlah": 21000,
               "diskon_baris": 0, "harga_dihitung": false},
              {"nama": "Air", "qty": "rusak", "satuan": null, "harga_satuan": null, "jumlah": 135000,
               "diskon_baris": 0, "harga_dihitung": false}
            ],
            "subtotal": null, "diskon": 0, "pajak": 0, "biaya_lain": 0, "pembulatan": 0,
            "total": 156000, "supplier": "Grosir", "tanggal_tertulis": "12/10"
          },
          "isu": [
            {"kode": "field_kosong", "tingkat": "galat", "pesan": "x", "baris": 1, "diharapkan": null, "ditemukan": null},
            {"kode": "kode_masa_depan", "tingkat": "info", "pesan": "y", "baris": null}
          ],
          "lolos": false,
          "jenis_nota": "tulis",
          "catatan": null,
          "meta": {"id_permintaan": "abc", "model": "gemini-3.1-flash-lite", "versi_prompt": "v1",
                   "token_input": 1000, "biaya_usd": 0.0006, "latensi_ms": 1800, "percobaan": 1},
          "field_baru_server": 123
        }
    """.trimIndent()

    @Test
    fun responsDipetakanKeDomain() {
        val hasil = json.decodeFromString(EkstrakRespons.serializer(), contoh).keDomain()
        assertEquals(0, BigDecimal("1.5").compareTo(hasil.nota.items[0].qty))
        assertNull(hasil.nota.items[1].qty) // qty rusak -> null, nanti ditandai validator
        assertEquals(156_000L, hasil.nota.total)
        assertEquals("Grosir", hasil.nota.supplier)
        assertEquals(KodeIsu.FIELD_KOSONG, hasil.isuServer[0].kode)
        assertEquals(KodeIsu.LAINNYA, hasil.isuServer[1].kode)
        assertEquals(Tingkat.INFO, hasil.isuServer[1].tingkat)
        assertEquals(1800L, hasil.meta.latensiMs)
        assertTrue(hasil.meta.biayaUsd!! > 0)
    }

    @Test
    fun kodeGalatServerDiutamakan() {
        assertEquals(JenisGagal.BUKAN_NOTA, jenisGagalDari(422, "bukan_nota"))
        assertEquals(JenisGagal.LAYANAN_SIBUK, jenisGagalDari(503, "layanan_sibuk"))
        assertEquals(JenisGagal.APLIKASI_DITOLAK, jenisGagalDari(401, "app_check_ditolak"))
        assertEquals(JenisGagal.GANGGUAN_SERVER, jenisGagalDari(502, "keluaran_ai_tidak_valid"))
    }

    @Test
    fun statusHttpSebagaiCadangan() {
        assertEquals(JenisGagal.APLIKASI_DITOLAK, jenisGagalDari(403, null))
        assertEquals(JenisGagal.WAKTU_HABIS, jenisGagalDari(504, null))
        assertEquals(JenisGagal.FOTO_TERLALU_BESAR, jenisGagalDari(413, null))
        assertEquals(JenisGagal.LAYANAN_SIBUK, jenisGagalDari(429, null))
        assertEquals(JenisGagal.GANGGUAN_SERVER, jenisGagalDari(500, "tidak_dikenal"))
    }
}
