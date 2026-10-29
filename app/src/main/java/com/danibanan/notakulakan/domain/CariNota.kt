package com.danibanan.notakulakan.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Pencarian nota di Riwayat. Murni: tanpa Android, jaringan, atau I/O. Aturan inti: tidak ada kriteria yang wajib
 * diisi. Kriteria kosong diabaikan; hanya yang terisi digabung dengan DAN.
 *
 * Batasan Lapisan 1: tidak ada pencocokan salah ketik; "susu" menemukan "Susu" dan "susunan", bukan "sosu".
 */

/** Filter opsional. [dari] dan [sampai] dalam milidetik epoch, keduanya inklusif; null = tanpa batas. */
data class FilterNota(
    val supplier: String? = null,
    val dari: Long? = null,
    val sampai: Long? = null,
) {
    val kosong: Boolean get() = supplier == null && dari == null && sampai == null
}

private val PEMISAH_KATA = Regex("""[\s\u00A0\u202F]+""")

/** Memecah teks cari menjadi kata huruf kecil; spasi tepi dan spasi ganda hilang. Kosong bila hanya spasi. */
fun kataCari(teks: String): List<String> =
    teks.lowercase().split(PEMISAH_KATA).filter { it.isNotEmpty() }

private fun String?.mengandung(kata: String): Boolean = this != null && lowercase().contains(kata)

/**
 * Semua kata cari harus cocok (substring, tanpa peduli huruf besar/kecil), tetapi tiap kata boleh cocok di bidang
 * yang berbeda: supplier, nama barang mana pun, atau tanggal sebagaimana tampil di daftar ([tanggalTampil]).
 */
private fun cocokSemuaKata(nota: RingkasNota, kata: List<String>, tanggalTampil: String): Boolean =
    kata.all { k ->
        nota.supplier.mengandung(k) || tanggalTampil.mengandung(k) || nota.namaBarang.any { it.mengandung(k) }
    }

private fun lolosFilter(nota: RingkasNota, filter: FilterNota): Boolean {
    if (filter.supplier != null && nota.supplier?.trim()?.equals(filter.supplier.trim(), ignoreCase = true) != true) {
        return false
    }
    if (filter.dari != null && nota.dibuatPada < filter.dari) return false
    if (filter.sampai != null && nota.dibuatPada > filter.sampai) return false
    return true
}

/**
 * Menyaring [daftar] (urutan asli dipertahankan). [teks] kosong atau hanya spasi = tanpa kata cari; [filter] kosong =
 * tanpa filter. Tidak ada hasil berarti daftar kosong, bukan galat. [tanggalTampil] memformat waktu seperti di layar.
 */
fun cariNota(
    daftar: List<RingkasNota>,
    teks: String,
    filter: FilterNota = FilterNota(),
    tanggalTampil: (Long) -> String,
): List<RingkasNota> {
    val kata = kataCari(teks)
    if (kata.isEmpty() && filter.kosong) return daftar
    return daftar.filter { lolosFilter(it, filter) && (kata.isEmpty() || cocokSemuaKata(it, kata, tanggalTampil(it.dibuatPada))) }
}

/** Nama barang yang mengandung salah satu kata cari, tanpa duplikat (huruf besar/kecil sama), urutan baris nota. */
fun barangCocok(nota: RingkasNota, teks: String): List<String> {
    val kata = kataCari(teks)
    if (kata.isEmpty()) return emptyList()
    return nota.namaBarang
        .filterNotNull()
        .filter { nama -> kata.any { nama.mengandung(it) } }
        .distinctBy { it.trim().lowercase() }
}

/** Supplier unik untuk pilihan filter, urut abjad tanpa peduli huruf besar/kecil; nota tanpa supplier dilewati. */
fun daftarSupplier(daftar: List<RingkasNota>): List<String> =
    daftar.mapNotNull { it.supplier?.trim()?.takeIf(String::isNotEmpty) }
        .distinctBy { it.lowercase() }
        .sortedBy { it.lowercase() }

/** Pilihan rentang tanggal di filter. "Semua tanggal" = tanpa batas. */
enum class RentangTanggal { SEMUA, TUJUH_HARI, TIGA_PULUH_HARI, BULAN_INI, BULAN_LALU }

/**
 * Mengisi [FilterNota.dari]/[FilterNota.sampai] dari [rentang] pada hari [sekarang] di [zona]. Hari ini dihitung
 * sebagai hari pertama "7 hari terakhir"; bulan dihitung kalender, dari pukul 00.00.
 */
fun FilterNota.denganRentang(rentang: RentangTanggal, sekarang: Long, zona: ZoneId): FilterNota {
    val hariIni = Instant.ofEpochMilli(sekarang).atZone(zona).toLocalDate()
    fun awal(hari: LocalDate) = hari.atStartOfDay(zona).toInstant().toEpochMilli()
    return when (rentang) {
        RentangTanggal.SEMUA -> copy(dari = null, sampai = null)
        RentangTanggal.TUJUH_HARI -> copy(dari = awal(hariIni.minusDays(6)), sampai = null)
        RentangTanggal.TIGA_PULUH_HARI -> copy(dari = awal(hariIni.minusDays(29)), sampai = null)
        RentangTanggal.BULAN_INI -> copy(dari = awal(hariIni.withDayOfMonth(1)), sampai = null)
        RentangTanggal.BULAN_LALU -> {
            val awalBulanIni = hariIni.withDayOfMonth(1)
            copy(dari = awal(awalBulanIni.minusMonths(1)), sampai = awal(awalBulanIni) - 1)
        }
    }
}
