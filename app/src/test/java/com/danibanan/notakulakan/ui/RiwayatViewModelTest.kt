package com.danibanan.notakulakan.ui

import com.danibanan.notakulakan.domain.RingkasNota
import com.danibanan.notakulakan.ui.riwayat.RiwayatUi
import com.danibanan.notakulakan.ui.riwayat.RiwayatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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
class RiwayatViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo = RepoBacaPalsu()

    @Before
    fun siapkan() = Dispatchers.setMain(dispatcher)

    @After
    fun bereskan() = Dispatchers.resetMain()

    private fun ringkas(id: Long) = RingkasNota(id, total = 120_000, jumlahBaris = 2, dibuatPada = 1_000_000L, supplier = "UD Makmur")

    @Test
    fun memuatMenjadiAdaDaftarNota() = runTest(dispatcher) {
        repo.fakeRiwayat = { flowOf(listOf(ringkas(1), ringkas(2))) }
        val vm = RiwayatViewModel(repo)
        backgroundScope.launch { vm.ui.collect {} }
        advanceUntilIdle()
        assertEquals(RiwayatUi.Ada(listOf(ringkas(1), ringkas(2))), vm.ui.value)
    }

    @Test
    fun daftarKosongMenjadiAdaTanpaNota() = runTest(dispatcher) {
        val vm = RiwayatViewModel(repo)
        backgroundScope.launch { vm.ui.collect {} }
        advanceUntilIdle()
        assertEquals(RiwayatUi.Ada(emptyList()), vm.ui.value)
    }

    @Test
    fun galatBacaTanpaCrashDanTanpaPesanMentah() = runTest(dispatcher) {
        repo.fakeRiwayat = { flow { throw IllegalStateException("SQLITE_CORRUPT: halaman 42") } }
        val vm = RiwayatViewModel(repo)
        backgroundScope.launch { vm.ui.collect {} }
        advanceUntilIdle()
        // Galat hanya berupa keadaan tanpa pesan; teks exception tidak sampai ke UI.
        assertEquals(RiwayatUi.Galat, vm.ui.value)
    }

    @Test
    fun cobaLagiMemuatUlangDanPulihKeSukses() = runTest(dispatcher) {
        var panggilan = 0
        repo.fakeRiwayat = {
            panggilan++
            if (panggilan == 1) flow { throw IllegalStateException("gagal sekali") } else flowOf(listOf(ringkas(7)))
        }
        val vm = RiwayatViewModel(repo)
        backgroundScope.launch { vm.ui.collect {} }
        advanceUntilIdle()
        assertEquals(RiwayatUi.Galat, vm.ui.value)

        vm.cobaLagi()
        advanceUntilIdle()
        assertEquals(2, panggilan)
        assertTrue(vm.ui.value is RiwayatUi.Ada)
        assertEquals(listOf(ringkas(7)), (vm.ui.value as RiwayatUi.Ada).daftar)
    }
}
