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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import com.danibanan.notakulakan.domain.repo.adalahDataContoh
import com.danibanan.notakulakan.domain.repo.NotaTersimpan
import com.danibanan.notakulakan.ui.komponen.BarAtas
import com.danibanan.notakulakan.ui.komponen.BarBawahTengah
import com.danibanan.notakulakan.ui.komponen.BarisBarang
import com.danibanan.notakulakan.ui.komponen.BarisNota
import com.danibanan.notakulakan.ui.komponen.BarisRingkasanTotal
import com.danibanan.notakulakan.ui.komponen.DataKosong
import com.danibanan.notakulakan.ui.komponen.FotoNota
import com.danibanan.notakulakan.ui.komponen.JenisKosong
import com.danibanan.notakulakan.ui.komponen.JenisPesan
import com.danibanan.notakulakan.ui.komponen.KondisiGalat
import com.danibanan.notakulakan.ui.komponen.PemisahBaris
import com.danibanan.notakulakan.ui.komponen.PesanPeriksa
import com.danibanan.notakulakan.ui.komponen.SkeletonBarisBarang
import com.danibanan.notakulakan.ui.komponen.SpandukOffline
import com.danibanan.notakulakan.ui.komponen.StatusBaris
import com.danibanan.notakulakan.ui.komponen.Tab
import com.danibanan.notakulakan.ui.komponen.TombolTeks
import com.danibanan.notakulakan.ui.komponen.formatTanggal
import com.danibanan.notakulakan.ui.komponen.rememberTersambung
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
class RiwayatViewModel @Inject constructor(private val repo: NotaRepository) : ViewModel() {
    private val percobaan = MutableStateFlow(0)
    private val _galatHapusContoh = MutableStateFlow(false)
    val galatHapusContoh: StateFlow<Boolean> = _galatHapusContoh.asStateFlow()

