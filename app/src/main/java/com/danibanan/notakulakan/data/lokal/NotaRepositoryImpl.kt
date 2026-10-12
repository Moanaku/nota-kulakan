package com.danibanan.notakulakan.data.lokal

import android.content.Context
import androidx.room.withTransaction
import com.danibanan.notakulakan.di.IoDispatcher
import com.danibanan.notakulakan.domain.CatatanHarga
import com.danibanan.notakulakan.domain.JENDELA_DUPLIKAT_HARI
import com.danibanan.notakulakan.domain.PilihanProduk
import com.danibanan.notakulakan.domain.Produk
import com.danibanan.notakulakan.domain.RingkasNota
import com.danibanan.notakulakan.domain.cariMiripDuplikat
import com.danibanan.notakulakan.domain.hitungPerubahanHarga
import com.danibanan.notakulakan.domain.kunciAlias
import com.danibanan.notakulakan.domain.milliKeQty
import com.danibanan.notakulakan.domain.model.ItemBaris
import com.danibanan.notakulakan.domain.model.Nota
import com.danibanan.notakulakan.domain.qtyKeMilli
import com.danibanan.notakulakan.domain.repo.DrafSimpan
import com.danibanan.notakulakan.domain.repo.HasilSimpan
import com.danibanan.notakulakan.domain.repo.Jam
import com.danibanan.notakulakan.domain.repo.NotaRepository
import com.danibanan.notakulakan.domain.repo.NotaTersimpan
import com.danibanan.notakulakan.domain.repo.PerubahanTersimpan
import com.danibanan.notakulakan.domain.repo.StokProduk
import com.danibanan.notakulakan.domain.satuanSama
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class NotaRepositoryImpl @Inject constructor(
    private val db: BasisdataNota,
    private val dao: NotaDao,
    @ApplicationContext private val ctx: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
    private val jam: Jam,
) : NotaRepository {

    private val folderFoto: File get() = File(ctx.filesDir, "nota").apply { mkdirs() }

    override fun stok(): Flow<List<StokProduk>> = combine(dao.stok(), dao.riwayatHarga()) { stok, harga ->
        val perubahan = hitungPerubahanHarga(harga.map { it.keCatatan() })
        stok.map { s ->
            val p = perubahan[s.produkId]?.takeIf { satuanSama(it.satuan, s.satuan) }
            StokProduk(
                produkId = s.produkId,
                nama = s.nama,
                satuan = s.satuan,
                totalMasuk = milliKeQty(s.qtyMilli),
                hargaTerakhir = p?.hargaSekarang,
                selisihHarga = p?.selisih,
            )
        }
    }

    override fun riwayat(): Flow<List<RingkasNota>> = dao.riwayat().map { daftar -> daftar.map { it.keDomain() } }

    override suspend fun detail(id: Long): NotaTersimpan? = withContext(io) {
        val nota = dao.nota(id) ?: return@withContext null
        val baris = dao.barisNota(id)
        val produk = dao.produkDenganId(baris.map { it.produkId }.distinct()).associateBy { it.id }
        NotaTersimpan(
            id = nota.id,
            nota = Nota(
                items = baris.map {
                    ItemBaris(
                        nama = it.namaDiNota,
                        qty = milliKeQty(it.qtyMilli),
                        satuan = it.satuan,
                        hargaSatuan = it.hargaSatuan,
                        jumlah = it.jumlah,
                        diskonBaris = it.diskonBaris,
                        hargaDihitung = it.hargaDihitung,
                    )
                },
                total = nota.total,
                subtotal = nota.subtotal,
                diskon = nota.diskon,
                pajak = nota.pajak,
                biayaLain = nota.biayaLain,
                pembulatan = nota.pembulatan,
                supplier = nota.supplier,
                tanggalTertulis = nota.tanggalTertulis,
            ),
            namaProduk = baris.map { produk[it.produkId]?.nama ?: it.namaDiNota },
            dibuatPada = nota.dibuatPada,
            fotoPath = nota.fotoPath,
            dikonfirmasiMeskiTidakCocok = nota.dikonfirmasiMeskiTidakCocok,
        )
    }

    override suspend fun hapus(id: Long) = withContext(io) {
        val foto = dao.nota(id)?.fotoPath
        dao.hapusNota(id) // baris ikut terhapus (CASCADE), stok otomatis berkurang
        foto?.let { File(it).delete() }
        Unit
    }

    override suspend fun daftarProduk(): List<Produk> = withContext(io) {
        dao.semuaProduk().map { Produk(it.id, it.nama) }
    }

    override suspend fun saranProduk(namaDiNota: List<String>): List<Produk?> = withContext(io) {
        val kunci = namaDiNota.map(::kunciAlias)
        val alias = dao.aliasUntuk(kunci.filter { it.isNotEmpty() }.distinct()).associateBy { it.kunci }
        val produk = dao.produkDenganId(alias.values.map { it.produkId }.distinct()).associateBy { it.id }
        kunci.map { k -> alias[k]?.let { produk[it.produkId] }?.let { Produk(it.id, it.nama) } }
    }

    override suspend fun cariDuplikat(total: Long?, jumlahBaris: Int, sekarang: Long): RingkasNota? =
        withContext(io) {
            val sejak = sekarang - JENDELA_DUPLIKAT_HARI * 24L * 60 * 60 * 1000
            cariMiripDuplikat(total, jumlahBaris, sekarang, dao.ringkasSejak(sejak).map { it.keDomain() })
        }

    override suspend fun simpan(draf: DrafSimpan): HasilSimpan = withContext(io) {
        val nota = draf.nota
        require(nota.total != null) { "total wajib diisi sebelum disimpan" }
        require(nota.items.size == draf.pilihan.size)
        require(nota.items.all { it.qty != null && it.hargaSatuan != null && it.jumlah != null && it.nama.isNotBlank() }) {
            "baris belum lengkap"
        }

        // Foto disalin dulu; bila transaksi gagal, salinannya dihapus agar tidak ada file yatim.
        val fotoTetap = draf.fotoSementara?.takeIf { it.exists() }?.let { sementara ->
            File(folderFoto, "${UUID.randomUUID()}.jpg").also { sementara.copyTo(it) }
        }
        val sekarang = jam.sekarang()
        try {
            val (notaId, produkIds) = db.withTransaction {
                val produkIds = draf.pilihan.map { pilihan ->
                    when (pilihan) {
                        is PilihanProduk.Ada -> pilihan.produk.id
                        is PilihanProduk.Baru -> cariAtauBuatProduk(pilihan.nama, sekarang)
                    }
                }
                dao.simpanAlias(
                    nota.items.zip(produkIds)
                        .map { (item, produkId) -> AliasEntity(kunciAlias(item.nama), produkId) }
                        .filter { it.kunci.isNotEmpty() },
                )
                val notaId = dao.tambahNota(
                    NotaEntity(
                        supplier = nota.supplier?.trim()?.ifEmpty { null },
                        tanggalTertulis = nota.tanggalTertulis,
                        total = nota.total,
                        subtotal = nota.subtotal,
                        diskon = nota.diskon,
                        pajak = nota.pajak,
                        biayaLain = nota.biayaLain,
                        pembulatan = nota.pembulatan,
                        dibuatPada = sekarang,
                        fotoPath = fotoTetap?.absolutePath,
                        dikonfirmasiMeskiTidakCocok = draf.dikonfirmasiMeskiTidakCocok,
                        model = draf.meta?.model,
                        versiPrompt = draf.meta?.versiPrompt,
                        biayaUsd = draf.meta?.biayaUsd,
                    ),
                )
                dao.tambahBaris(
                    nota.items.mapIndexed { i, item ->
                        BarisNotaEntity(
                            notaId = notaId,
                            produkId = produkIds[i],
                            urutan = i,
                            namaDiNota = item.nama.trim(),
                            qtyMilli = qtyKeMilli(item.qty!!),
                            satuan = item.satuan?.trim()?.ifEmpty { null },
                            hargaSatuan = item.hargaSatuan!!,
                            jumlah = item.jumlah!!,
                            diskonBaris = item.diskonBaris,
                            hargaDihitung = item.hargaDihitung,
                        )
                    },
                )
                notaId to produkIds
            }
            draf.fotoSementara?.delete()
            HasilSimpan(notaId, perubahanUntuk(produkIds))
        } catch (e: Exception) {
            fotoTetap?.delete()
            throw e
        }
    }

    private suspend fun cariAtauBuatProduk(nama: String, sekarang: Long): Long {
        val bersih = nama.trim()
        val kunci = kunciAlias(bersih)
        dao.produkDenganKunci(kunci)?.let { return it.id }
        return dao.tambahProduk(ProdukEntity(nama = bersih, kunci = kunci, dibuatPada = sekarang))
    }

    private suspend fun perubahanUntuk(produkIds: List<Long>): List<PerubahanTersimpan> {
        val unik = produkIds.distinct()
        val nama = dao.produkDenganId(unik).associate { it.id to it.nama }
        val perubahan = hitungPerubahanHarga(dao.riwayatHargaProduk(unik).map { it.keCatatan() })
        return unik.mapNotNull { id ->
            val p = perubahan[id] ?: return@mapNotNull null
            PerubahanTersimpan(nama[id] ?: "?", p.satuan, p.hargaSekarang, p.hargaSebelumnya)
        }
    }

    private fun HargaRow.keCatatan() = CatatanHarga(produkId, satuan, hargaSatuan, waktu, notaId)

    private fun RingkasRow.keDomain() = RingkasNota(id, total, jumlahBaris, dibuatPada, supplier)
}
