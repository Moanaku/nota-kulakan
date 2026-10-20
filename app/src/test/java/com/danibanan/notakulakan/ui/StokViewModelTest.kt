package com.danibanan.notakulakan.ui

import com.danibanan.notakulakan.domain.RingkasNota
import com.danibanan.notakulakan.domain.repo.StokProduk
import com.danibanan.notakulakan.ui.stok.StokUi
import com.danibanan.notakulakan.ui.stok.StokViewModel
import java.math.BigDecimal
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
class StokViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo = RepoBacaPalsu()

    @Before
    fun siapkan() = Dispatchers.setMain(dispatcher)

    @After
    fun bereskan() = Dispatchers.resetMain()

    private val gula = StokProduk(1, "Gula Pasir Kristal Putih Kemasan 1 Kg", "kg", BigDecimal("10"), 14_500, 500)
    private val nota = RingkasNota(9, total = 145_000, jumlahBaris = 1, dibuatPada = 1_000_000L, supplier = "UD Makmur")

    @Test
    fun memuatMenjadiAdaStokDanNotaTerakhir() = runTest(dispatcher) {
        repo.fakeStok = { flowOf(listOf(gula)) }
        repo.fakeRiwayat = { flowOf(listOf(nota)) }
        val vm = StokViewModel(repo)
        backgroundScope.launch { vm.ui.collect {} }
        advanceUntilIdle()
        assertEquals(StokUi.Ada(listOf(gula), nota), vm.ui.value)
    }

    @Test
    fun kosongMenjadiAdaTanpaBarangDanTanpaNota() = runTest(dispatcher) {
        val vm = StokViewModel(repo)
        backgroundScope.launch { vm.ui.collect {} }
        advanceUntilIdle()
        assertEquals(StokUi.Ada(emptyList(), null), vm.ui.value)
    }

    @Test
    fun galatBacaTanpaCrashDanTanpaPesanMentah() = runTest(dispatcher) {
        repo.fakeStok = { flow { throw IllegalStateException("SQLITE_CORRUPT: halaman 42") } }
        val vm = StokViewModel(repo)
        backgroundScope.launch { vm.ui.collect {} }
        advanceUntilIdle()
        assertEquals(StokUi.Galat, vm.ui.value)
    }

    @Test
    fun cobaLagiMemuatUlangDanPulihKeSukses() = runTest(dispatcher) {
        var panggilan = 0
        repo.fakeStok = {
            panggilan++
            if (panggilan == 1) flow { throw IllegalStateException("gagal sekali") } else flowOf(listOf(gula))
        }
        val vm = StokViewModel(repo)
        backgroundScope.launch { vm.ui.collect {} }
        advanceUntilIdle()
        assertEquals(StokUi.Galat, vm.ui.value)

        vm.cobaLagi()
        advanceUntilIdle()
        assertEquals(2, panggilan)
        assertTrue(vm.ui.value is StokUi.Ada)
        assertEquals(listOf(gula), (vm.ui.value as StokUi.Ada).stok)
    }
}
