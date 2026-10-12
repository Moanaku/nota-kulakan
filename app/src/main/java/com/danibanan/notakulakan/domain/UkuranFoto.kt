package com.danibanan.notakulakan.domain

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Hitungan ukuran untuk kompresi foto. Murni supaya bisa diuji tanpa Android.
 * Perilakunya sama dengan Pillow `thumbnail` di harness evaluasi: tidak pernah memperbesar,
 * rasio tetap, dibulatkan ke piksel terdekat.
 */
object UkuranFoto {

    fun target(lebar: Int, tinggi: Int, maxSisi: Int): Pair<Int, Int> {
        require(lebar > 0 && tinggi > 0 && maxSisi > 0)
        val sisi = max(lebar, tinggi)
        if (sisi <= maxSisi) return lebar to tinggi
        return if (lebar >= tinggi) {
            maxSisi to max(1, (tinggi.toDouble() * maxSisi / lebar).roundToInt())
        } else {
            max(1, (lebar.toDouble() * maxSisi / tinggi).roundToInt()) to maxSisi
        }
    }

    /**
     * inSampleSize terbesar (pangkat 2) yang hasil decode-nya masih >= maxSisi, supaya foto
     * kamera 12-50 MP tidak dimuat penuh ke memori sebelum dikecilkan.
     */
    fun inSampleSize(lebar: Int, tinggi: Int, maxSisi: Int): Int {
        var sampel = 1
        val sisi = max(lebar, tinggi)
        while (sisi / (sampel * 2) >= maxSisi) sampel *= 2
        return sampel
    }
}
