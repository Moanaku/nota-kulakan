package com.danibanan.notakulakan.data.lokal

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Skema lokal. Stok TIDAK disimpan sebagai angka yang diperbarui, tetapi dihitung dari baris
 * nota (SUM). Akibatnya menghapus nota otomatis membatalkan stoknya, dan tidak ada angka stok
 * yang bisa "melenceng" dari riwayat.
 */

@Entity(tableName = "produk", indices = [Index(value = ["kunci"], unique = true)])
data class ProdukEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nama: String,
    /** kunciAlias(nama): mencegah "Gula Pasir" dan "gula pasir" menjadi dua produk. */
    val kunci: String,
    val dibuatPada: Long,
)

/** Nama di nota (dinormalkan) -> produk. Diisi setiap kali nota disimpan. */
@Entity(
    tableName = "alias_produk",
    foreignKeys = [
        ForeignKey(
            entity = ProdukEntity::class,
            parentColumns = ["id"],
            childColumns = ["produkId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("produkId")],
)
data class AliasEntity(
    @PrimaryKey val kunci: String,
    val produkId: Long,
)

@Entity(tableName = "nota")
data class NotaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplier: String?,
    val tanggalTertulis: String?,
    val total: Long,
    val subtotal: Long?,
    val diskon: Long,
    val pajak: Long,
    val biayaLain: Long,
    val pembulatan: Long,
    val dibuatPada: Long,
    val fotoPath: String?,
    val dikonfirmasiMeskiTidakCocok: Boolean,
    val model: String?,
    val versiPrompt: String?,
    val biayaUsd: Double?,
)

@Entity(
    tableName = "baris_nota",
    foreignKeys = [
        ForeignKey(
            entity = NotaEntity::class,
            parentColumns = ["id"],
            childColumns = ["notaId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProdukEntity::class,
            parentColumns = ["id"],
            childColumns = ["produkId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("notaId"), Index("produkId")],
)
data class BarisNotaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val notaId: Long,
    val produkId: Long,
    val urutan: Int,
    val namaDiNota: String,
    /** qty x 1000, supaya SUM pasti (tanpa float). */
    val qtyMilli: Long,
    val satuan: String?,
    val hargaSatuan: Long,
    val jumlah: Long,
    val diskonBaris: Long,
    val hargaDihitung: Boolean,
)

data class StokRow(val produkId: Long, val nama: String, val satuan: String?, val qtyMilli: Long)

data class HargaRow(val produkId: Long, val satuan: String?, val hargaSatuan: Long, val waktu: Long, val notaId: Long)

data class RingkasRow(val id: Long, val total: Long, val jumlahBaris: Int, val dibuatPada: Long, val supplier: String?)

@Dao
interface NotaDao {
    @Insert
    suspend fun tambahNota(nota: NotaEntity): Long

    @Insert
    suspend fun tambahBaris(baris: List<BarisNotaEntity>)

    @Insert
    suspend fun tambahProduk(produk: ProdukEntity): Long

    @Upsert
    suspend fun simpanAlias(alias: List<AliasEntity>)

    @Query("SELECT * FROM produk WHERE kunci = :kunci LIMIT 1")
    suspend fun produkDenganKunci(kunci: String): ProdukEntity?

    @Query("SELECT * FROM produk ORDER BY nama COLLATE NOCASE")
    suspend fun semuaProduk(): List<ProdukEntity>

    @Query("SELECT * FROM produk WHERE id IN (:id)")
    suspend fun produkDenganId(id: List<Long>): List<ProdukEntity>

    @Query("SELECT * FROM alias_produk WHERE kunci IN (:kunci)")
    suspend fun aliasUntuk(kunci: List<String>): List<AliasEntity>

    @Query(
        """
        SELECT p.id AS produkId, p.nama AS nama, MAX(b.satuan) AS satuan, SUM(b.qtyMilli) AS qtyMilli
        FROM produk p JOIN baris_nota b ON b.produkId = p.id
        GROUP BY p.id, LOWER(IFNULL(b.satuan, ''))
        ORDER BY p.nama COLLATE NOCASE
        """,
    )
    fun stok(): Flow<List<StokRow>>

    @Query(
        """
        SELECT b.produkId AS produkId, b.satuan AS satuan, b.hargaSatuan AS hargaSatuan,
               n.dibuatPada AS waktu, b.notaId AS notaId
        FROM baris_nota b JOIN nota n ON n.id = b.notaId
        """,
    )
    fun riwayatHarga(): Flow<List<HargaRow>>

    @Query(
        """
        SELECT b.produkId AS produkId, b.satuan AS satuan, b.hargaSatuan AS hargaSatuan,
               n.dibuatPada AS waktu, b.notaId AS notaId
        FROM baris_nota b JOIN nota n ON n.id = b.notaId
        WHERE b.produkId IN (:produkId)
        """,
    )
    suspend fun riwayatHargaProduk(produkId: List<Long>): List<HargaRow>

    @Query(
        """
        SELECT n.id AS id, n.total AS total, n.dibuatPada AS dibuatPada, n.supplier AS supplier,
               (SELECT COUNT(*) FROM baris_nota b WHERE b.notaId = n.id) AS jumlahBaris
        FROM nota n ORDER BY n.dibuatPada DESC
        """,
    )
    fun riwayat(): Flow<List<RingkasRow>>

    @Query(
        """
        SELECT n.id AS id, n.total AS total, n.dibuatPada AS dibuatPada, n.supplier AS supplier,
               (SELECT COUNT(*) FROM baris_nota b WHERE b.notaId = n.id) AS jumlahBaris
        FROM nota n WHERE n.dibuatPada >= :sejak
        """,
    )
    suspend fun ringkasSejak(sejak: Long): List<RingkasRow>

    @Query("SELECT * FROM nota WHERE id = :id")
    suspend fun nota(id: Long): NotaEntity?

    @Query("SELECT * FROM baris_nota WHERE notaId = :id ORDER BY urutan")
    suspend fun barisNota(id: Long): List<BarisNotaEntity>

    @Query("DELETE FROM nota WHERE id = :id")
    suspend fun hapusNota(id: Long)
}

@Database(
    entities = [ProdukEntity::class, AliasEntity::class, NotaEntity::class, BarisNotaEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class BasisdataNota : RoomDatabase() {
    abstract fun notaDao(): NotaDao
}
