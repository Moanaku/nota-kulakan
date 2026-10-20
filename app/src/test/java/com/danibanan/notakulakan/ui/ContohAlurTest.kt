package com.danibanan.notakulakan.ui

import androidx.lifecycle.SavedStateHandle
import com.danibanan.notakulakan.data.contoh.bacaContoh
import com.danibanan.notakulakan.domain.PilihanProduk
import com.danibanan.notakulakan.domain.Produk
import com.danibanan.notakulakan.domain.RingkasNota
import com.danibanan.notakulakan.domain.StatusSimpan
import com.danibanan.notakulakan.domain.Validator
import com.danibanan.notakulakan.domain.model.KodeIsu
import com.danibanan.notakulakan.domain.repo.Analitik
import com.danibanan.notakulakan.domain.repo.ContohRepository
import com.danibanan.notakulakan.domain.repo.DrafSimpan
import com.danibanan.notakulakan.domain.repo.EkstraksiRepository
import com.danibanan.notakulakan.domain.repo.HasilEkstraksi
import com.danibanan.notakulakan.domain.repo.HasilSimpan
import com.danibanan.notakulakan.domain.repo.IdContoh
import com.danibanan.notakulakan.domain.repo.Jam
import com.danibanan.notakulakan.domain.repo.JenisGagal
import com.danibanan.notakulakan.domain.repo.NotaContoh
import com.danibanan.notakulakan.domain.repo.NotaRepository
import com.danibanan.notakulakan.domain.repo.NotaTersimpan
import com.danibanan.notakulakan.domain.repo.PREFIKS_CONTOH
import com.danibanan.notakulakan.domain.repo.PenyiapFoto
import com.danibanan.notakulakan.domain.repo.StokProduk
import com.danibanan.notakulakan.domain.repo.adalahDataContoh
import com.danibanan.notakulakan.domain.statusSimpan
import com.danibanan.notakulakan.ui.hasil.HasilUi
import com.danibanan.notakulakan.ui.hasil.HasilViewModel
import com.danibanan.notakulakan.ui.riwayat.RiwayatUi
import com.danibanan.notakulakan.ui.riwayat.RiwayatViewModel
import java.io.File
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

internal fun folderContoh(): File =
    listOf("src/main/assets/contoh", "app/src/main/assets/contoh").map(::File).first { it.isDirectory }

private val jsonContoh = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

internal fun hasilContoh(id: IdContoh): HasilEkstraksi.Berhasil =
    bacaContoh(jsonContoh, File(folderContoh(), "${id.berkas}.json").readText())

/** Membaca berkas contoh asli dari assets, seperti implementasi di APK, tanpa Context. */
internal class ContohPalsu : ContohRepository {
    var dimuat = 0
    override suspend fun muat(id: IdContoh): NotaContoh {
        dimuat++
        return NotaContoh(hasilContoh(id), File.createTempFile("contoh", ".jpg"))
    }
}

/** Repositori dalam memori yang menghitung stok dari baris nota, seperti basis data (stok tidak disimpan). */
internal class RepoMemori : NotaRepository {
    class Baris(val produk: String, val qty: BigDecimal, val satuan: String?)
    class Catatan(val id: Long, val supplier: String?, val total: Long, val baris: List<Baris>)

    val catatan = MutableStateFlow<List<Catatan>>(emptyList())
    var galatHapusContoh = false
    private var berikutnya = 1L

    override fun riwayat(): Flow<List<RingkasNota>> =
        catatan.map { daftar -> daftar.map { RingkasNota(it.id, it.total, it.baris.size, 1_000_000L, it.supplier) } }

    override fun stok(): Flow<List<StokProduk>> = catatan.map { daftar ->
        daftar.flatMap { it.baris }.groupBy { it.produk to it.satuan }.map { (kunci, baris) ->
            StokProduk(0, kunci.first, kunci.second, baris.fold(BigDecimal.ZERO) { a, b -> a + b.qty }, null, null)
        }
    }

    override suspend fun simpan(draf: DrafSimpan): HasilSimpan {
        val id = berikutnya++
        val baris = draf.nota.items.map { Baris(it.nama, it.qty!!, it.satuan) }
        catatan.value = catatan.value + Catatan(id, draf.nota.supplier, draf.nota.total!!, baris)
        return HasilSimpan(id, emptyList())
    }

    override suspend fun hapusDataContoh(): Int {
        if (galatHapusContoh) error("basis data gagal")
        val (contoh, sisa) = catatan.value.partition { adalahDataContoh(it.supplier) }
        catatan.value = sisa
        return contoh.size
    }

