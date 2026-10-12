package com.danibanan.notakulakan.domain

import java.math.BigDecimal

/** Teks tidak bisa dibaca sebagai angka dengan yakin. */
class AngkaTidakValid(pesan: String) : IllegalArgumentException(pesan)

/**
 * Cermin dari backend/src/nota/money.py. Dipakai untuk masukan pengguna di layar koreksi,
 * supaya "60.000" yang diketik dibaca sama persis seperti di backend.
 * Kedua versi dijaga shared/test-vectors.json. Jangan ubah satu sisi saja.
 */
object Rupiah {
    private val AWALAN = Regex("^(?:rp\\.?|idr)\\s*", RegexOption.IGNORE_CASE)
    private val AKHIRAN_TANPA_SEN = Regex("[,.]-+$")
    private val ANGKA_DAN_PEMISAH = Regex("[0-9.,]+")

    fun parse(teks: String): Long {
        var s = teks.trim()
        var negatif = false
        if (s.startsWith("(") && s.endsWith(")") && s.length >= 2) {
            negatif = true
            s = s.substring(1, s.length - 1).trim()
        }
        if (s.startsWith("-")) {
            negatif = true
            s = s.substring(1).trim()
        }
        s = AWALAN.replaceFirst(s, "")
        if (s.startsWith("-")) {
            negatif = true
            s = s.substring(1).trim()
        }
        s = AKHIRAN_TANPA_SEN.replace(s, "")

        if (!ANGKA_DAN_PEMISAH.matches(s) || s.none { it.isDigit() }) {
            throw AngkaTidakValid("bukan angka rupiah: '$teks'")
        }

        val adaTitik = '.' in s
        val adaKoma = ',' in s
        var pecahan = ""
        val digits: String

        if (adaTitik && adaKoma) {
            val desimal = if (s.lastIndexOf('.') > s.lastIndexOf(',')) '.' else ','
            val ribuan = if (desimal == '.') ',' else '.'
            val posisi = s.lastIndexOf(desimal)
            val kiri = s.substring(0, posisi)
            pecahan = s.substring(posisi + 1)
            if (desimal in kiri) throw AngkaTidakValid("pemisah campur aduk: '$teks'")
            digits = gabungRibuan(kiri, ribuan, teks)
        } else if (adaTitik || adaKoma) {
            val pemisah = if (adaTitik) '.' else ','
            val bagian = s.split(pemisah)
            if (bagian.size > 2) {
                digits = gabungRibuan(s, pemisah, teks)
            } else {
                val (kiri, kanan) = bagian
                if (kanan.length == 3 && kiri != "" && kiri != "0") {
                    digits = kiri + kanan
                } else if (kiri != "" && kanan.length in 1..2) {
                    digits = kiri
                    pecahan = kanan
                } else {
                    throw AngkaTidakValid("format angka ambigu: '$teks'")
                }
            }
        } else {
            digits = s
        }

        if (digits.isEmpty() || !digits.all { it.isDigit() }) {
            throw AngkaTidakValid("bukan angka rupiah: '$teks'")
        }
        if (pecahan.isNotEmpty() &&
            (!pecahan.all { it.isDigit() } || pecahan.length > 2 || pecahan.toInt() != 0)
        ) {
            throw AngkaTidakValid("bukan bilangan bulat rupiah: '$teks'")
        }
        val nilai = digits.toLongOrNull() ?: throw AngkaTidakValid("angka terlalu besar: '$teks'")
        return if (negatif) -nilai else nilai
    }

    fun parseAtauNull(teks: String): Long? = try {
        parse(teks)
    } catch (_: AngkaTidakValid) {
        null
    }

    /** 60000 -> "Rp 60.000"; -5000 -> "-Rp 5.000". Tidak bergantung Locale perangkat. */
    fun format(n: Long): String {
        val tanda = if (n < 0) "-" else ""
        return "${tanda}Rp ${kelompokRibuan(kotlin.math.abs(n))}"
    }

    /** 60000 -> "60.000" (untuk mengisi kolom masukan). */
    fun formatTanpaRp(n: Long): String {
        val tanda = if (n < 0) "-" else ""
        return tanda + kelompokRibuan(kotlin.math.abs(n))
    }

    private fun kelompokRibuan(n: Long): String = n.toString().reversed().chunked(3).joinToString(".").reversed()

    internal fun gabungRibuan(teks: String, pemisah: Char, asli: String): String {
        val kelompok = teks.split(pemisah)
        if (kelompok.size == 1) return kelompok[0]
        if (kelompok[0].length !in 1..3 || kelompok.drop(1).any { it.length != 3 }) {
            throw AngkaTidakValid("pengelompokan ribuan tidak wajar: '$asli'")
        }
        return kelompok.joinToString("")
    }
}

object Qty {
    private val POLA = Regex("[0-9]+(?:[.,][0-9]+)*")
    private val SPASI = Regex("\\s+")

    /** Cermin parse_qty: koma desimal, titik + 3 digit = ribuan, maksimal 3 angka desimal. */
    fun parse(teks: String): BigDecimal {
        val s = SPASI.replace(teks, "").trimEnd('x', 'X')
        if (s.isEmpty() || !POLA.matches(s)) throw AngkaTidakValid("bukan angka qty: '$teks'")

        val normal: String = if (',' in s) {
            if (s.count { it == ',' } > 1) throw AngkaTidakValid("format qty ambigu: '$teks'")
            val (kiri, kanan) = s.split(',')
            if (kanan.isEmpty() || !kanan.all { it.isDigit() }) {
                throw AngkaTidakValid("format qty ambigu: '$teks'")
            }
            "${gabungRibuanQty(kiri, teks)}.$kanan"
        } else if ('.' in s) {
            val bagian = s.split('.')
            if (bagian.size == 2 && bagian[1].length in 1..2) s else gabungRibuanQty(s, teks)
        } else {
            s
        }

        val nilai = normal.toBigDecimalOrNull() ?: throw AngkaTidakValid("bukan angka qty: '$teks'")
        if (nilai.signum() <= 0) throw AngkaTidakValid("qty harus lebih dari 0: '$teks'")
        if (nilai.scale() > 3) throw AngkaTidakValid("qty lebih dari 3 angka desimal: '$teks'")
        return nilai
    }

    fun parseAtauNull(teks: String): BigDecimal? = try {
        parse(teks)
    } catch (_: AngkaTidakValid) {
        null
    }

    /** 1.500 -> "1,5"; 2 -> "2". */
    fun format(qty: BigDecimal): String = qty.stripTrailingZeros().toPlainString().replace('.', ',')

    private fun gabungRibuanQty(teks: String, asli: String): String {
        if ('.' !in teks) return teks
        return try {
            Rupiah.gabungRibuan(teks, '.', asli)
        } catch (_: AngkaTidakValid) {
            throw AngkaTidakValid("format qty ambigu: '$asli'")
        }
    }
}
