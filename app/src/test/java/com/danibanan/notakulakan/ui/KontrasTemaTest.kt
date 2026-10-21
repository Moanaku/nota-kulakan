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
 * Rasio kontras WCAG 2.x dari konstanta tema, dibandingkan dengan tabel di docs/DESIGN.md bagian 1 (v2).
 * Ambang: teks normal >= 4,5; garis kontrol (batas input, tombol sekunder) >= 3. Nilai "harapan" ditulis tangan dari tabel DESIGN.md.
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
        Pasangan("teks / latar", { it.teks }, { it.latar }, teks, 15.35, 15.61),
        Pasangan("teks / container", { it.teks }, { it.container }, teks, 13.69, 14.26),
        Pasangan("teks / container-high", { it.teks }, { it.containerHigh }, teks, 12.14, 12.77),
        Pasangan("teks-sekunder / latar", { it.teksSekunder }, { it.latar }, teks, 9.27, 11.07),
        Pasangan("teks-sekunder / container", { it.teksSekunder }, { it.container }, teks, 8.27, 10.11),
        Pasangan("teks-sekunder / container-high", { it.teksSekunder }, { it.containerHigh }, teks, 7.33, 9.05),
        Pasangan("garis-kontrol / latar", { it.garisKontrol }, { it.latar }, garis, 4.91, 6.21),
        Pasangan("garis-kontrol / container", { it.garisKontrol }, { it.container }, garis, 4.38, 5.68),
        Pasangan("garis-kontrol / container-high", { it.garisKontrol }, { it.containerHigh }, garis, 3.88, 5.08),
        Pasangan("aksen / latar", { it.aksen }, { it.latar }, teks, 6.10, 9.78),
        Pasangan("aksen / container", { it.aksen }, { it.container }, teks, 5.44, 8.93),
        Pasangan("aksen / container-high", { it.aksen }, { it.containerHigh }, teks, 4.83, 8.00),
        Pasangan("di-atas-aksen / aksen", { it.diAtasAksen }, { it.aksen }, teks, 6.42, 8.20),
        Pasangan(
            "di-atas-aksen-container / aksen-container",
            { it.diAtasAksenContainer }, { it.aksenContainer }, teks, 8.12, 8.97,
        ),
        Pasangan("aksen / aksen-container", { it.aksen }, { it.aksenContainer }, teks, 5.25, 5.99),
        Pasangan("galat / latar", { it.galat }, { it.latar }, teks, 5.67, 9.27),
        Pasangan(
            "di-atas-galat-container / galat-container",
            { it.diAtasGalatContainer }, { it.galatContainer }, teks, 8.43, 10.47,
        ),
    )

    /** Pasangan yang dipakai di aplikasi tetapi tidak ada di tabel DESIGN.md v2: hanya ambang, tanpa nilai harapan. */
    private val tanpaNilaiTabel = listOf(
        Triple("galat / container", { p: Palet -> p.galat } to { p: Palet -> p.container }, teks),
        Triple("di-atas-galat / galat", { p: Palet -> p.diAtasGalat } to { p: Palet -> p.galat }, teks),
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
    fun pasanganTanpaNilaiTabelMemenuhiAmbang() {
        for ((mode, palet) in listOf("terang" to PaletTerang, "gelap" to PaletGelap)) {
            for ((nama, fg, minimum) in tanpaNilaiTabel) {
                val r = rasio(fg.first(palet), fg.second(palet))
                assertTrue("$nama ($mode) = ${"%.2f".format(r)} < $minimum", r >= minimum)
            }
        }
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
        // DESIGN.md: pemisah 1,39 (terang) dan 1,47 (gelap) terhadap latar; dekoratif, bukan batas kontrol.
        assertEquals(1.39, rasio(PaletTerang.pemisah, PaletTerang.latar), 0.011)
        assertEquals(1.47, rasio(PaletGelap.pemisah, PaletGelap.latar), 0.011)
    }
}
