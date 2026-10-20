package com.danibanan.notakulakan.ui

import androidx.lifecycle.SavedStateHandle
import com.danibanan.notakulakan.domain.model.ItemBaris
import com.danibanan.notakulakan.domain.model.Nota
import com.danibanan.notakulakan.domain.repo.NotaTersimpan
import com.danibanan.notakulakan.ui.riwayat.DetailUi
import com.danibanan.notakulakan.ui.riwayat.DetailViewModel
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo = RepoBacaPalsu()

    @Before
    fun siapkan() = Dispatchers.setMain(dispatcher)

    @After
    fun bereskan() = Dispatchers.resetMain()

    private val tersimpan = NotaTersimpan(
        id = 5,
        nota = Nota(items = listOf(ItemBaris("Minyak Goreng", BigDecimal("2"), "btl", 30_000, 60_000)), total = 60_000),
        namaProduk = listOf("Minyak Goreng"),
        dibuatPada = 1_000_000L,
        fotoPath = null,
        dikonfirmasiMeskiTidakCocok = false,
    )

    private fun buatVm() = DetailViewModel(SavedStateHandle(mapOf("id" to 5L)), repo)

    @Test
    fun memuatMenjadiAdaNota() = runTest(dispatcher) {
        repo.fakeDetail = { tersimpan }
        val vm = buatVm()
        advanceUntilIdle()
        assertEquals(DetailUi.Ada(tersimpan), vm.ui.value)
    }

    @Test
    fun notaTidakDitemukanMenjadiTidakAda() = runTest(dispatcher) {
        val vm = buatVm()
        advanceUntilIdle()
        assertEquals(DetailUi.TidakAda, vm.ui.value)
    }

    @Test
    fun galatBacaTanpaCrashDanTanpaPesanMentah() = runTest(dispatcher) {
        repo.fakeDetail = { throw IllegalStateException("SQLITE_CORRUPT: halaman 42") }
        val vm = buatVm()
        advanceUntilIdle()
        // Galat hanya berupa keadaan tanpa pesan; teks exception tidak sampai ke UI.
        assertEquals(DetailUi.Galat, vm.ui.value)
    }

    @Test
    fun cobaLagiMemuatUlangDanPulihKeSukses() = runTest(dispatcher) {
        var panggilan = 0
        repo.fakeDetail = {
            panggilan++
            if (panggilan == 1) throw IllegalStateException("gagal sekali") else tersimpan
        }
        val vm = buatVm()
        advanceUntilIdle()
        assertEquals(DetailUi.Galat, vm.ui.value)

        vm.muat()
        advanceUntilIdle()
        assertEquals(2, panggilan)
        assertTrue(vm.ui.value is DetailUi.Ada)
        assertEquals(tersimpan, (vm.ui.value as DetailUi.Ada).nota)
    }
}
