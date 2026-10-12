package com.danibanan.notakulakan.domain.model

import java.math.BigDecimal

/**
 * Model domain, cermin dari backend/src/nota/schema.py. Uang selalu Long rupiah, qty BigDecimal.
 * Field yang belum terbaca boleh null; validator yang menandainya.
 */
data class ItemBaris(
    val nama: String,
    val qty: BigDecimal?,
    val satuan: String?,
    val hargaSatuan: Long?,
    val jumlah: Long?,
    val diskonBaris: Long = 0,
    val hargaDihitung: Boolean = false,
)

data class Nota(
    val items: List<ItemBaris>,
    val total: Long?,
    val subtotal: Long? = null,
    val diskon: Long = 0,
    val pajak: Long = 0,
    val biayaLain: Long = 0,
    val pembulatan: Long = 0,
    val supplier: String? = null,
    val tanggalTertulis: String? = null,
) {
    companion object {
        val KOSONG = Nota(items = emptyList(), total = null)
    }
}

enum class KodeIsu(val nilai: String) {
    TIDAK_ADA_ITEM("tidak_ada_item"),
    FIELD_KOSONG("field_kosong"),
    FORMAT_TIDAK_DIKENALI("format_tidak_dikenali"),
    QTY_TIDAK_VALID("qty_tidak_valid"),
    BARIS_TIDAK_COCOK("baris_tidak_cocok"),
    HARGA_DIHITUNG("harga_dihitung"),
    SUBTOTAL_TIDAK_COCOK("subtotal_tidak_cocok"),
    TOTAL_TIDAK_COCOK("total_tidak_cocok"),
    TOTAL_TIDAK_BISA_DIHITUNG("total_tidak_bisa_dihitung"),

    /** Kode baru dari server yang belum dikenal versi aplikasi ini. */
    LAINNYA("lainnya"),
    ;

    companion object {
        fun dari(nilai: String): KodeIsu = entries.firstOrNull { it.nilai == nilai } ?: LAINNYA
    }
}

enum class Tingkat(val nilai: String) {
    GALAT("galat"),
    INFO("info"),
    ;

    companion object {
        fun dari(nilai: String): Tingkat = if (nilai == INFO.nilai) INFO else GALAT
    }
}

data class Isu(
    val kode: KodeIsu,
    val pesan: String,
    val baris: Int? = null,
    val diharapkan: Long? = null,
    val ditemukan: Long? = null,
    val tingkat: Tingkat = Tingkat.GALAT,
)

data class HasilValidasi(val isu: List<Isu>) {
    val lolos: Boolean get() = isu.none { it.tingkat == Tingkat.GALAT }

    /** Indeks baris yang disorot merah. */
    val barisBermasalah: Set<Int>
        get() = isu.filter { it.tingkat == Tingkat.GALAT }.mapNotNull { it.baris }.toSet()
}
