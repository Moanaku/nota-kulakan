package com.danibanan.notakulakan.domain

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Kerangka tes pencarian nota (nota sintetis). Dani meninjau dan menambah kasusnya. */
class CariNotaTest {

    private val hari = 24L * 60 * 60 * 1000

    // Tanggal tampil palsu supaya tes tidak bergantung zona waktu atau locale: "<hari ke-n> Okt" (tanpa tahun,
    // karena pencarian substring: "2" akan cocok dengan "2026").
    private val tanggal: (Long) -> String = { "${it / hari} Okt" }

    private fun nota(id: Long, supplier: String?, vararg barang: String?, waktu: Long = id * hari) =
        RingkasNota(id, total = 100_000, jumlahBaris = barang.size, dibuatPada = waktu, supplier = supplier, namaBarang = barang.toList())

    private val a = nota(1, "Grosir Sukses", "Susu Kental Manis", "Gula Pasir")
    private val b = nota(2, "Toko Sinar Pagi", "Minyak Goreng", "Kopi Sachet")
    private val c = nota(3, "UD Makmur", "SUSU UHT 1 L")
    private val semua = listOf(a, b, c)

    private fun cari(teks: String, filter: FilterNota = FilterNota()) =
        cariNota(semua, teks, filter, tanggal).map { it.id }

    @Test
    fun satuKataSusuMenemukanNotaBerbarangSusuSaja() {
        assertEquals(listOf(1L, 3L), cari("susu"))
    }

    @Test
    fun hurufBesarKecilDanSpasiTepiTidakMempengaruhi() {
        assertEquals(cari("susu"), cari("  SUSU  "))
        assertEquals(cari("susu"), cari("SuSu"))
    }

    @Test
    fun kosongAtauSpasiSajaMengembalikanSemuaNota() {
        assertEquals(listOf(1L, 2L, 3L), cari(""))
        assertEquals(listOf(1L, 2L, 3L), cari("   "))
    }

    @Test
    fun duaKataBolehDiBidangBerbedaTapiSemuaHarusCocok() {
        // "sukses" ada di supplier nota 1, "susu" di barang nota 1.
        assertEquals(listOf(1L), cari("sukses susu"))
        assertEquals(listOf(1L), cari("sukses   susu"))
        // "sukses" cocok, "kopi" tidak ada di nota 1.
        assertEquals(emptyList<Long>(), cari("sukses kopi"))
    }

    @Test
    fun potonganTengahKataCocok() {
        assertEquals(listOf(1L, 3L), cari("usu"))
    }

    @Test
    fun tanggalSebagaimanaTampilBisaDicari() {
        assertEquals(listOf(2L), cari("2 okt"))
        assertEquals(listOf(1L, 2L, 3L), cari("okt"))
    }

    @Test
    fun filterSupplierSaja() {
        assertEquals(listOf(3L), cari("", FilterNota(supplier = "ud makmur")))
    }

    @Test
    fun filterTanggalSajaInklusif() {
        assertEquals(listOf(2L, 3L), cari("", FilterNota(dari = 2 * hari)))
        assertEquals(listOf(1L, 2L), cari("", FilterNota(sampai = 2 * hari)))
        assertEquals(listOf(2L), cari("", FilterNota(dari = 2 * hari, sampai = 2 * hari)))
    }

    @Test
    fun filterDigabungDenganKataDenganDan() {
        assertEquals(listOf(3L), cari("susu", FilterNota(supplier = "UD Makmur")))
        assertEquals(emptyList<Long>(), cari("kopi", FilterNota(supplier = "UD Makmur")))
        assertEquals(listOf(3L), cari("susu", FilterNota(dari = 3 * hari)))
    }

    @Test
    fun tidakAdaHasilBerupaDaftarKosongBukanGalat() {
        assertTrue(cariNota(semua, "tidak-ada-barang-ini", FilterNota(), tanggal).isEmpty())
        assertTrue(cariNota(emptyList(), "susu", FilterNota(), tanggal).isEmpty())
    }

    @Test
    fun namaBarangKosongAtauNullTidakCrash() {
        val aneh = nota(9, null, null, "", "Susu")
        assertEquals(listOf(9L), cariNota(listOf(aneh), "susu", FilterNota(), tanggal).map { it.id })
        assertTrue(cariNota(listOf(aneh), "kopi", FilterNota(), tanggal).isEmpty())
        assertEquals(listOf("Susu"), barangCocok(aneh, "susu"))
    }

    @Test
    fun notaTanpaSupplierTidakLolosFilterSupplier() {
        val tanpa = nota(8, null, "Susu")
        assertTrue(cariNota(listOf(tanpa), "", FilterNota(supplier = "UD Makmur"), tanggal).isEmpty())
    }

    @Test
    fun barangCocokHanyaBarangYangMengandungKataTanpaDuplikat() {
        val n = nota(5, "X", "Susu Kental", "susu kental", "Gula", "Susu UHT")
        assertEquals(listOf("Susu Kental", "Susu UHT"), barangCocok(n, "susu"))
        assertEquals(listOf("Gula"), barangCocok(n, "gula susuk"))
        assertTrue(barangCocok(n, "  ").isEmpty())
    }

    @Test
    fun daftarSupplierUnikTerurutDanMelewatiKosong() {
        val d = listOf(nota(1, "b toko"), nota(2, "A Toko"), nota(3, "B Toko "), nota(4, null), nota(5, "  "))
        assertEquals(listOf("A Toko", "b toko"), daftarSupplier(d))
    }

    @Test
    fun rentangTanggalDihitungDariHariIniDanKalender() {
        val zona = ZoneId.of("Asia/Jakarta")
        fun ms(t: String) = ZonedDateTime.parse("${t}+07:00[Asia/Jakarta]").toInstant().toEpochMilli()
        val sekarang = ms("2026-10-15T13:30:00")
        val f = FilterNota(supplier = "X")
        assertEquals(FilterNota("X", null, null), f.denganRentang(RentangTanggal.SEMUA, sekarang, zona))
        assertEquals(ms("2026-10-09T00:00:00"), f.denganRentang(RentangTanggal.TUJUH_HARI, sekarang, zona).dari)
        assertEquals(ms("2026-09-16T00:00:00"), f.denganRentang(RentangTanggal.TIGA_PULUH_HARI, sekarang, zona).dari)
        assertEquals(ms("2026-10-01T00:00:00"), f.denganRentang(RentangTanggal.BULAN_INI, sekarang, zona).dari)
        val lalu = f.denganRentang(RentangTanggal.BULAN_LALU, sekarang, zona)
        assertEquals(ms("2026-09-01T00:00:00"), lalu.dari)
        assertEquals(ms("2026-10-01T00:00:00") - 1, lalu.sampai)
        assertEquals("X", lalu.supplier)
    }
}
