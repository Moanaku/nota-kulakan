package com.danibanan.notakulakan.domain

import java.math.BigDecimal

/**
 * Logika murni seputar produk, alias, harga modal, dan duplikat. Tanpa Android, mudah diuji.
 */

data class Produk(val id: Long, val nama: String)

/** Pilihan produk untuk satu baris nota sebelum disimpan. */
sealed interface PilihanProduk {
    data class Ada(val produk: Produk) : PilihanProduk
    data class Baru(val nama: String) : PilihanProduk
}

/**
 * Kunci alias: nama di nota yang dinormalkan ("Indomie Grg." -> "indomie grg").
 * Setelah pengguna memetakan satu nama nota ke produk, nota berikutnya dengan singkatan yang
 * sama langsung terpetakan. Ini pencocokan persis, bukan AI, sehingga tidak pernah salah tebak.
 * Sama dengan normalisasi_nama di backend/src/nota_eval/metrik.py.
 */
fun kunciAlias(nama: String): String =
    nama.lowercase()
        .replace(Regex("[^0-9a-z]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

/** Satu catatan harga modal dari satu baris nota tersimpan. */
data class CatatanHarga(
    val produkId: Long,
    val satuan: String?,
    val hargaSatuan: Long,
    val waktu: Long,
    val notaId: Long,
)

data class PerubahanHarga(
    val produkId: Long,
    val satuan: String?,
    val hargaSekarang: Long,
    val hargaSebelumnya: Long?,
) {
    val selisih: Long? get() = hargaSebelumnya?.let { hargaSekarang - it }
}

fun satuanSama(a: String?, b: String?): Boolean =
    a?.trim()?.lowercase().orEmpty() == b?.trim()?.lowercase().orEmpty()

/**
 * Harga modal terakhir per produk dan selisihnya terhadap nota SEBELUMNYA dengan satuan yang
 * sama. Harga per dus dan per pcs tidak dibandingkan, karena itu bukan kenaikan harga.
 */
fun hitungPerubahanHarga(riwayat: List<CatatanHarga>): Map<Long, PerubahanHarga> =
    riwayat.groupBy { it.produkId }.mapValues { (produkId, daftar) ->
        val urut = daftar.sortedWith(compareByDescending<CatatanHarga> { it.waktu }.thenByDescending { it.notaId })
        val terbaru = urut.first()
        val sebelumnya = urut.firstOrNull { it.notaId != terbaru.notaId && satuanSama(it.satuan, terbaru.satuan) }
        PerubahanHarga(produkId, terbaru.satuan, terbaru.hargaSatuan, sebelumnya?.hargaSatuan)
    }

data class RingkasNota(
    val id: Long,
    val total: Long?,
    val jumlahBaris: Int,
    val dibuatPada: Long,
    val supplier: String?,
    /** Nama barang di semua baris nota (nama di nota dan nama produk), hanya untuk pencarian Riwayat. */
    val namaBarang: List<String?> = emptyList(),
)

const val JENDELA_DUPLIKAT_HARI = 14
private const val MILIDETIK_PER_HARI = 24L * 60 * 60 * 1000

/**
 * Nota yang kemungkinan sudah pernah dicatat: total sama, jumlah baris sama, dicatat dalam
 * 14 hari terakhir. Mencegah stok terhitung dua kali karena nota yang sama difoto ulang.
 */
fun cariMiripDuplikat(total: Long?, jumlahBaris: Int, sekarang: Long, lama: List<RingkasNota>): RingkasNota? {
    if (total == null) return null
    val batas = sekarang - JENDELA_DUPLIKAT_HARI * MILIDETIK_PER_HARI
    return lama
        .filter { it.total == total && it.jumlahBaris == jumlahBaris && it.dibuatPada >= batas }
        .maxByOrNull { it.dibuatPada }
}

/** Qty disimpan sebagai bilangan bulat per seribu (1,5 -> 1500) agar SUM di SQLite tetap pasti. */
fun qtyKeMilli(qty: BigDecimal): Long = qty.movePointRight(3).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact()

fun milliKeQty(milli: Long): BigDecimal = BigDecimal.valueOf(milli, 3).stripTrailingZeros()