    override suspend fun detail(id: Long): NotaTersimpan? = null
    override suspend fun hapus(id: Long) = Unit
    override suspend fun daftarProduk(): List<Produk> = emptyList()
    override suspend fun saranProduk(namaDiNota: List<String>): List<Produk?> = namaDiNota.map { null }
    override suspend fun cariDuplikat(total: Long?, jumlahBaris: Int, sekarang: Long): RingkasNota? = null
}

@OptIn(ExperimentalCoroutinesApi::class)
class ContohAlurTest {

    private val dispatcher = StandardTestDispatcher()

    private class EkstraksiTerlarang : EkstraksiRepository {
        var panggilan = 0
        override suspend fun ekstrak(foto: File): HasilEkstraksi {
            panggilan++
            return HasilEkstraksi.Gagal(JenisGagal.TANPA_JARINGAN)
        }
    }

    private class FotoTerlarang : PenyiapFoto {
        var panggilan = 0
        override suspend fun siapkan(uri: String): File {
            panggilan++
            error("mode contoh tidak boleh menyiapkan foto dari URI")
        }
    }

    private class AnalitikPalsu : Analitik {
        var jumlahDicatat = 0
        override fun ekstraksiSelesai(lolos: Boolean, jumlahBaris: Int, latensiMs: Long, model: String) {
            jumlahDicatat++
        }
        override fun ekstraksiGagal(jenis: JenisGagal) {
            jumlahDicatat++
        }
        override fun notaDisimpan(jumlahBaris: Int, barisDikoreksi: Int, dikonfirmasi: Boolean, durasiDetik: Long, manual: Boolean) {
            jumlahDicatat++
        }
    }

    private val ekstraksi = EkstraksiTerlarang()
    private val foto = FotoTerlarang()
    private val analitik = AnalitikPalsu()
    private val contoh = ContohPalsu()
    private val repo = RepoMemori()

    @Before
    fun siapkan() = Dispatchers.setMain(dispatcher)

    @After
    fun bereskan() = Dispatchers.resetMain()

    private fun buatVm(id: IdContoh) = HasilViewModel(
        SavedStateHandle(mapOf("contoh" to id.berkas)),
        ekstraksi,
        contoh,
        repo,
        foto,
        analitik,
        Jam { 1_000_000L },
    )

    private fun HasilViewModel.draf() = (ui.value as HasilUi.Draf).draf

    // --- data contoh ---------------------------------------------------------------------------

    @Test
    fun notaBersihTanpaIsuValidator() {
        val nota = hasilContoh(IdContoh.BERSIH).nota
        assertEquals(emptyList<Any>(), Validator.validasi(nota).isu)
        assertEquals(570_000L, nota.total)
        assertEquals(StatusSimpan.BISA, statusSimpan(Validator.validasi(nota)))
    }

    @Test
    fun notaSelisihPunyaTepatSatuIsuDanCocokSetelahKoreksi() {
        val nota = hasilContoh(IdContoh.SELISIH).nota
        val isu = Validator.validasi(nota).isu
        assertEquals(1, isu.size)
        assertEquals(KodeIsu.BARIS_TIDAK_COCOK, isu[0].kode)
        assertEquals(0, isu[0].baris)
        assertEquals(60L, nota.items[0].hargaSatuan) // "60" di berkas mentah, bukan 60.000
        val dikoreksi = nota.copy(items = nota.items.mapIndexed { i, it -> if (i == 0) it.copy(hargaSatuan = 60_000) else it })
        assertEquals(emptyList<Any>(), Validator.validasi(dikoreksi).isu)
    }

    @Test
    fun notaDiskonSesuaiHitungan() {
        val nota = hasilContoh(IdContoh.DISKON).nota
        assertEquals(emptyList<Any>(), Validator.validasi(nota).isu)
        assertEquals(10_000L, nota.items[0].diskonBaris)
        assertEquals(15_000L, nota.diskon)
        assertEquals(25_000L, nota.biayaLain)
        assertEquals(660_000L, nota.total)
    }

    @Test
    fun gambarContohAdaDanKecil() {
        for (id in IdContoh.entries) {
            val gambar = File(folderContoh(), "${id.berkas}.jpg")
            assertTrue("${gambar.name} tidak ada", gambar.isFile)
            assertTrue("${gambar.name} melebihi 80 KB", gambar.length() <= 80 * 1024)
        }
    }

    // --- alur di ViewModel -----------------------------------------------------------------------

