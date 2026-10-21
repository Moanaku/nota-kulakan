package com.danibanan.notakulakan.domain

import com.danibanan.notakulakan.domain.model.HasilValidasi
import com.danibanan.notakulakan.domain.model.Isu
import com.danibanan.notakulakan.domain.model.ItemBaris
import com.danibanan.notakulakan.domain.model.KodeIsu
import com.danibanan.notakulakan.domain.model.Nota
import com.danibanan.notakulakan.domain.model.Tingkat
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Cermin dari backend/src/nota/validator.py, supaya sorotan di layar koreksi berubah seketika
 * tanpa memanggil server. Versi Python adalah acuan; keduanya dijaga shared/test-vectors.json.
 *
 * Batas: lolos berarti angka konsisten satu sama lain, BUKAN berarti benar.
 */
object Validator {

    fun bulatkan(nilai: BigDecimal): Long = nilai.setScale(0, RoundingMode.HALF_UP).longValueExact()

    fun periksaBaris(i: Int, item: ItemBaris): List<Isu> {
        val isu = mutableListOf<Isu>()
        val kosong = buildList {
            if (item.qty == null) add("qty")
            if (item.hargaSatuan == null) add("harga_satuan")
            if (item.jumlah == null) add("jumlah")
        }
        if (kosong.isNotEmpty()) {
            isu += Isu(
                KodeIsu.FIELD_KOSONG,
                "Baris ${i + 1} (${item.nama}): belum terisi: ${kosong.joinToString(", ")}",
                baris = i,
            )
        }
        val qtyBuruk = item.qty != null && item.qty.signum() <= 0
        if (qtyBuruk) {
            isu += Isu(KodeIsu.QTY_TIDAK_VALID, "Baris ${i + 1} (${item.nama}): qty harus lebih dari 0", baris = i)
        }
        if (kosong.isNotEmpty() || qtyBuruk) return isu
        val qty = item.qty!!
        val harga = item.hargaSatuan!!
        val jumlah = item.jumlah!!

        if (item.hargaDihitung) {
            isu += Isu(
                KodeIsu.HARGA_DIHITUNG,
                "Baris ${i + 1} (${item.nama}): harga satuan tidak tertulis, dihitung dari jumlah dibagi qty",
                baris = i,
                tingkat = Tingkat.INFO,
            )
            return isu
        }

        val diharapkan = bulatkan(qty.multiply(BigDecimal.valueOf(harga))) - item.diskonBaris
        if (diharapkan != jumlah) {
            isu += Isu(
                KodeIsu.BARIS_TIDAK_COCOK,
                "Baris ${i + 1} (${item.nama}): ${Qty.format(qty)} × ${Rupiah.format(harga)}" +
                    " = ${Rupiah.format(diharapkan)}, tapi tertulis ${Rupiah.format(jumlah)}",
                baris = i,
                diharapkan = diharapkan,
                ditemukan = jumlah,
            )
        }
        return isu
    }

    fun validasi(nota: Nota): HasilValidasi {
        val isu = mutableListOf<Isu>()

        if (nota.items.isEmpty()) {
            isu += Isu(KodeIsu.TIDAK_ADA_ITEM, "Tidak ada baris barang yang terbaca")
        }
        nota.items.forEachIndexed { i, item -> isu += periksaBaris(i, item) }

        if (nota.total == null) {
            isu += Isu(KodeIsu.FIELD_KOSONG, "Total nota belum terisi")
        }
        if (nota.items.isEmpty()) return HasilValidasi(isu)

        if (nota.items.any { it.jumlah == null }) {
            isu += Isu(
                KodeIsu.TOTAL_TIDAK_BISA_DIHITUNG,
                "Ada baris tanpa jumlah, jadi subtotal dan total belum bisa diperiksa",
            )
            return HasilValidasi(isu)
        }

        val jumlahBaris = nota.items.sumOf { it.jumlah!! }

        if (nota.subtotal != null && nota.subtotal != jumlahBaris) {
            isu += Isu(
                KodeIsu.SUBTOTAL_TIDAK_COCOK,
                "Jumlah semua baris ${Rupiah.format(jumlahBaris)}, tapi subtotal tertulis ${Rupiah.format(nota.subtotal)}",
                diharapkan = jumlahBaris,
                ditemukan = nota.subtotal,
            )
        }

        if (nota.total != null) {
            val totalHitung = jumlahBaris - nota.diskon + nota.pajak + nota.biayaLain + nota.pembulatan
            if (totalHitung != nota.total) {
                isu += Isu(
                    KodeIsu.TOTAL_TIDAK_COCOK,
                    "Hasil hitung ${Rupiah.format(totalHitung)}, tapi total tertulis ${Rupiah.format(nota.total)}",
                    diharapkan = totalHitung,
                    ditemukan = nota.total,
                )
            }
        }
        return HasilValidasi(isu)
    }
}

/** Apakah nota boleh disimpan, dan apakah perlu konfirmasi pengguna. */
enum class StatusSimpan { BISA, PERLU_KONFIRMASI, TERHALANG }

/**
 * Data yang belum lengkap (kosong, qty tidak sah, format tak terbaca) TERHALANG: tidak ada
 * yang bisa disimpan ke stok tanpa angka. Ketidakcocokan hitungan boleh disimpan setelah
 * pengguna mengonfirmasi angkanya sesuai nota fisik, karena nota asli kadang memang salah hitung.
 */
fun statusSimpan(validasi: HasilValidasi): StatusSimpan {
    val galat = validasi.isu.filter { it.tingkat == Tingkat.GALAT }
    val penghalang = setOf(
        KodeIsu.TIDAK_ADA_ITEM,
        KodeIsu.FIELD_KOSONG,
        KodeIsu.QTY_TIDAK_VALID,
        KodeIsu.FORMAT_TIDAK_DIKENALI,
        KodeIsu.TOTAL_TIDAK_BISA_DIHITUNG,
        KodeIsu.LAINNYA,
    )
    return when {
        galat.any { it.kode in penghalang } -> StatusSimpan.TERHALANG
        galat.isNotEmpty() -> StatusSimpan.PERLU_KONFIRMASI
        else -> StatusSimpan.BISA
    }
}
