package com.danibanan.notakulakan.data.remote

import com.danibanan.notakulakan.domain.model.Isu
import com.danibanan.notakulakan.domain.model.ItemBaris
import com.danibanan.notakulakan.domain.model.KodeIsu
import com.danibanan.notakulakan.domain.model.Nota
import com.danibanan.notakulakan.domain.model.Tingkat
import com.danibanan.notakulakan.domain.repo.HasilEkstraksi
import com.danibanan.notakulakan.domain.repo.JenisGagal
import com.danibanan.notakulakan.domain.repo.MetaEkstraksi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Bentuk JSON dari backend/src/nota/api.py. Field baru dari server diabaikan (ignoreUnknownKeys). */
@Serializable
data class EkstrakRespons(
    val nota: NotaDto,
    val isu: List<IsuDto> = emptyList(),
    val lolos: Boolean = false,
    @SerialName("jenis_nota") val jenisNota: String? = null,
    val catatan: String? = null,
    val meta: MetaDto,
)

@Serializable
data class NotaDto(
    val items: List<ItemDto> = emptyList(),
    val subtotal: Long? = null,
    val diskon: Long = 0,
    val pajak: Long = 0,
    @SerialName("biaya_lain") val biayaLain: Long = 0,
    val pembulatan: Long = 0,
    val total: Long? = null,
    val supplier: String? = null,
    @SerialName("tanggal_tertulis") val tanggalTertulis: String? = null,
)

@Serializable
data class ItemDto(
    val nama: String,
    val qty: String? = null,
    val satuan: String? = null,
    @SerialName("harga_satuan") val hargaSatuan: Long? = null,
    val jumlah: Long? = null,
    @SerialName("diskon_baris") val diskonBaris: Long = 0,
    @SerialName("harga_dihitung") val hargaDihitung: Boolean = false,
)

@Serializable
data class IsuDto(
    val kode: String,
    val tingkat: String = "galat",
    val pesan: String,
    val baris: Int? = null,
    val diharapkan: Long? = null,
    val ditemukan: Long? = null,
)

@Serializable
data class MetaDto(
    @SerialName("id_permintaan") val idPermintaan: String,
    val model: String,
    @SerialName("versi_prompt") val versiPrompt: String,
    @SerialName("biaya_usd") val biayaUsd: Double? = null,
    @SerialName("latensi_ms") val latensiMs: Long = 0,
)

@Serializable
data class GalatRespons(val galat: GalatDto)

@Serializable
data class GalatDto(val kode: String, val pesan: String = "")

fun EkstrakRespons.keDomain(): HasilEkstraksi.Berhasil = HasilEkstraksi.Berhasil(
    nota = nota.keDomain(),
    isuServer = isu.map { it.keDomain() },
    jenisNota = jenisNota,
    catatan = catatan,
    meta = MetaEkstraksi(meta.idPermintaan, meta.model, meta.versiPrompt, meta.biayaUsd, meta.latensiMs),
)

fun NotaDto.keDomain(): Nota = Nota(
    items = items.map { it.keDomain() },
    total = total,
    subtotal = subtotal,
    diskon = diskon,
    pajak = pajak,
    biayaLain = biayaLain,
    pembulatan = pembulatan,
    supplier = supplier,
    tanggalTertulis = tanggalTertulis,
)

// qty dari server berbentuk teks desimal bertitik ("1.5"). Teks rusak menjadi null dan ditandai validator.
fun ItemDto.keDomain(): ItemBaris = ItemBaris(
    nama = nama,
    qty = qty?.toBigDecimalOrNull(),
    satuan = satuan,
    hargaSatuan = hargaSatuan,
    jumlah = jumlah,
    diskonBaris = diskonBaris,
    hargaDihitung = hargaDihitung,
)

fun IsuDto.keDomain(): Isu = Isu(
    kode = KodeIsu.dari(kode),
    pesan = pesan,
    baris = baris,
    diharapkan = diharapkan,
    ditemukan = ditemukan,
    tingkat = Tingkat.dari(tingkat),
)

/** Kode galat server diutamakan; status HTTP hanya cadangan bila badan galat tidak terbaca. */
fun jenisGagalDari(status: Int, kode: String?): JenisGagal = when (kode) {
    "waktu_habis" -> JenisGagal.WAKTU_HABIS
    "layanan_sibuk" -> JenisGagal.LAYANAN_SIBUK
    "bukan_nota" -> JenisGagal.BUKAN_NOTA
    "foto_tidak_valid", "permintaan_tidak_valid" -> JenisGagal.FOTO_TIDAK_VALID
    "foto_terlalu_besar" -> JenisGagal.FOTO_TERLALU_BESAR
    "app_check_ditolak" -> JenisGagal.APLIKASI_DITOLAK
    "gagal_layanan_ai", "keluaran_ai_tidak_valid" -> JenisGagal.GANGGUAN_SERVER
    else -> when (status) {
        401, 403 -> JenisGagal.APLIKASI_DITOLAK
        408, 504 -> JenisGagal.WAKTU_HABIS
        413 -> JenisGagal.FOTO_TERLALU_BESAR
        422 -> JenisGagal.BUKAN_NOTA
        429, 503 -> JenisGagal.LAYANAN_SIBUK
        else -> JenisGagal.GANGGUAN_SERVER
    }
}
