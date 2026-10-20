package com.danibanan.notakulakan.ui

import com.danibanan.notakulakan.domain.Produk
import com.danibanan.notakulakan.domain.RingkasNota
import com.danibanan.notakulakan.domain.repo.DrafSimpan
import com.danibanan.notakulakan.domain.repo.HasilSimpan
import com.danibanan.notakulakan.domain.repo.NotaRepository
import com.danibanan.notakulakan.domain.repo.NotaTersimpan
import com.danibanan.notakulakan.domain.repo.StokProduk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Repositori palsu untuk tes ViewModel baca (Riwayat, Stok, Detail). Setiap fungsi bisa diganti per tes;
 * bawaannya kosong. Kegagalan disimulasikan dengan melempar di dalam Flow atau suspend, seperti basis data lokal.
 */
internal class RepoBacaPalsu : NotaRepository {
    var fakeRiwayat: () -> Flow<List<RingkasNota>> = { flowOf(emptyList()) }
    var fakeStok: () -> Flow<List<StokProduk>> = { flowOf(emptyList()) }
    var fakeDetail: suspend (Long) -> NotaTersimpan? = { null }

    override fun riwayat(): Flow<List<RingkasNota>> = fakeRiwayat()
    override fun stok(): Flow<List<StokProduk>> = fakeStok()
    override suspend fun detail(id: Long): NotaTersimpan? = fakeDetail(id)
    override suspend fun hapus(id: Long) = Unit
    override suspend fun daftarProduk(): List<Produk> = emptyList()
    override suspend fun saranProduk(namaDiNota: List<String>): List<Produk?> = namaDiNota.map { null }
    override suspend fun cariDuplikat(total: Long?, jumlahBaris: Int, sekarang: Long): RingkasNota? = null
    override suspend fun simpan(draf: DrafSimpan): HasilSimpan = error("tidak dipakai di tes baca")
}