    @Test
    fun jalurContohTidakMemanggilJaringanAtauPenyiapFoto() = runTest(dispatcher) {
        val vm = buatVm(IdContoh.SELISIH)
        advanceUntilIdle()
        val d = vm.draf()
        assertEquals(1, contoh.dimuat)
        assertEquals(0, ekstraksi.panggilan)
        assertEquals(0, foto.panggilan)
        assertTrue(d.contoh)
        assertNull(d.catatanModel) // tidak ada "Catatan dari pembaca nota" palsu
        assertNull(d.meta)
        assertEquals(setOf(0), d.validasi.barisBermasalah)
    }

    @Test
    fun koreksiLaluSimpanMemberiSupplierBerawalanContohTanpaAnalitik() = runTest(dispatcher) {
        val vm = buatVm(IdContoh.SELISIH)
        advanceUntilIdle()
        vm.ubahBaris(0, vm.draf().nota.items[0].copy(hargaSatuan = 60_000))
        advanceUntilIdle()
        assertEquals(StatusSimpan.BISA, vm.draf().status)
        vm.simpan()
        advanceUntilIdle()
        assertTrue(vm.ui.value is HasilUi.Tersimpan)
        assertEquals("${PREFIKS_CONTOH}Grosir Contoh Makmur", repo.catatan.value.single().supplier)
        assertEquals(0, analitik.jumlahDicatat)
    }

    @Test
    fun tandaContohTetapAdaWalauSupplierDiubahAtauDikosongkan() = runTest(dispatcher) {
        val vm = buatVm(IdContoh.BERSIH)
        advanceUntilIdle()
        val n = vm.draf().nota
        vm.ubahTingkatNota(n.total, n.subtotal, n.diskon, n.pajak, n.biayaLain, supplier = "Toko Saya")
        vm.simpan()
        advanceUntilIdle()
        assertEquals("${PREFIKS_CONTOH}Toko Saya", repo.catatan.value.single().supplier)

        val vm2 = buatVm(IdContoh.BERSIH)
        advanceUntilIdle()
        val n2 = vm2.draf().nota
        vm2.ubahTingkatNota(n2.total, n2.subtotal, n2.diskon, n2.pajak, n2.biayaLain, supplier = null)
        vm2.simpan(paksa = true)
        advanceUntilIdle()
        val kedua = repo.catatan.value.last().supplier
        assertEquals("${PREFIKS_CONTOH}Nota contoh", kedua)
        assertTrue(adalahDataContoh(kedua))
    }

    @Test
    fun hapusDataContohHanyaMenghapusYangBerawalanDanMemperbaruiStok() = runTest(dispatcher) {
        // Satu nota nyata (tanpa tanda) dan dua nota contoh; Mie Goreng ada di nota nyata dan nota contoh bersih.
        repo.simpan(
            DrafSimpan(
                hasilContoh(IdContoh.BERSIH).nota.copy(supplier = "UD Nyata"),
                List(3) { PilihanProduk.Baru("x") },
                null,
                false,
                null,
            ),
        )
        for (id in listOf(IdContoh.BERSIH, IdContoh.DISKON)) {
            val vm = buatVm(id)
            advanceUntilIdle()
            vm.simpan(paksa = true)
            advanceUntilIdle()
        }
        assertEquals(3, repo.catatan.value.size)

        val vm = RiwayatViewModel(repo)
        backgroundScope.launch { vm.ui.collect {} }
        val stok = mutableListOf<List<StokProduk>>()
        backgroundScope.launch { repo.stok().collect { stok += it } }
        advanceUntilIdle()
        assertEquals(0, BigDecimal("4").compareTo(stok.last().first { it.nama == "Mie Goreng" }.totalMasuk))
        assertTrue(stok.last().any { it.nama == "Sabun Cuci" })

        vm.hapusDataContoh()
        advanceUntilIdle()

        assertFalse(vm.galatHapusContoh.value)
        val sisa = (vm.ui.value as RiwayatUi.Ada).daftar
        assertEquals(listOf("UD Nyata"), sisa.map { it.supplier })
        // Stok ikut turun: barang yang hanya ada di nota contoh hilang; yang dari nota nyata tetap.
        assertTrue(stok.last().none { it.nama == "Sabun Cuci" })
        assertEquals(0, BigDecimal("2").compareTo(stok.last().first { it.nama == "Mie Goreng" }.totalMasuk))
    }

    @Test
    fun hapusDataContohGagalMenandaiGalatTanpaMenghapus() = runTest(dispatcher) {
        val vm = buatVm(IdContoh.BERSIH)
        advanceUntilIdle()
        vm.simpan(paksa = true)
        advanceUntilIdle()
        repo.galatHapusContoh = true
        val riwayat = RiwayatViewModel(repo)
        riwayat.hapusDataContoh()
        advanceUntilIdle()
        assertTrue(riwayat.galatHapusContoh.value)
        assertEquals(1, repo.catatan.value.size)
    }
}
