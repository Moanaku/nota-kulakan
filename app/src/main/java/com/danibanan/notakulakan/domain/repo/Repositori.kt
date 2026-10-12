package com.danibanan.notakulakan.domain.repo

import com.danibanan.notakulakan.domain.PilihanProduk
import com.danibanan.notakulakan.domain.Produk
import com.danibanan.notakulakan.domain.RingkasNota
import com.danibanan.notakulakan.domain.model.Isu
import com.danibanan.notakulakan.domain.model.ItemBaris
import com.danibanan.notakulakan.domain.model.Nota
import java.io.File
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow

// ---------------------------------------------------------------------------
// Ekstraksi (backend)
// ---------------------------------------------------------------------------

data class MetaEkstraksi(
    val idPermintaan: String,
    val model: String,
    val versiPrompt: String,
    val biayaUsd: Double?,
    val latensiMs: Long,
)

/** Jenis kegagalan yang dibedakan di layar, masing-masing dengan jalan keluarnya. */
enum class JenisGagal {
    TANPA_JARINGAN,
    WAKTU_HABIS,
    LAYANAN_SIBUK,
    BUKAN_NOTA,
    FOTO_TIDAK_VALID,
    FOTO_TERLALU_BESAR,
    APLIKASI_DITOLAK,
    GANGGUAN_SERVER,
}

sealed interface HasilEkstraksi {
    data class Berhasil(
        val nota: Nota,
        val isuServer: List<Isu>,
        val jenisNota: String?,
        val catatan: String?,
        val meta: MetaEkstraksi,
    ) : HasilEkstraksi

    data class Gagal(val jenis: JenisGagal) : HasilEkstraksi
}

interface EkstraksiRepository {
    suspend fun ekstrak(foto: File): HasilEkstraksi
}

// ---------------------------------------------------------------------------
// Penyimpanan lokal
// ---------------------------------------------------------------------------

data class StokProduk(
    val produkId: Long,
    val nama: String,
    val satuan: String?,
    val totalMasuk: BigDecimal,
    val hargaTerakhir: Long?,
    val selisihHarga: Long?,
)

data class DrafSimpan(
    val nota: Nota,
    val pilihan: List<PilihanProduk>,
    val fotoSementara: File?,
    val dikonfirmasiMeskiTidakCocok: Boolean,
    val meta: MetaEkstraksi?,
)

data class PerubahanTersimpan(
    val namaProduk: String,
    val satuan: String?,
    val hargaSekarang: Long,
    val hargaSebelumnya: Long?,
)

data class HasilSimpan(val notaId: Long, val perubahan: List<PerubahanTersimpan>)

data class NotaTersimpan(
    val id: Long,
    val nota: Nota,
    val namaProduk: List<String>,
    val dibuatPada: Long,
    val fotoPath: String?,
    val dikonfirmasiMeskiTidakCocok: Boolean,
)

interface NotaRepository {
    fun stok(): Flow<List<StokProduk>>
    fun riwayat(): Flow<List<RingkasNota>>
    suspend fun detail(id: Long): NotaTersimpan?
    suspend fun hapus(id: Long)
    suspend fun daftarProduk(): List<Produk>

    /** Produk yang sudah pernah dipetakan ke nama-nama ini (berdasarkan kunci alias). */
    suspend fun saranProduk(namaDiNota: List<String>): List<Produk?>
    suspend fun cariDuplikat(total: Long?, jumlahBaris: Int, sekarang: Long): RingkasNota?
    suspend fun simpan(draf: DrafSimpan): HasilSimpan
}

/** Sumber waktu yang bisa diganti di tes. */
fun interface Jam {
    fun sekarang(): Long
}

/** Analitik produk: hanya angka dan status, tidak pernah nama barang, harga, atau foto. */
interface Analitik {
    fun ekstraksiSelesai(lolos: Boolean, jumlahBaris: Int, latensiMs: Long, model: String)
    fun ekstraksiGagal(jenis: JenisGagal)
    fun notaDisimpan(jumlahBaris: Int, barisDikoreksi: Int, dikonfirmasi: Boolean, durasiDetik: Long, manual: Boolean)
}

/** Menyiapkan foto (dari kamera atau galeri) menjadi JPEG terkompres siap unggah. */
interface PenyiapFoto {
    suspend fun siapkan(uri: String): File
}

/** Contoh baris kosong untuk isi manual. */
fun barisKosong(): ItemBaris = ItemBaris(nama = "", qty = null, satuan = null, hargaSatuan = null, jumlah = null)
