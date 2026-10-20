package com.danibanan.notakulakan.ui.riwayat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.danibanan.notakulakan.R
import com.danibanan.notakulakan.domain.Qty
import com.danibanan.notakulakan.domain.RingkasNota
import com.danibanan.notakulakan.domain.Rupiah
import com.danibanan.notakulakan.domain.Validator
import com.danibanan.notakulakan.domain.model.ItemBaris
import com.danibanan.notakulakan.domain.model.Tingkat
import com.danibanan.notakulakan.domain.repo.NotaRepository
import com.danibanan.notakulakan.domain.repo.NotaTersimpan
import com.danibanan.notakulakan.ui.komponen.BarAtas
import com.danibanan.notakulakan.ui.komponen.BarBawah
import com.danibanan.notakulakan.ui.komponen.BarisBarang
import com.danibanan.notakulakan.ui.komponen.BarisRingkasanTotal
import com.danibanan.notakulakan.ui.komponen.ChipStatus
import com.danibanan.notakulakan.ui.komponen.DataKosong
import com.danibanan.notakulakan.ui.komponen.FotoNota
import com.danibanan.notakulakan.ui.komponen.JenisKosong
import com.danibanan.notakulakan.ui.komponen.JenisPesan
import com.danibanan.notakulakan.ui.komponen.KartuRingkasanNota
import com.danibanan.notakulakan.ui.komponen.KondisiGalat
import com.danibanan.notakulakan.ui.komponen.PemisahBaris
import com.danibanan.notakulakan.ui.komponen.PesanPeriksa
import com.danibanan.notakulakan.ui.komponen.SkeletonBarisBarang
import com.danibanan.notakulakan.ui.komponen.StatusBaris
import com.danibanan.notakulakan.ui.komponen.Tab
import com.danibanan.notakulakan.ui.komponen.TombolCatatNota
import com.danibanan.notakulakan.ui.komponen.TombolSekunder
import com.danibanan.notakulakan.ui.komponen.TombolTeks
import com.danibanan.notakulakan.ui.komponen.formatTanggal
import com.danibanan.notakulakan.ui.theme.Elevasi
import com.danibanan.notakulakan.ui.theme.Radius
import com.danibanan.notakulakan.ui.theme.Spasi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Daftar riwayat
// ---------------------------------------------------------------------------

