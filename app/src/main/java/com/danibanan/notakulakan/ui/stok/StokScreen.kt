package com.danibanan.notakulakan.ui.stok

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
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
import com.danibanan.notakulakan.ui.komponen.BarBawahTengah
import com.danibanan.notakulakan.ui.komponen.DataKosong
import com.danibanan.notakulakan.ui.komponen.JenisKosong
import com.danibanan.notakulakan.ui.komponen.KartuRingkasanNota
import com.danibanan.notakulakan.ui.komponen.KondisiGalat
import com.danibanan.notakulakan.ui.komponen.PemisahBaris
import com.danibanan.notakulakan.ui.komponen.SkeletonBarisBarang
import com.danibanan.notakulakan.ui.komponen.SpandukOffline
import com.danibanan.notakulakan.ui.komponen.Tab
import com.danibanan.notakulakan.ui.komponen.formatTanggal
import com.danibanan.notakulakan.ui.komponen.SKALA_FONT_SUSUN_SEMPIT
import com.danibanan.notakulakan.ui.komponen.SKALA_FONT_SUSUN_VERTIKAL
import com.danibanan.notakulakan.ui.komponen.StatusHargaBaris
import com.danibanan.notakulakan.ui.komponen.rememberTersambung
import com.danibanan.notakulakan.ui.komponen.statusHarga
import com.danibanan.notakulakan.ui.theme.Spasi
import com.danibanan.notakulakan.ui.theme.Ukuran
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
 * lokal) dan offline (spanduk: stok tetap dari HP, hanya nota baru yang belum bisa dibaca). Judul dan keterangan
 * ikut menggulir bersama daftar.
 */
@Composable
fun StokScreen(
    onCatatNota: () -> Unit,
    onKeRiwayat: () -> Unit,
    vm: StokViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    StokLayar(
        ui = ui,
        tersambung = rememberTersambung(),
        onCatatNota = onCatatNota,
        onKeRiwayat = onKeRiwayat,
        onCobaLagi = vm::cobaLagi,
    )
}

/** Isi layar Stok tanpa ViewModel, supaya setiap kondisinya bisa dipotret lewat katalog debug. */
@Composable
internal fun StokLayar(
    ui: StokUi,
    tersambung: Boolean,
    onCatatNota: () -> Unit,
    onKeRiwayat: () -> Unit,
    onCobaLagi: () -> Unit,
) {
    Scaffold(
        bottomBar = { BarBawahTengah(Tab.STOK, onStok = {}, onRiwayat = onKeRiwayat, onCatatNota = onCatatNota) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            SpandukOffline(tampil = !tersambung, teks = stringResource(R.string.offline_spanduk_data))
            // Daftar bergulir sampai di bawah bilah; padding bawah = tinggi bilah + 16 supaya baris terakhir tidak tertutup.
            LazyColumn(
                Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(
                    start = Spasi.l,
                    end = Spasi.l,
                    top = Spasi.s,
                    bottom = padding.calculateBottomPadding() + Spasi.l,
                ),
            ) {
                item {
                    Text(
                        stringResource(R.string.stok_judul),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        stringResource(R.string.stok_penjelasan),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spasi.xs, bottom = Spasi.l),
                    )
                }
                when (ui) {
                    StokUi.Memuat -> item { SkeletonBarisBarang(jumlahBaris = 5) }

                    StokUi.Galat -> item {
                        KondisiGalat(sebab = stringResource(R.string.galat_stok), onCobaLagi = onCobaLagi)
                    }

                    is StokUi.Ada -> if (ui.stok.isEmpty()) {
                        item {
                            DataKosong(
                                jenis = JenisKosong.BELUM_ADA_DATA,
                                objek = stringResource(R.string.objek_barang),
                                teksAksi = stringResource(R.string.catat_nota),
                                onAksi = onCatatNota,
                                isi = stringResource(R.string.stok_kosong_isi),
                            )
                        }
                    } else {
                        daftarStok(ui)
                    }
                }
            }
        }
    }
}

