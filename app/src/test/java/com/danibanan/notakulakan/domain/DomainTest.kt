package com.danibanan.notakulakan.domain

import com.danibanan.notakulakan.domain.model.HasilValidasi
import com.danibanan.notakulakan.domain.model.Isu
import com.danibanan.notakulakan.domain.model.ItemBaris
import com.danibanan.notakulakan.domain.model.KodeIsu
import com.danibanan.notakulakan.domain.model.Nota
import com.danibanan.notakulakan.domain.model.Tingkat
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AngkaTest {
    @Test
    fun formatRupiah() {
        assertEquals("Rp 0", Rupiah.format(0))
        assertEquals("Rp 999", Rupiah.format(999))
        assertEquals("Rp 60.000", Rupiah.format(60_000))
        assertEquals("Rp 1.250.000", Rupiah.format(1_250_000))
        assertEquals("-Rp 5.000", Rupiah.format(-5_000))
        assertEquals("60.000", Rupiah.formatTanpaRp(60_000))
    }

    @Test
    fun formatLaluParseKembali() {
        for (n in listOf(0L, 1, 999, 1000, 60_000, 1_250_000, 987_654_321)) {
            assertEquals(n, Rupiah.parse(Rupiah.format(n)))
            assertEquals(n, Rupiah.parse(Rupiah.formatTanpaRp(n)))
        }
    }

    @Test
    fun formatQty() {
        assertEquals("2", Qty.format(BigDecimal("2")))
        assertEquals("1,5", Qty.format(BigDecimal("1.500")))
        assertEquals("1000", Qty.format(BigDecimal("1000")))
        assertEquals(BigDecimal("1.5"), Qty.parse(Qty.format(BigDecimal("1.5"))))
    }

    @Test
    fun parseAtauNull() {
        assertNull(Rupiah.parseAtauNull("6O.000"))
        assertNull(Qty.parseAtauNull("1/2"))
        assertEquals(60_000L, Rupiah.parseAtauNull("60.000"))
    }
}

class StatusSimpanTest {
    private fun isu(kode: KodeIsu, tingkat: Tingkat = Tingkat.GALAT) = Isu(kode, "x", tingkat = tingkat)

    @Test
    fun lolosBisaDisimpan() {
        assertEquals(StatusSimpan.BISA, statusSimpan(HasilValidasi(emptyList())))
        assertEquals(StatusSimpan.BISA, statusSimpan(HasilValidasi(listOf(isu(KodeIsu.HARGA_DIHITUNG, Tingkat.INFO)))))
    }

    @Test
    fun ketidakcocokanPerluKonfirmasi() {
        for (kode in listOf(KodeIsu.BARIS_TIDAK_COCOK, KodeIsu.SUBTOTAL_TIDAK_COCOK, KodeIsu.TOTAL_TIDAK_COCOK)) {
            assertEquals(kode.name, StatusSimpan.PERLU_KONFIRMASI, statusSimpan(HasilValidasi(listOf(isu(kode)))))
        }
    }

    @Test
    fun dataTidakLengkapTerhalang() {
        val v = HasilValidasi(listOf(isu(KodeIsu.TOTAL_TIDAK_COCOK), isu(KodeIsu.FIELD_KOSONG)))
        assertEquals(StatusSimpan.TERHALANG, statusSimpan(v))
        assertEquals(StatusSimpan.TERHALANG, statusSimpan(Validator.validasi(Nota.KOSONG)))
    }

    @Test
    fun koreksiBarisMembuatLolos() {
        val salah = Nota(listOf(ItemBaris("Gula", BigDecimal("2"), "bal", 60, 120_000)), total = 120_000)
        assertEquals(StatusSimpan.PERLU_KONFIRMASI, statusSimpan(Validator.validasi(salah)))
        val benar = salah.copy(items = listOf(salah.items[0].copy(hargaSatuan = 60_000)))
        assertEquals(StatusSimpan.BISA, statusSimpan(Validator.validasi(benar)))
    }
}

class ProdukTest {
    @Test
    fun kunciAliasMenormalkan() {
        assertEquals("indomie grg", kunciAlias("  Indomie   Grg. "))
        assertEquals("aqua 600ml", kunciAlias("AQUA-600ml"))
        assertEquals(kunciAlias("Gula Pasir 1kg"), kunciAlias("gula pasir (1kg)"))
    }

    @Test
    fun perubahanHargaHanyaSatuanSama() {
        val riwayat = listOf(
            CatatanHarga(1, "dus", 108_000, waktu = 100, notaId = 1),
            CatatanHarga(1, "pcs", 3_000, waktu = 150, notaId = 2),
            CatatanHarga(1, "Dus", 110_000, waktu = 200, notaId = 3),
            CatatanHarga(2, "karton", 210_000, waktu = 200, notaId = 3),
        )
        val hasil = hitungPerubahanHarga(riwayat)
        assertEquals(110_000L, hasil[1]!!.hargaSekarang)
        assertEquals(2_000L, hasil[1]!!.selisih)
        assertNull(hasil[2]!!.selisih)
    }

    @Test
    fun perubahanHargaAbaikanBarisLainDiNotaYangSama() {
        val riwayat = listOf(
            CatatanHarga(1, "dus", 100, waktu = 10, notaId = 5),
            CatatanHarga(1, "dus", 120, waktu = 10, notaId = 5),
        )
        assertNull(hitungPerubahanHarga(riwayat)[1]!!.selisih)
    }

    @Test
    fun duplikatTotalDanBarisSamaDalamJendela() {
        val hari = 24L * 60 * 60 * 1000
        val sekarang = 100 * hari
        val lama = listOf(
            RingkasNota(1, 895_000, 4, sekarang - 20 * hari, null),
            RingkasNota(2, 895_000, 4, sekarang - 3 * hari, "Grosir"),
            RingkasNota(3, 895_000, 5, sekarang - 1 * hari, null),
        )
        assertEquals(2L, cariMiripDuplikat(895_000, 4, sekarang, lama)?.id)
        assertNull(cariMiripDuplikat(895_000, 2, sekarang, lama))
        assertNull(cariMiripDuplikat(null, 4, sekarang, lama))
    }

    @Test
    fun qtyMilliBolakBalik() {
        assertEquals(1500L, qtyKeMilli(BigDecimal("1.5")))
        assertEquals(0, BigDecimal("1.5").compareTo(milliKeQty(1500)))
        assertEquals(0, BigDecimal("2").compareTo(milliKeQty(2000)))
    }
}

class UkuranFotoTest {
    @Test
    fun tidakMemperbesar() {
        assertEquals(800 to 600, UkuranFoto.target(800, 600, 1600))
    }

    @Test
    fun mengecilkanSamaDenganHarnessEvaluasi() {
        // Sama dengan tes Pillow di backend/tests/test_eval.py.
        assertEquals(1600 to 1067, UkuranFoto.target(3000, 2000, 1600))
        assertEquals(1067 to 1600, UkuranFoto.target(2000, 3000, 1600))
    }

    @Test
    fun inSampleSizeTidakTurunDiBawahTarget() {
        assertEquals(1, UkuranFoto.inSampleSize(3000, 2000, 1600))
        assertEquals(2, UkuranFoto.inSampleSize(4000, 3000, 1600))
        assertEquals(4, UkuranFoto.inSampleSize(8160, 6120, 1600))
        val s = UkuranFoto.inSampleSize(8160, 6120, 1600)
        assertTrue(8160 / s >= 1600)
    }
}
