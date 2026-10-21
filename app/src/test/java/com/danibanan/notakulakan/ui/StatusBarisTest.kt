package com.danibanan.notakulakan.ui

import com.danibanan.notakulakan.domain.Validator
import com.danibanan.notakulakan.domain.model.ItemBaris
import com.danibanan.notakulakan.ui.komponen.StatusBaris
import com.danibanan.notakulakan.ui.komponen.statusBaris
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pemetaan tampilan status baris (Periksa dan Detail memakai fungsi yang sama), bukan aturan hitungan. */
class StatusBarisTest {
    private fun status(item: ItemBaris) = statusBaris(item, Validator.periksaBaris(0, item))

    @Test
    fun barisLengkapDanCocok() {
        assertEquals(StatusBaris.COCOK, status(ItemBaris("Gula", BigDecimal("2"), "bal", 60, 120)))
    }

    @Test
    fun barisLengkapTapiJumlahBeda_selisih() {
        assertEquals(StatusBaris.SELISIH, status(ItemBaris("Gula", BigDecimal("2"), "bal", 60, 120_000)))
    }

    @Test
    fun barisBelumTerbaca_bukanSelisih() {
        assertEquals(StatusBaris.BELUM_TERBACA, status(ItemBaris("Kecap", null, null, null, null)))
    }

    @Test
    fun sebagianBelumTerbaca_tetapBelumTerbaca() {
        assertEquals(StatusBaris.BELUM_TERBACA, status(ItemBaris("Kecap", BigDecimal("2"), "dus", 60, null)))
    }
}
