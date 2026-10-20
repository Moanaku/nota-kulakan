package com.danibanan.notakulakan.ui.stok

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.danibanan.notakulakan.R
import com.danibanan.notakulakan.domain.Qty
import com.danibanan.notakulakan.domain.RingkasNota
import com.danibanan.notakulakan.domain.Rupiah
import com.danibanan.notakulakan.domain.repo.NotaRepository
import com.danibanan.notakulakan.domain.repo.StokProduk
import com.danibanan.notakulakan.ui.komponen.BarAtas
import com.danibanan.notakulakan.ui.komponen.BarBawah
import com.danibanan.notakulakan.ui.komponen.BarisBarang
import com.danibanan.notakulakan.ui.komponen.DataKosong
import com.danibanan.notakulakan.ui.komponen.JenisKosong
import com.danibanan.notakulakan.ui.komponen.KartuRingkasanNota
import com.danibanan.notakulakan.ui.komponen.KondisiGalat
import com.danibanan.notakulakan.ui.komponen.PemisahBaris
import com.danibanan.notakulakan.ui.komponen.SkeletonBarisBarang
import com.danibanan.notakulakan.ui.komponen.Tab
import com.danibanan.notakulakan.ui.komponen.TombolCatatNota
import com.danibanan.notakulakan.ui.komponen.formatTanggal
import com.danibanan.notakulakan.ui.komponen.paddingBawahFab
import com.danibanan.notakulakan.ui.komponen.statusHarga
import com.danibanan.notakulakan.ui.theme.Spasi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

sealed interface StokUi {
    data object Memuat : StokUi
    data object Galat : StokUi
    data class Ada(val stok: List<StokProduk>, val notaTerakhir: RingkasNota?) : StokUi
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StokViewModel @Inject constructor(repo: NotaRepository) : ViewModel() {
    private val percobaan = MutableStateFlow(0)

    val ui: StateFlow<StokUi> = percobaan
        .flatMapLatest {
            combine(repo.stok(), repo.riwayat()) { stok, riwayat -> StokUi.Ada(stok, riwayat.firstOrNull()) }
                .map<StokUi.Ada, StokUi> { it }
                .catch { emit(StokUi.Galat) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StokUi.Memuat)

    fun cobaLagi() = percobaan.update { it + 1 }
}

/**
 * Empat kondisi: memuat (skeleton), kosong (belum ada nota; tidak ada filter), galat (gagal membaca basis data
 * lokal), offline tidak berlaku (stok dihitung dari nota yang tersimpan di HP).
 */
@Composable
fun StokScreen(
    onCatatNota: () -> Unit,
    onKeRiwayat: () -> Unit,
    vm: StokViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { BarAtas(stringResource(R.string.stok_judul)) },
        bottomBar = { BarBawah(Tab.STOK, onStok = {}, onRiwayat = onKeRiwayat) },
        floatingActionButton = { TombolCatatNota(onCatatNota) },
    ) { padding ->
        when (val s = ui) {
            StokUi.Memuat -> SkeletonBarisBarang(Modifier.padding(padding).padding(horizontal = Spasi.l), jumlahBaris = 5)

            StokUi.Galat -> KondisiGalat(
                sebab = stringResource(R.string.galat_stok),
                onCobaLagi = vm::cobaLagi,
                modifier = Modifier.padding(padding),
            )

            is StokUi.Ada -> if (s.stok.isEmpty()) {
                DataKosong(
                    jenis = JenisKosong.BELUM_ADA_DATA,
                    objek = stringResource(R.string.objek_barang),
                    modifier = Modifier.padding(padding),
                    teksAksi = stringResource(R.string.catat_nota),
                    onAksi = onCatatNota,
                )
            } else {
                DaftarStok(s, Modifier.fillMaxSize().padding(padding))
            }
        }
    }
}

@Composable
private fun DaftarStok(s: StokUi.Ada, modifier: Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = Spasi.l, end = Spasi.l, top = Spasi.s, bottom = paddingBawahFab()),
    ) {
        item {
            Text(
                stringResource(R.string.stok_penjelasan),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spasi.l),
            )
        }
        s.notaTerakhir?.let { n ->
            item {
                Text(
                    stringResource(R.string.stok_nota_terakhir),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = Spasi.s),
                )
                KartuRingkasanNota(
                    toko = n.supplier ?: stringResource(R.string.supplier_kosong),
                    tanggal = formatTanggal(n.dibuatPada),
                    jumlahBarang = stringResource(R.string.riwayat_baris, n.jumlahBaris),
                    total = n.total?.let(Rupiah::format) ?: "-",
                    status = null, // RingkasNota belum menyimpan status; lihat LAPORAN.
                )
            }
        }
        item {
            Text(
                stringResource(R.string.stok_daftar_judul),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = Spasi.xl, bottom = Spasi.s),
            )
        }
        items(s.stok, key = { "${it.produkId}-${it.satuan}" }) { p ->
            BarisStok(p)
            PemisahBaris()
        }
    }
}

@Composable
private fun BarisStok(p: StokProduk) {
    val masuk = stringResource(R.string.stok_total_masuk, listOfNotNull(Qty.format(p.totalMasuk), p.satuan).joinToString(" "))
    val hargaModal = p.satuan?.let { stringResource(R.string.stok_harga_modal_per, it) }
    val harga = statusHarga(p.selisihHarga, adaRiwayat = p.selisihHarga != null)
    BarisBarang(
        nama = p.nama,
        qtyHarga = listOfNotNull(masuk, hargaModal).joinToString(" · "),
        jumlah = p.hargaTerakhir?.let(Rupiah::format) ?: "-",
        status = harga.status,
        teksStatus = harga.teks,
    )
}