    val ui: StateFlow<RiwayatUi> = percobaan
        .flatMapLatest {
            repo.riwayat()
                .map<List<RingkasNota>, RiwayatUi> { RiwayatUi.Ada(it) }
                .catch { emit(RiwayatUi.Galat) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RiwayatUi.Memuat)

    fun cobaLagi() = percobaan.update { it + 1 }

    /** Hapus nota contoh (supplier berawalan "(Contoh) "); daftar dan stok menyegarkan diri dari basis data. */
    fun hapusDataContoh() {
        viewModelScope.launch {
            _galatHapusContoh.value = try {
                repo.hapusDataContoh()
                false
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                true
            }
        }
    }
}

/**
 * Empat kondisi: memuat (skeleton), kosong (belum ada nota; tidak ada filter, jadi tidak ada "hasil filter kosong"),
 * galat (gagal membaca basis data lokal) dan offline (spanduk: data tetap dari HP, hanya nota baru yang belum bisa
 * dibaca). Judul dan keterangan ikut menggulir bersama daftar.
 */
@Composable
fun RiwayatScreen(
    onBukaNota: (Long) -> Unit,
    onKeStok: () -> Unit,
    onCatatNota: () -> Unit,
    vm: RiwayatViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val galatHapusContoh by vm.galatHapusContoh.collectAsStateWithLifecycle()
    var tanyaHapusContoh by remember { mutableStateOf(false) }
    RiwayatLayar(
        ui = ui,
        galatHapusContoh = galatHapusContoh,
        tersambung = rememberTersambung(),
        onBukaNota = onBukaNota,
        onKeStok = onKeStok,
        onCatatNota = onCatatNota,
        onCobaLagi = vm::cobaLagi,
        onHapusContoh = { tanyaHapusContoh = true },
    )
    if (tanyaHapusContoh) {
        DialogHapusContoh(
            onBatal = { tanyaHapusContoh = false },
            onHapus = { tanyaHapusContoh = false; vm.hapusDataContoh() },
        )
    }
}

/** Isi layar Riwayat tanpa ViewModel, supaya setiap kondisinya bisa dipotret lewat katalog debug. */
@Composable
internal fun RiwayatLayar(
    ui: RiwayatUi,
    galatHapusContoh: Boolean,
    tersambung: Boolean,
    onBukaNota: (Long) -> Unit,
    onKeStok: () -> Unit,
    onCatatNota: () -> Unit,
    onCobaLagi: () -> Unit,
    onHapusContoh: () -> Unit,
) {
    Scaffold(
        bottomBar = { BarBawahTengah(Tab.RIWAYAT, onStok = onKeStok, onRiwayat = {}, onCatatNota = onCatatNota) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            SpandukOffline(tampil = !tersambung, teks = stringResource(R.string.offline_spanduk_data))
            // Daftar bergulir sampai di bawah bilah; padding bawah = tinggi bilah + 16 supaya baris terakhir bisa dibaca.
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
                        stringResource(R.string.riwayat_judul),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        stringResource(R.string.riwayat_penjelasan),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spasi.xs, bottom = Spasi.l),
                    )
                }
                when (ui) {
                    RiwayatUi.Memuat -> item { SkeletonBarisBarang(jumlahBaris = 4) }

                    RiwayatUi.Galat -> item {
                        KondisiGalat(sebab = stringResource(R.string.galat_riwayat), onCobaLagi = onCobaLagi)
                    }

                    is RiwayatUi.Ada -> if (ui.daftar.isEmpty()) {
                        item {
                            DataKosong(
                                jenis = JenisKosong.BELUM_ADA_DATA,
                                objek = stringResource(R.string.objek_nota_tercatat),
                                teksAksi = stringResource(R.string.catat_nota),
                                onAksi = onCatatNota,
                                isi = stringResource(R.string.riwayat_kosong_isi),
                            )
                        }
                    } else {
                        items(ui.daftar, key = { it.id }) { n ->
                            BarisNota(
                                toko = n.supplier ?: stringResource(R.string.supplier_kosong),
                                tanggal = formatTanggal(n.dibuatPada),
                                jumlahBarang = stringResource(R.string.riwayat_baris, n.jumlahBaris),
                                total = n.total?.let(Rupiah::format) ?: "-",
                                onClick = { onBukaNota(n.id) },
                            )
                            PemisahBaris()
                        }
                        if (ui.daftar.any { adalahDataContoh(it.supplier) }) {
                            item {
                                Column(Modifier.fillMaxWidth().padding(top = Spasi.l), verticalArrangement = Arrangement.spacedBy(Spasi.s)) {
                                    if (galatHapusContoh) PesanPeriksa(stringResource(R.string.galat_hapus_contoh), JenisPesan.GALAT)
                                    TombolTeks(
                                        stringResource(R.string.hapus_contoh),
                                        onClick = onHapusContoh,
                                        sejajarMargin = true,
                                        galat = true,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Dialog elevasi 2, radius 16; konsekuensi ditulis di isi, aksi menyebut objeknya. */
@Composable
internal fun DialogHapusContoh(onBatal: () -> Unit, onHapus: () -> Unit) {
    AlertDialog(
        onDismissRequest = onBatal,
        shape = RoundedCornerShape(Radius.besar),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = Elevasi.tinggi,
        title = { Text(stringResource(R.string.hapus_contoh_judul), style = MaterialTheme.typography.headlineSmall) },
        text = { Text(stringResource(R.string.hapus_contoh_isi), style = MaterialTheme.typography.bodyLarge) },
        confirmButton = { TombolTeks(stringResource(R.string.hapus_contoh), onClick = onHapus, galat = true) },
        dismissButton = { TombolTeks(stringResource(R.string.batal), onClick = onBatal) },
    )
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

/**
 * Empat kondisi: memuat (skeleton), galat (nota tidak bisa dibuka, "Coba lagi"), kosong (nota sudah tidak ada: layar
 * menutup sendiri) dan offline (tidak berlaku: nota dibaca dari HP). "Hapus nota" ada di bawah daftar sebagai tombol
 * teks berwarna galat, bukan aksi utama.
 */
@Composable
fun DetailNotaScreen(onKembali: () -> Unit, vm: DetailViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var tanyaHapus by remember { mutableStateOf(false) }

    LaunchedEffect(ui) {
        if (ui == DetailUi.Terhapus || ui == DetailUi.TidakAda) onKembali()
    }

    DetailLayar(ui = ui, onKembali = onKembali, onCobaLagi = vm::muat, onHapus = { tanyaHapus = true })

    if (tanyaHapus) {
        DialogHapusNota(onBatal = { tanyaHapus = false }, onHapus = { tanyaHapus = false; vm.hapus() })
    }
}

/** Isi layar Detail tanpa ViewModel, supaya setiap kondisinya bisa dipotret lewat katalog debug. */
@Composable
internal fun DetailLayar(ui: DetailUi, onKembali: () -> Unit, onCobaLagi: () -> Unit, onHapus: () -> Unit) {
    Scaffold(topBar = { BarAtas(stringResource(R.string.detail_judul), onKembali = onKembali) }) { padding ->
        when (ui) {
            // TidakAda dan Terhapus langsung menutup layar (efek di DetailNotaScreen); skeleton menutupi satu frame itu.
            DetailUi.Memuat, DetailUi.TidakAda, DetailUi.Terhapus ->
                SkeletonBarisBarang(Modifier.padding(padding).padding(horizontal = Spasi.l))

            DetailUi.Galat -> KondisiGalat(
                sebab = stringResource(R.string.galat_detail),
                onCobaLagi = onCobaLagi,
                modifier = Modifier.padding(padding),
            )

            is DetailUi.Ada -> IsiDetail(ui, padding, onHapus = onHapus)
        }
    }
}

/** Dialog elevasi 2, radius 16; konsekuensi lama dipertahankan, aksi menyebut objeknya. */
@Composable
internal fun DialogHapusNota(onBatal: () -> Unit, onHapus: () -> Unit) {
    AlertDialog(
        onDismissRequest = onBatal,
        shape = RoundedCornerShape(Radius.besar),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = Elevasi.tinggi,
        title = { Text(stringResource(R.string.hapus_nota_judul), style = MaterialTheme.typography.headlineSmall) },
        text = { Text(stringResource(R.string.hapus_nota_isi), style = MaterialTheme.typography.bodyLarge) },
        confirmButton = { TombolTeks(stringResource(R.string.hapus_nota), onClick = onHapus, galat = true) },
        dismissButton = { TombolTeks(stringResource(R.string.batal), onClick = onBatal) },
    )
}

@Composable
private fun IsiDetail(s: DetailUi.Ada, padding: PaddingValues, onHapus: () -> Unit) {
    val t = s.nota
    var tampilFoto by remember { mutableStateOf(false) }
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = Spasi.l, end = Spasi.l, top = Spasi.s, bottom = Spasi.l),
    ) {
        // Satu elemen dominan: total nota; supplier dan tanggal sebagai keterangan, status (PesanPeriksa) di bawahnya.
        item {
            Column(Modifier.padding(bottom = Spasi.s), verticalArrangement = Arrangement.spacedBy(Spasi.s)) {
                BarisRingkasanTotal(
                    label = stringResource(R.string.total_tertulis),
                    nilai = t.nota.total?.let(Rupiah::format) ?: "-",
                    dominan = true,
                )
                // Supplier satu baris sendiri (boleh membungkus); tanggal dan jam di baris keterangan, tidak terpotong di tengah.
                t.nota.supplier?.let {
                    Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    formatTanggal(t.dibuatPada),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (t.dikonfirmasiMeskiTidakCocok) {
                    PesanPeriksa(stringResource(R.string.detail_dikonfirmasi), JenisPesan.GALAT)
                } else {
                    PesanPeriksa(stringResource(R.string.status_cocok), JenisPesan.COCOK)
                }
            }
        }
        if (t.fotoPath != null) {
            item {
                Column {
                    TombolTeks(
                        teks = stringResource(if (tampilFoto) R.string.sembunyikan_foto else R.string.lihat_foto),
                        onClick = { tampilFoto = !tampilFoto },
                        sejajarMargin = true,
                    )
                    if (tampilFoto) FotoNota(t.fotoPath, Modifier.padding(bottom = Spasi.s))
                }
            }
        }
        item {
            Text(
                stringResource(R.string.hasil_barang_judul),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = Spasi.m, bottom = Spasi.s),
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
                TombolTeks(stringResource(R.string.hapus_nota), onClick = onHapus, sejajarMargin = true, galat = true)
            }
        }
    }
}
