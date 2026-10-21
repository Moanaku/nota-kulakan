package com.danibanan.notakulakan.ui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import com.danibanan.notakulakan.ui.theme.SourceSerif4
import com.danibanan.notakulakan.ui.theme.SourceSans3
import com.danibanan.notakulakan.ui.theme.TipografiNota
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TipografiTemaTest {
    private val semuaPeran: Map<String, TextStyle> = TipografiNota.let {
        mapOf(
            "displayLarge" to it.displayLarge, "displayMedium" to it.displayMedium, "displaySmall" to it.displaySmall,
            "headlineLarge" to it.headlineLarge, "headlineMedium" to it.headlineMedium,
            "headlineSmall" to it.headlineSmall,
            "titleLarge" to it.titleLarge, "titleMedium" to it.titleMedium, "titleSmall" to it.titleSmall,
            "bodyLarge" to it.bodyLarge, "bodyMedium" to it.bodyMedium, "bodySmall" to it.bodySmall,
            "labelLarge" to it.labelLarge, "labelMedium" to it.labelMedium, "labelSmall" to it.labelSmall,
        )
    }

    @Test
    fun limaBelasPeranMemakaiEnamUkuranDanDuaKeluarga() {
        assertEquals(15, semuaPeran.size)
        val ukuran = setOf(12f, 14f, 16f, 20f, 24f, 28f)
        for ((nama, gaya) in semuaPeran) {
            assertTrue("$nama ukuran ${gaya.fontSize.value}", gaya.fontSize.value in ukuran)
            assertTrue("$nama keluarga", gaya.fontFamily === SourceSans3 || gaya.fontFamily === SourceSerif4)
            assertTrue("$nama bukan font sistem", gaya.fontFamily != FontFamily.Default)
        }
    }

    @Test
    fun teksKeputusanTidakKurangDari14Sp() {
        // DESIGN.md: hanya label-kecil (12 sp) yang boleh di bawah 14 sp.
        val kecil = semuaPeran.filter { it.value.fontSize.value < 14f }.keys
        assertEquals(setOf("labelSmall"), kecil)
    }
}
