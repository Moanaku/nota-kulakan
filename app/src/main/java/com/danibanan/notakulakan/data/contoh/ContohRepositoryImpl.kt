package com.danibanan.notakulakan.data.contoh

import android.content.Context
import com.danibanan.notakulakan.di.IoDispatcher
import com.danibanan.notakulakan.domain.Qty
import com.danibanan.notakulakan.domain.Rupiah
import com.danibanan.notakulakan.domain.model.ItemBaris
import com.danibanan.notakulakan.domain.model.Nota
import com.danibanan.notakulakan.domain.repo.ContohRepository
import com.danibanan.notakulakan.domain.repo.HasilEkstraksi
import com.danibanan.notakulakan.domain.repo.IdContoh
import com.danibanan.notakulakan.domain.repo.MetaEkstraksi
import com.danibanan.notakulakan.domain.repo.NotaContoh
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Keluaran baca MENTAH nota contoh: semua angka berupa teks seperti yang disalin model dari nota
 * ("60.000"). Penafsirannya lewat [Rupiah] dan [Qty], jalur yang sama dengan angka dari server.
 * Berkasnya ada di folder assets/contoh (satu .json per nota) dan seluruhnya sintetis.
 */
@Serializable
data class ContohMentah(
    val label: String = "",
    @SerialName("jenis_nota") val jenisNota: String? = null,
    val nota: NotaMentah,
)

@Serializable
data class NotaMentah(
    val items: List<ItemMentah> = emptyList(),
    val subtotal: String? = null,
    val diskon: String? = null,
    val pajak: String? = null,
    @SerialName("biaya_lain") val biayaLain: String? = null,
    val total: String? = null,
    val supplier: String? = null,
    @SerialName("tanggal_tertulis") val tanggalTertulis: String? = null,
)

@Serializable
data class ItemMentah(
    val nama: String,
    val qty: String? = null,
    val satuan: String? = null,
    @SerialName("harga_satuan") val hargaSatuan: String? = null,
    val jumlah: String? = null,
    @SerialName("diskon_baris") val diskonBaris: String? = null,
)

private fun String?.rupiahAtauNull(): Long? = this?.let(Rupiah::parseAtauNull)

/** Teks mentah menjadi [HasilEkstraksi.Berhasil]. Isu hitungan sengaja tidak dibawa: validator Kotlin yang menilai. */
fun bacaContoh(json: Json, teks: String): HasilEkstraksi.Berhasil {
    val mentah = json.decodeFromString(ContohMentah.serializer(), teks)
    val nota = mentah.nota
    return HasilEkstraksi.Berhasil(
        nota = Nota(
            items = nota.items.map {
                ItemBaris(
                    nama = it.nama,
                    qty = it.qty?.let(Qty::parseAtauNull),
                    satuan = it.satuan,
                    hargaSatuan = it.hargaSatuan.rupiahAtauNull(),
                    jumlah = it.jumlah.rupiahAtauNull(),
                    diskonBaris = it.diskonBaris.rupiahAtauNull() ?: 0L,
                )
            },
            total = nota.total.rupiahAtauNull(),
            subtotal = nota.subtotal.rupiahAtauNull(),
            diskon = nota.diskon.rupiahAtauNull() ?: 0L,
            pajak = nota.pajak.rupiahAtauNull() ?: 0L,
            biayaLain = nota.biayaLain.rupiahAtauNull() ?: 0L,
            supplier = nota.supplier,
            tanggalTertulis = nota.tanggalTertulis,
        ),
        isuServer = emptyList(),
        jenisNota = mentah.jenisNota,
        catatan = null,
        meta = MetaEkstraksi(idPermintaan = "contoh", model = "contoh", versiPrompt = "-", biayaUsd = null, latensiMs = 0),
    )
}

/** Membaca nota contoh dari assets APK. Tidak menyentuh jaringan. */
class ContohRepositoryImpl @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val json: Json,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ContohRepository {

    override suspend fun muat(id: IdContoh): NotaContoh = withContext(io) {
        val teks = ctx.assets.open("contoh/${id.berkas}.json").bufferedReader().use { it.readText() }
        // Salinan di cache: jalur simpan memperlakukannya seperti foto sementara dan menyalinnya ke penyimpanan tetap.
        val foto = File(ctx.cacheDir, "foto/contoh-${UUID.randomUUID()}.jpg").apply { parentFile?.mkdirs() }
        ctx.assets.open("contoh/${id.berkas}.jpg").use { masuk -> foto.outputStream().use { masuk.copyTo(it) } }
        NotaContoh(bacaContoh(json, teks), foto)
    }
}
