package com.danibanan.notakulakan.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kode UI di luar `ui/theme` tidak boleh memuat warna hex, `fontSize =`, atau `.dp` di luar skala
 * spasi/radius/elevasi/ikon (0, 2, 4, 8, 12, 16, 24, 32, 48). Pelanggaran dilaporkan per berkas dan baris.
 */
class TanpaNilaiMentahTest {
    private val skala = setOf("0", "2", "4", "8", "12", "16", "24", "32", "48")
    private val dpAngka = Regex("""(?<![\w.])(\d+(?:\.\d+)?)\.dp\b""")

    // Berkas lama yang akan dimigrasi bersama layarnya (tugas migrasi berikutnya). Jangan menambah daftar ini
    // untuk kode baru; kode baru wajib memakai token dari ui/theme.
    private val akanDimigrasi = setOf(
        "ui/stok/StokScreen.kt",
        "ui/tema/Tema.kt", // tema lama, digantikan ui/theme
        "ui/komponen/Komponen.kt", // komponen lama (Lencana, Pemberitahuan, BarBawah, FotoNota)
    )

    private fun akar(): File = listOf("src/main/java", "app/src/main/java").map(::File).first { it.isDirectory }

    private fun pelanggaran(f: File): List<String> =
        f.readLines().mapIndexedNotNull { i, baris ->
            val kode = baris.substringBefore("//")
            val masalah = buildList {
                if (Regex("""Color\(\s*0x""").containsMatchIn(kode)) add("Color(0x")
                if (Regex("""fontSize\s*=""").containsMatchIn(kode)) add("fontSize =")
                dpAngka.findAll(kode).filter { it.groupValues[1] !in skala }.forEach { add("${it.groupValues[1]}.dp") }
            }
            if (masalah.isEmpty()) null else "${f.name}:${i + 1} ${masalah.joinToString()}"
        }

    @Test
    fun kodeBaruTidakMemakaiNilaiMentah() {
        val dasar = akar().resolve("com/danibanan/notakulakan")
        val berkas = dasar.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue("Tidak menemukan sumber Kotlin di $dasar", berkas.size > 10)
        val gagal = berkas
            .filter { f ->
                val rel = f.relativeTo(dasar).invariantSeparatorsPath
                !rel.startsWith("ui/theme/") && rel !in akanDimigrasi
            }
            .flatMap(::pelanggaran)
        assertTrue("Nilai mentah di luar ui/theme:\n" + gagal.joinToString("\n"), gagal.isEmpty())
    }

    @Test
    fun daftarPengecualianTidakMenyimpanBerkasBersih() {
        // Pengecualian harus jujur: berkas yang sudah bersih tidak boleh tetap dikecualikan.
        val dasar = akar().resolve("com/danibanan/notakulakan")
        val bersih = akanDimigrasi.filter { pelanggaran(dasar.resolve(it)).isEmpty() }
        assertTrue("Berkas bersih masih dikecualikan: $bersih", bersih.isEmpty())
    }
}