sealed interface RiwayatUi {
    data object Memuat : RiwayatUi
    data object Galat : RiwayatUi
    data class Ada(val daftar: List<RingkasNota>) : RiwayatUi
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RiwayatViewModel @Inject constructor(repo: NotaRepository) : ViewModel() {
    private val percobaan = MutableStateFlow(0)

    val ui: StateFlow<RiwayatUi> = percobaan
        .flatMapLatest {
            repo.riwayat()
                .map<List<RingkasNota>, RiwayatUi> { RiwayatUi.Ada(it) }
                .catch { emit(RiwayatUi.Galat) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RiwayatUi.Memuat)

    fun cobaLagi() = percobaan.update { it + 1 }
}

/**
 * Empat kondisi: memuat (skeleton), kosong (belum ada nota; tidak ada filter, jadi tidak ada "hasil filter kosong"),
 * galat (gagal membaca basis data lokal), offline tidak berlaku (data tersimpan di HP).
 */
@Composable
fun RiwayatScreen(
    onBukaNota: (Long) -> Unit,
    onKeStok: () -> Unit,
    onCatatNota: () -> Unit,
    vm: RiwayatViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { BarAtas(stringResource(R.string.riwayat_judul)) },
        bottomBar = { BarBawah(Tab.RIWAYAT, onStok = onKeStok, onRiwayat = {}) },
        floatingActionButton = { TombolCatatNota(onCatatNota) },
    ) { padding ->
        when (val s = ui) {
            RiwayatUi.Memuat -> SkeletonBarisBarang(Modifier.padding(padding).padding(horizontal = Spasi.l), jumlahBaris = 4)

            RiwayatUi.Galat -> KondisiGalat(
                sebab = stringResource(R.string.galat_riwayat),
                onCobaLagi = vm::cobaLagi,
                modifier = Modifier.padding(padding),
            )

            is RiwayatUi.Ada -> if (s.daftar.isEmpty()) {
                DataKosong(
                    jenis = JenisKosong.BELUM_ADA_DATA,
                    objek = stringResource(R.string.objek_nota),
                    modifier = Modifier.padding(padding),
                    teksAksi = stringResource(R.string.catat_nota),
                    onAksi = onCatatNota,
                )
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(start = Spasi.l, end = Spasi.l, top = Spasi.s, bottom = Spasi.xxxl * 2),
                    verticalArrangement = Arrangement.spacedBy(Spasi.m),
                ) {
                    items(s.daftar, key = { it.id }) { n ->
                        KartuRingkasanNota(
                            toko = n.supplier ?: stringResource(R.string.supplier_kosong),
                            tanggal = formatTanggal(n.dibuatPada),
                            jumlahBarang = stringResource(R.string.riwayat_baris, n.jumlahBaris),
                            total = n.total?.let(Rupiah::format) ?: "-",
                            status = null, // RingkasNota belum menyimpan status; lihat LAPORAN.
                            onClick = { onBukaNota(n.id) },
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Detail nota
// ---------------------------------------------------------------------------

sealed interface DetailUi {
    data object Memuat : DetailUi
    data object Galat : DetailUi
    data object TidakAda : DetailUi
    data class Ada(val nota: NotaTersimpan, val galatHapus: Boolean = false) : DetailUi
    data object Terhapus : DetailUi
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: NotaRepository,
) : ViewModel() {
    private val id: Long = checkNotNull(savedStateHandle["id"])
    private val _ui = MutableStateFlow<DetailUi>(DetailUi.Memuat)
    val ui: StateFlow<DetailUi> = _ui.asStateFlow()

    init {
        muat()
    }

    fun muat() {
        _ui.value = DetailUi.Memuat
        viewModelScope.launch {
            _ui.value = try {
                repo.detail(id)?.let { DetailUi.Ada(it) } ?: DetailUi.TidakAda
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                DetailUi.Galat
            }
        }
    }

    fun hapus() {
        val sekarang = _ui.value as? DetailUi.Ada ?: return
        viewModelScope.launch {
            _ui.value = try {
                repo.hapus(id)
                DetailUi.Terhapus
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                sekarang.copy(galatHapus = true)
            }
        }
    }
}

private fun statusBaris(i: Int, item: ItemBaris): StatusBaris = when {
    Validator.periksaBaris(i, item).any { it.tingkat == Tingkat.GALAT } -> StatusBaris.SELISIH
    item.qty == null || item.hargaSatuan == null || item.jumlah == null -> StatusBaris.BELUM_TERBACA
    else -> StatusBaris.COCOK
}

@Composable
fun DetailNotaScreen(onKembali: () -> Unit, vm: DetailViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var tanyaHapus by remember { mutableStateOf(false) }

    LaunchedEffect(ui) {
        if (ui == DetailUi.Terhapus || ui == DetailUi.TidakAda) onKembali()
    }

    Scaffold(topBar = { BarAtas(stringResource(R.string.detail_judul), onKembali = onKembali) }) { padding ->
        when (val s = ui) {
            // TidakAda dan Terhapus langsung menutup layar (efek di atas); skeleton menutupi satu frame itu.
            DetailUi.Memuat, DetailUi.TidakAda, DetailUi.Terhapus ->
                SkeletonBarisBarang(Modifier.padding(padding).padding(horizontal = Spasi.l))

            DetailUi.Galat -> KondisiGalat(
                sebab = stringResource(R.string.galat_detail),
                onCobaLagi = vm::muat,
                modifier = Modifier.padding(padding),
            )

            is DetailUi.Ada -> IsiDetail(s, padding, onHapus = { tanyaHapus = true })
        }
    }

    if (tanyaHapus) {
        AlertDialog(
            onDismissRequest = { tanyaHapus = false },
            shape = RoundedCornerShape(Radius.besar),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = Elevasi.tinggi,
            title = { Text(stringResource(R.string.hapus_nota_judul), style = MaterialTheme.typography.headlineSmall) },
            text = { Text(stringResource(R.string.hapus_nota_isi), style = MaterialTheme.typography.bodyLarge) },
            confirmButton = { TombolTeks(stringResource(R.string.hapus_nota), onClick = { tanyaHapus = false; vm.hapus() }) },
            dismissButton = { TombolTeks(stringResource(R.string.batal), onClick = { tanyaHapus = false }) },
        )
    }
}

@Composable
private fun IsiDetail(s: DetailUi.Ada, padding: PaddingValues, onHapus: () -> Unit) {
    val t = s.nota
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = Spasi.l, vertical = Spasi.s),
    ) {
        // Satu elemen dominan: total nota, dengan tanggal dan supplier sebagai keterangan.
        item {
            Column(Modifier.padding(bottom = Spasi.l), verticalArrangement = Arrangement.spacedBy(Spasi.s)) {
                Text(
                    listOfNotNull(t.nota.supplier, formatTanggal(t.dibuatPada)).joinToString(" · "),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BarisRingkasanTotal(
                    label = stringResource(R.string.total_tertulis),
                    nilai = t.nota.total?.let(Rupiah::format) ?: "-",
                    dominan = true,
                )
                if (t.dikonfirmasiMeskiTidakCocok) {
                    ChipStatus(StatusBaris.SELISIH, teks = stringResource(R.string.detail_dikonfirmasi))
                } else {
                    ChipStatus(StatusBaris.COCOK, teks = stringResource(R.string.status_cocok))
                }
            }
        }
        if (t.fotoPath != null) item { FotoNota(t.fotoPath, Modifier.padding(bottom = Spasi.m)) }
        item {
            Text(
                stringResource(R.string.hasil_barang_judul),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = Spasi.s),
            )
        }
        itemsIndexed(t.nota.items) { i, item ->
            val namaProduk = t.namaProduk.getOrNull(i) ?: item.nama
            val hitungan = "${item.qty?.let(Qty::format) ?: "?"}${item.satuan?.let { " $it" } ?: ""} × " +
                (item.hargaSatuan?.let(Rupiah::format) ?: "?")
            BarisBarang(
                nama = namaProduk,
                qtyHarga = if (namaProduk == item.nama) hitungan else "${item.nama} · $hitungan",
                jumlah = item.jumlah?.let(Rupiah::format) ?: "?",
                status = statusBaris(i, item),
            )
            PemisahBaris()
        }
        item {
            Column(Modifier.fillMaxWidth().padding(top = Spasi.xl), verticalArrangement = Arrangement.spacedBy(Spasi.s)) {
                if (s.galatHapus) PesanPeriksa(stringResource(R.string.galat_hapus), JenisPesan.GALAT)
                TombolSekunder(stringResource(R.string.hapus_nota), onClick = onHapus, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
