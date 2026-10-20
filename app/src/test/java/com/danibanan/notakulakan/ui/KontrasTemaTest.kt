package com.danibanan.notakulakan.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.danibanan.notakulakan.ui.theme.Palet
import com.danibanan.notakulakan.ui.theme.PaletGelap
import com.danibanan.notakulakan.ui.theme.PaletTerang
import kotlin.math.abs
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Rasio kontras WCAG 2.x dari konstanta tema, dibandingkan dengan tabel di docs/DESIGN.md bagian 1.
 * Ambang: teks normal >= 4,5; garis kontrol >= 3. Nilai "harapan" ditulis tangan dari tabel DESIGN.md.
 */
class KontrasTemaTest {
    private class Pasangan(
        val nama: String,
        val depan: (Palet) -> Color,
        val latar: (Palet) -> Color,
        val minimum: Double,
        val terang: Double,
        val gelap: Double,
    )

    private val teks = 4.5
    private val garis = 3.0

    private val tabel = listOf(
        Pasangan("teks / latar", { it.teks }, { it.latar }, teks, 13.89, 14.36),
        Pasangan("teks / container", { it.teks }, { it.container }, teks, 12.69, 13.17),
        Pasangan("teks / container-high", { it.teks }, { it.containerHigh }, teks, 11.33, 11.75),
        Pasangan("teks-sekunder / latar", { it.teksSekunder }, { it.latar }, teks, 6.52, 7.96),
        Pasangan("teks-sekunder / container", { it.teksSekunder }, { it.container }, teks, 5.95, 7.30),
        Pasangan("teks-sekunder / container-high", { it.teksSekunder }, { it.containerHigh }, teks, 5.32, 6.51),
        Pasangan("garis-kontrol / latar", { it.garisKontrol }, { it.latar }, garis, 3.66, 4.85),
        Pasangan("garis-kontrol / container", { it.garisKontrol }, { it.container }, garis, 3.34, 4.45),
        Pasangan("aksen / latar", { it.aksen }, { it.latar }, teks, 7.08, 8.69),
        Pasangan("aksen / container", { it.aksen }, { it.container }, teks, 6.47, 7.97),
        Pasangan("di-atas-aksen / aksen", { it.diAtasAksen }, { it.aksen }, teks, 7.33, 7.46),
        Pasangan(
            "di-atas-aksen-container / aksen-container",
            { it.diAtasAksenContainer }, { it.aksenContainer }, teks, 9.84, 8.91,
        ),
        Pasangan("galat / latar", { it.galat }, { it.latar }, teks, 5.66, 8.75),
        Pasangan("galat / container", { it.galat }, { it.container }, teks, 5.17, 8.02),
        Pasangan("di-atas-galat / galat", { it.diAtasGalat }, { it.galat }, teks, 6.14, 8.23),
        Pasangan(
            "di-atas-galat-container / galat-container",
            { it.diAtasGalatContainer }, { it.galatContainer }, teks, 9.61, 10.47,
        ),
    )

    private fun cahaya(c: Color): Double {
        val argb = c.toArgb()
        fun kanal(v: Int): Double {
            val s = v / 255.0
            return if (s <= 0.04045) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * kanal((argb shr 16) and 0xFF) + 0.7152 * kanal((argb shr 8) and 0xFF) +
            0.0722 * kanal(argb and 0xFF)
    }

    private fun rasio(a: Color, b: Color): Double {
        val (terang, gelap) = listOf(cahaya(a), cahaya(b)).sortedDescending()
        return (terang + 0.05) / (gelap + 0.05)
    }

    @Test
    fun semuaPasanganMemenuhiAmbangWcag() {
        val gagal = mutableListOf<String>()
        for ((mode, palet) in listOf("terang" to PaletTerang, "gelap" to PaletGelap)) {
            for (p in tabel) {
                val r = rasio(p.depan(palet), p.latar(palet))
                if (r < p.minimum) gagal += "${p.nama} ($mode) = ${"%.2f".format(r)} < ${p.minimum}"
            }
        }
        assertTrue("Kontras di bawah ambang:\n" + gagal.joinToString("\n"), gagal.isEmpty())
    }

    @Test
    fun rasioKodeSamaDenganTabelDesignMd() {
        val selisih = mutableListOf<String>()
        for ((mode, palet) in listOf("terang" to PaletTerang, "gelap" to PaletGelap)) {
            for (p in tabel) {
                val r = rasio(p.depan(palet), p.latar(palet))
                val harapan = if (mode == "terang") p.terang else p.gelap
                if (abs(r - harapan) > 0.011) {
                    selisih += "${p.nama} ($mode): kode ${"%.3f".format(r)}, DESIGN.md $harapan"
                }
            }
        }
        assertTrue("Kode menyimpang dari DESIGN.md:\n" + selisih.joinToString("\n"), selisih.isEmpty())
    }

    @Test
    fun pemisahSengajaTipisTapiTerlihat() {
        // DESIGN.md: pemisah 1,41 (terang) dan 1,43 (gelap) terhadap latar; dekoratif, bukan batas kontrol.
        assertEquals(1.41, rasio(PaletTerang.pemisah, PaletTerang.latar), 0.011)
        assertEquals(1.43, rasio(PaletGelap.pemisah, PaletGelap.latar), 0.011)
    }
}
