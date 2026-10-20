package com.danibanan.notakulakan.data

import com.danibanan.notakulakan.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Mengunci parameter kompresi foto ke konfigurasi akhir evaluasi (docs/EVALUASI.md bagian 12):
 * sisi terpanjang 1024 px, JPEG 85. Mengubahnya berarti mengubah variabel yang sudah diukur; ubah bersama
 * harness (`--max-sisi`) dan dokumen.
 */
class KonfigurasiFotoTest {
    @Test
    fun sisiTerpanjang1024() = assertEquals(1024, BuildConfig.FOTO_MAX_SISI)

    @Test
    fun kualitasJpeg85() = assertEquals(85, BuildConfig.FOTO_KUALITAS)
}
