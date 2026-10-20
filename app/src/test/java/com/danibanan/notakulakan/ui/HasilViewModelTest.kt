package com.danibanan.notakulakan.ui

import androidx.lifecycle.SavedStateHandle
import com.danibanan.notakulakan.domain.PilihanProduk
import com.danibanan.notakulakan.domain.Produk
import com.danibanan.notakulakan.domain.RingkasNota
import com.danibanan.notakulakan.domain.StatusSimpan
import com.danibanan.notakulakan.domain.model.ItemBaris
import com.danibanan.notakulakan.domain.model.Nota
import com.danibanan.notakulakan.domain.repo.Analitik
import com.danibanan.notakulakan.domain.repo.DrafSimpan
import com.danibanan.notakulakan.domain.repo.EkstraksiRepository
import com.danibanan.notakulakan.domain.repo.HasilEkstraksi
import com.danibanan.notakulakan.domain.repo.HasilSimpan
import com.danibanan.notakulakan.domain.repo.Jam
import com.danibanan.notakulakan.domain.repo.JenisGagal
import com.danibanan.notakulakan.domain.repo.MetaEkstraksi
import com.danibanan.notakulakan.domain.repo.NotaRepository
import com.danibanan.notakulakan.domain.repo.NotaTersimpan
import com.danibanan.notakulakan.domain.repo.PenyiapFoto
import com.danibanan.notakulakan.domain.repo.StokProduk
import com.danibanan.notakulakan.ui.hasil.HasilUi
import com.danibanan.notakulakan.ui.hasil.HasilViewModel
import java.io.File
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HasilViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val notaContoh = Nota(
        items = listOf(
            ItemBaris("Mie Goreng", BigDecimal("2"), "dus", 110_000, 220_000),
            ItemBaris("Gula Pasir", BigDecimal("2"), "bal", 60, 120_000), // salah baca 60.000
        ),
        total = 340_000,
    )
    private val meta = MetaEkstraksi("id", "tiruan", "v1", null, 900)

    private class EkstraksiPalsu(var hasil: HasilEkstraksi) : EkstraksiRepository {
        var panggilan = 0
        override suspend fun ekstrak(foto: File): HasilEkstraksi {
            panggilan++
            return hasil
        }
    }

    private class RepoPalsu : NotaRepository {
        var duplikat: RingkasNota? = null
        val tersimpan = mutableListOf<DrafSimpan>()
        val alias = mutableMapOf("mie goreng" to Produk(7, "Indomie Goreng"))

        override fun stok(): Flow<List<StokProduk>> = flowOf(emptyList())
        override fun riwayat(): Flow<List<RingkasNota>> = flowOf(emptyList())
        override suspend fun detail(id: Long): NotaTersimpan? = null
        override suspend fun hapus(id: Long) = Unit
        override suspend fun hapusDataContoh(): Int = 0
        override suspend fun daftarProduk(): List<Produk> = alias.values.toList()
        override suspend fun saranProduk(namaDiNota: List<String>): List<Produk?> =
            namaDiNota.map { alias[it.lowercase()] }
        override suspend fun cariDuplikat(total: Long?, jumlahBaris: Int, sekarang: Long) = duplikat
        override suspend fun simpan(draf: DrafSimpan): HasilSimpan {
            tersimpan += draf
            return HasilSimpan(1, emptyList())
        }
    }

    private class AnalitikPalsu : Analitik {
        val disimpan = mutableListOf<Int>()
        override fun ekstraksiSelesai(lolos: Boolean, jumlahBaris: Int, latensiMs: Long, model: String) = Unit
        override fun ekstraksiGagal(jenis: JenisGagal) = Unit
        override fun notaDisimpan(jumlahBaris: Int, barisDikoreksi: Int, dikonfirmasi: Boolean, durasiDetik: Long, manual: Boolean) {
            disimpan += barisDikoreksi
        }
    }

    private val foto = object : PenyiapFoto {
        override suspend fun siapkan(uri: String): File = File.createTempFile("nota", ".jpg")
    }
    private lateinit var ekstraksi: EkstraksiPalsu
    private lateinit var repo: RepoPalsu
    private lateinit var analitik: AnalitikPalsu

    @Before
    fun siapkan() {
        Dispatchers.setMain(dispatcher)
        ekstraksi = EkstraksiPalsu(HasilEkstraksi.Berhasil(notaContoh, emptyList(), "cetak", null, meta))
        repo = RepoPalsu()
        analitik = AnalitikPalsu()
    }

    @After
    fun bereskan() = Dispatchers.resetMain()

    private fun buatVm(uri: String? = "content://foto/1") = HasilViewModel(
        SavedStateHandle(mapOf("uri" to uri)),
        ekstraksi,
        ContohPalsu(),
        repo,
        foto,
        analitik,
        Jam { 1_000_000L },
    )

    private fun HasilViewModel.draf() = (ui.value as HasilUi.Draf).draf

    @Test
    fun ekstraksiMenjadiDrafDenganSorotanDanAlias() = runTest(dispatcher) {
        val vm = buatVm()
        advanceUntilIdle()
        val d = vm.draf()
        assertEquals(StatusSimpan.PERLU_KONFIRMASI, d.status)
        assertEquals(setOf(1), d.validasi.barisBermasalah)
        assertEquals(PilihanProduk.Ada(Produk(7, "Indomie Goreng")), d.pilihan[0])
        assertEquals(PilihanProduk.Baru("Gula Pasir"), d.pilihan[1])
        assertFalse(d.bolehSimpan)
    }

    @Test
    fun koreksiBarisMembuatBisaDisimpan() = runTest(dispatcher) {
        val vm = buatVm()
        advanceUntilIdle()
        vm.ubahBaris(1, vm.draf().nota.items[1].copy(hargaSatuan = 60_000))
        advanceUntilIdle()
        assertEquals(StatusSimpan.BISA, vm.draf().status)
        assertTrue(vm.draf().bolehSimpan)

        vm.simpan()
        advanceUntilIdle()
        assertTrue(vm.ui.value is HasilUi.Tersimpan)
        assertEquals(1, repo.tersimpan.size)
        assertFalse(repo.tersimpan[0].dikonfirmasiMeskiTidakCocok)
        assertEquals(listOf(1), analitik.disimpan) // satu baris dikoreksi
    }

    @Test
    fun ketidakcocokanBisaDisimpanSetelahKonfirmasi() = runTest(dispatcher) {
        val vm = buatVm()
        advanceUntilIdle()
        vm.setKonfirmasi(true)
        vm.simpan()
        advanceUntilIdle()
        assertTrue(repo.tersimpan.single().dikonfirmasiMeskiTidakCocok)
    }

    @Test
    fun duplikatMenahanSimpanSampaiDipaksa() = runTest(dispatcher) {
        repo.duplikat = RingkasNota(3, 340_000, 2, 999_000L, null)
        val vm = buatVm()
        advanceUntilIdle()
        vm.setKonfirmasi(true)
        vm.simpan()
        advanceUntilIdle()
        assertEquals(3L, vm.draf().duplikat?.id)
        assertTrue(repo.tersimpan.isEmpty())

        vm.simpan(paksa = true)
        advanceUntilIdle()
        assertEquals(1, repo.tersimpan.size)
    }

    @Test
    fun gagalJaringanBisaDiulangTanpaKompresUlang() = runTest(dispatcher) {
        ekstraksi.hasil = HasilEkstraksi.Gagal(JenisGagal.TANPA_JARINGAN)
        val vm = buatVm()
        advanceUntilIdle()
        val gagal = vm.ui.value as HasilUi.Gagal
        assertTrue(gagal.bisaUlang)

        ekstraksi.hasil = HasilEkstraksi.Berhasil(notaContoh, emptyList(), null, null, meta)
        vm.proses()
        advanceUntilIdle()
        assertTrue(vm.ui.value is HasilUi.Draf)
        assertEquals(2, ekstraksi.panggilan)
    }

    @Test
    fun bukanNotaTidakDitawariCobaLagi() = runTest(dispatcher) {
        ekstraksi.hasil = HasilEkstraksi.Gagal(JenisGagal.BUKAN_NOTA)
        val vm = buatVm()
        advanceUntilIdle()
        assertFalse((vm.ui.value as HasilUi.Gagal).bisaUlang)
    }

    @Test
    fun isiManualTanpaFoto() = runTest(dispatcher) {
        val vm = buatVm(uri = null)
        advanceUntilIdle()
        val d = vm.draf()
        assertTrue(d.manual)
        assertEquals(StatusSimpan.TERHALANG, d.status)
        assertTrue(d.namaKosong)
        assertNull(d.fotoPath)
        assertEquals(0, ekstraksi.panggilan)
    }

    @Test
    fun hapusBarisMenggeserPilihanProduk() = runTest(dispatcher) {
        val vm = buatVm()
        advanceUntilIdle()
        vm.hapusBaris(0)
        val d = vm.draf()
        assertEquals(1, d.nota.items.size)
        assertEquals(PilihanProduk.Baru("Gula Pasir"), d.pilihan.single())
        assertEquals(setOf(0), d.barisDiubah)
    }
}