private fun LazyListScope.daftarStok(s: StokUi.Ada) {
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
    item { JudulDaftarBarang() }
    items(s.stok, key = { "${it.produkId}-${it.satuan}" }) { p ->
        BarisStok(p)
        PemisahBaris()
    }
}

/** Judul bagian + keterangan kolom di kanan ("Harga modal per satuan"); di skala font besar keterangan pindah ke bawah. */
@Composable
private fun JudulDaftarBarang() {
    val w = MaterialTheme.colorScheme
    val judul = stringResource(R.string.stok_daftar_judul)
    val kolom = stringResource(R.string.stok_kolom_harga)
    val modifier = Modifier.fillMaxWidth().padding(top = Spasi.xl, bottom = Spasi.s)
    if (LocalDensity.current.fontScale >= SKALA_FONT_SUSUN_SEMPIT) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(Spasi.xs)) {
            Text(judul, style = MaterialTheme.typography.headlineSmall, color = w.onSurface)
            Text(kolom, style = MaterialTheme.typography.bodySmall, color = w.onSurfaceVariant)
        }
    } else {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(Spasi.l), verticalAlignment = Alignment.Bottom) {
            Text(judul, style = MaterialTheme.typography.headlineSmall, color = w.onSurface)
            Text(
                kolom,
                style = MaterialTheme.typography.bodySmall,
                color = w.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Satu barang: kiri nama (isi-tebal), total masuk (keterangan), status harga; kanan harga modal (isi-tebal, rata kanan)
 * dan "per satuan" (keterangan). Di skala font besar kolom kanan turun ke bawah kolom kiri supaya nama tidak terhimpit.
 */
@Composable
private fun BarisStok(p: StokProduk) {
    val w = MaterialTheme.colorScheme
    val masuk = stringResource(R.string.stok_total_masuk, listOfNotNull(Qty.format(p.totalMasuk), p.satuan).joinToString(" "))
    val harga = statusHarga(p.selisihHarga, adaRiwayat = p.selisihHarga != null)
    val hargaModal = p.hargaTerakhir?.let(Rupiah::format) ?: "-"
    val perSatuan = p.satuan?.let { stringResource(R.string.stok_per_satuan, it) }
    val kiri: @Composable (Modifier) -> Unit = {
        Column(it, verticalArrangement = Arrangement.spacedBy(Spasi.xs)) {
            Text(p.nama, style = MaterialTheme.typography.titleMedium, color = w.onSurface)
            Text(masuk, style = MaterialTheme.typography.bodySmall, color = w.onSurfaceVariant)
            StatusHargaBaris(harga, Modifier.padding(top = Spasi.xs))
        }
    }
    val kanan: @Composable (Modifier, Alignment.Horizontal) -> Unit = { m, rata ->
        Column(m, horizontalAlignment = rata, verticalArrangement = Arrangement.spacedBy(Spasi.xs)) {
            Text(hargaModal, style = MaterialTheme.typography.titleMedium, color = w.onSurface, textAlign = TextAlign.End)
            if (perSatuan != null) {
                Text(perSatuan, style = MaterialTheme.typography.bodySmall, color = w.onSurfaceVariant, textAlign = TextAlign.End)
            }
        }
    }
    val dasar = Modifier.fillMaxWidth().defaultMinSize(minHeight = Ukuran.sentuhMin).padding(vertical = Spasi.s)
    if (LocalDensity.current.fontScale >= SKALA_FONT_SUSUN_VERTIKAL) {
        Column(dasar, verticalArrangement = Arrangement.spacedBy(Spasi.s)) {
            kiri(Modifier)
            kanan(Modifier, Alignment.Start)
        }
    } else {
        Row(dasar, horizontalArrangement = Arrangement.spacedBy(Spasi.l), verticalAlignment = Alignment.Top) {
            kiri(Modifier.weight(1f))
            kanan(Modifier.widthIn(min = Spasi.xxxl), Alignment.End)
        }
    }
}
