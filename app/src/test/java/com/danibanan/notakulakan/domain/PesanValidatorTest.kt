package com.danibanan.notakulakan.domain

import com.danibanan.notakulakan.domain.model.ItemBaris
import com.danibanan.notakulakan.domain.model.KodeIsu
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

/** Teks pesan harus sama persis dengan backend (`validator.py`), memakai tanda ×. */
class PesanValidatorTest {
    @Test
    fun pesanBarisTidakCocokMemakaiTandaKali() {
        val item = ItemBaris("Gula", BigDecimal("2"), "bal", 60, 120_000)
        val pesan = Validator.periksaBaris(0, item).filter { it.kode == KodeIsu.BARIS_TIDAK_COCOK }.map { it.pesan }
        assertEquals(listOf("Baris 1 (Gula): 2 × Rp 60 = Rp 120, tapi tertulis Rp 120.000"), pesan)
    }
}
