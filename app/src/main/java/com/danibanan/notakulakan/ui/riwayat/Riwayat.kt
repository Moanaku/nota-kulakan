package com.danibanan.notakulakan.ui.riwayat

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.danibanan.notakulakan.R
import com.danibanan.notakulakan.domain.FilterNota
import com.danibanan.notakulakan.domain.Qty
import com.danibanan.notakulakan.domain.RentangTanggal
import com.danibanan.notakulakan.domain.RingkasNota
import com.danibanan.notakulakan.domain.Rupiah
import com.danibanan.notakulakan.domain.Validator
import com.danibanan.notakulakan.domain.barangCocok
import com.danibanan.notakulakan.domain.cariNota
import com.danibanan.notakulakan.domain.daftarSupplier
import com.danibanan.notakulakan.domain.denganRentang
import com.danibanan.notakulakan.domain.kataCari
import com.danibanan.notakulakan.domain.model.ItemBaris
import com.danibanan.notakulakan.domain.repo.NotaRepository
import com.danibanan.notakulakan.domain.repo.NotaTersimpan
import com.danibanan.notakulakan.domain.repo.adalahDataContoh
import com.danibanan.notakulakan.ui.komponen.BarAtas
import com.danibanan.notakulakan.ui.komponen.BarBawahTengah
import com.danibanan.notakulakan.ui.komponen.BarisBarang
import com.danibanan.notakulakan.ui.komponen.BarisNota
import com.danibanan.notakulakan.ui.komponen.BarisRingkasanTotal
import com.danibanan.notakulakan.ui.komponen.DataKosong
import com.danibanan.notakulakan.ui.komponen.FotoNota
import com.danibanan.notakulakan.ui.komponen.JenisKosong
import com.danibanan.notakulakan.ui.komponen.JenisPesan
import com.danibanan.notakulakan.ui.komponen.KolomTeks
import com.danibanan.notakulakan.ui.komponen.KondisiGalat
import com.danibanan.notakulakan.ui.komponen.PemisahBaris
import com.danibanan.notakulakan.ui.komponen.PesanPeriksa
import com.danibanan.notakulakan.ui.komponen.SheetPilihan
import com.danibanan.notakulakan.ui.komponen.SkeletonBarisBarang
import com.danibanan.notakulakan.ui.komponen.SkeletonBarisNota
import com.danibanan.notakulakan.ui.komponen.SpandukOffline
import com.danibanan.notakulakan.ui.komponen.Tab
import com.danibanan.notakulakan.ui.komponen.TombolSekunder
import com.danibanan.notakulakan.ui.komponen.TombolTeks
import com.danibanan.notakulakan.ui.komponen.TombolUtama
import com.danibanan.notakulakan.ui.komponen.formatTanggal
import com.danibanan.notakulakan.ui.komponen.rememberTersambung
import com.danibanan.notakulakan.ui.komponen.statusBaris
import com.danibanan.notakulakan.ui.theme.Elevasi
import com.danibanan.notakulakan.ui.theme.Radius
import com.danibanan.notakulakan.ui.theme.Spasi
import com.danibanan.notakulakan.ui.theme.Ukuran
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
 * Kondisi: memuat (skeleton berbentuk baris nota), belum ada nota (kolom cari disembunyikan; tidak ada yang dicari),
 * hasil cari/filter kosong (berbeda dari belum ada nota), galat (gagal membaca basis data lokal) dan offline (tidak
 * relevan untuk pencarian: semuanya dari HP; spanduk hanya menjelaskan nota baru yang belum bisa dibaca). Judul dan
 * keterangan ikut menggulir bersama daftar.
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

/** Jeda sebelum kata cari diterapkan saat mengetik (ms). Mengosongkan kolom diterapkan seketika. */
internal const val JEDA_CARI_MS = 250L

/** Barang yang cocok ditampilkan paling banyak sekian, sisanya "+N lainnya". */
internal const val BATAS_BARANG_CARI = 2

/**
 * Isi layar Riwayat tanpa ViewModel, supaya setiap kondisinya bisa dipotret lewat katalog debug. Pencarian dan
 * filter hanya menyaring daftar yang sudah ada di memori (lokal, tidak mengubah data); parameter `*Awal` hanya
 * untuk katalog.
 */
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
    kataAwal: String = "",
    supplierAwal: String? = null,
    rentangAwal: RentangTanggal = RentangTanggal.SEMUA,
    filterTerbukaAwal: Boolean = false,
) {
    var kata by rememberSaveable { mutableStateOf(kataAwal) }
    var kataTerapan by rememberSaveable { mutableStateOf(kataAwal) }
    var supplierPilih by rememberSaveable { mutableStateOf(supplierAwal) }
    var rentang by rememberSaveable { mutableStateOf(rentangAwal) }
    var filterTerbuka by rememberSaveable { mutableStateOf(filterTerbukaAwal) }

    LaunchedEffect(kata) {
        if (kata.isNotEmpty()) delay(JEDA_CARI_MS)
        kataTerapan = kata
    }
    val filter = remember(supplierPilih, rentang) {
        FilterNota(supplier = supplierPilih).denganRentang(rentang, System.currentTimeMillis(), ZoneId.systemDefault())
    }
    val semuaNota = (ui as? RiwayatUi.Ada)?.daftar.orEmpty()
    val hasil = remember(semuaNota, kataTerapan, filter) { cariNota(semuaNota, kataTerapan, filter, ::formatTanggal) }
    val mencari = kataCari(kataTerapan).isNotEmpty() || !filter.kosong

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
                    RiwayatUi.Memuat -> item { SkeletonBarisNota() }

                    RiwayatUi.Galat -> item {
                        KondisiGalat(sebab = stringResource(R.string.galat_riwayat), onCobaLagi = onCobaLagi)
                    }

                    is RiwayatUi.Ada -> if (ui.daftar.isEmpty()) {
                        // Belum ada nota sama sekali: tidak ada yang bisa dicari, jadi kolom cari tidak tampil.
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
                        item {
                            KontrolCari(
                                kata = kata,
                                onKata = { kata = it },
                                supplierPilih = supplierPilih,
                                rentang = rentang,
                                onHapusSupplier = { supplierPilih = null },
                                onHapusRentang = { rentang = RentangTanggal.SEMUA },
                                onBukaFilter = { filterTerbuka = true },
                                onHapusSemuaFilter = { supplierPilih = null; rentang = RentangTanggal.SEMUA },
                                jumlahHasil = hasil.size.takeIf { mencari && it > 0 },
                            )
                        }
                        if (hasil.isEmpty()) {
                            item {
                                val kataTampil = kataTerapan.trim()
                                val adaKata = kataCari(kataTerapan).isNotEmpty()
                                val adaFilter = !filter.kosong
                                DataKosong(
                                    jenis = JenisKosong.HASIL_FILTER_KOSONG,
                                    objek = stringResource(R.string.objek_nota_tercatat),
                                    judul = when {
                                        adaKata && adaFilter -> stringResource(R.string.cari_kosong_kata_filter, kataTampil)
                                        adaKata -> stringResource(R.string.cari_kosong_kata, kataTampil)
                                        else -> stringResource(R.string.cari_kosong_filter)
                                    },
                                    isi = stringResource(if (adaKata) R.string.cari_kosong_isi_kata else R.string.cari_kosong_isi_filter),
                                    teksAksi = stringResource(
                                        when {
                                            adaKata && adaFilter -> R.string.cari_hapus_pencarian_filter
                                            adaKata -> R.string.cari_hapus
                                            else -> R.string.kosong_hapus_filter
                                        },
                                    ),
                                    onAksi = {
                                        kata = ""
                                        kataTerapan = ""
                                        supplierPilih = null
                                        rentang = RentangTanggal.SEMUA
                                    },
                                )
                            }
                        }
                        items(hasil, key = { it.id }) { n ->
                            val cocok = barangCocok(n, kataTerapan)
                            BarisNota(
                                toko = n.supplier ?: stringResource(R.string.supplier_kosong),
                                tanggal = formatTanggal(n.dibuatPada),
                                jumlahBarang = stringResource(R.string.riwayat_baris, n.jumlahBaris),
                                total = n.total?.let(Rupiah::format) ?: "-",
                                onClick = { onBukaNota(n.id) },
                                keterangan = keteranganCocok(cocok),
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
    if (filterTerbuka) {
        LembarFilter(
            daftarSupplier = remember(semuaNota) { daftarSupplier(semuaNota) },
            supplierPilih = supplierPilih,
            onSupplier = { supplierPilih = it },
            rentang = rentang,
            onRentang = { rentang = it },
            onTutup = { filterTerbuka = false },
        )
    }
}

/** "Susu Kental, Susu UHT +1 lainnya": maksimal [BATAS_BARANG_CARI] nama, sisanya dihitung. Null bila tidak ada kata cari. */
@Composable
private fun keteranganCocok(cocok: List<String>): String? {
    if (cocok.isEmpty()) return null
    val tampil = cocok.take(BATAS_BARANG_CARI).joinToString(", ") { it.trim() }
    val sisa = cocok.size - BATAS_BARANG_CARI
    return if (sisa > 0) "$tampil ${pluralStringResource(R.plurals.cari_barang_lainnya, sisa, sisa)}" else tampil
}

/**
 * Kolom cari (teks bebas, tanpa tombol "Cari"; menyaring saat mengetik), tombol Filter, chip filter aktif yang bisa
 * dihapus satu per satu dan baris jumlah hasil. Dibangun dari komponen yang ada: [KolomTeks] dan [TombolSekunder].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KontrolCari(
    kata: String,
    onKata: (String) -> Unit,
    supplierPilih: String?,
    rentang: RentangTanggal,
    onHapusSupplier: () -> Unit,
    onHapusRentang: () -> Unit,
    onBukaFilter: () -> Unit,
    onHapusSemuaFilter: () -> Unit,
    jumlahHasil: Int?,
) {
    val fokus = LocalFocusManager.current
    val adaFilter = supplierPilih != null || rentang != RentangTanggal.SEMUA
    Column(Modifier.fillMaxWidth().padding(bottom = Spasi.s), verticalArrangement = Arrangement.spacedBy(Spasi.s)) {
        KolomTeks(
            nilai = kata,
            onNilaiBerubah = onKata,
            label = stringResource(R.string.cari_nota),
            bantuan = stringResource(R.string.cari_nota_bantuan),
            ikonAwal = Icons.Outlined.Search,
            aksiAkhir = if (kata.isNotEmpty()) {
                {
                    val hapus = stringResource(R.string.cari_hapus)
                    IconButton(onClick = { onKata("") }, modifier = Modifier.size(Ukuran.sentuhMin)) {
                        Icon(Icons.Outlined.Close, contentDescription = hapus, modifier = Modifier.size(Ukuran.ikon))
                    }
                }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { fokus.clearFocus() }),
        )
        TombolSekunder(stringResource(R.string.filter_tombol), onClick = onBukaFilter)
        if (adaFilter) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spasi.s), verticalArrangement = Arrangement.spacedBy(Spasi.s)) {
                if (supplierPilih != null) {
                    ChipFilter(stringResource(R.string.filter_chip_supplier, supplierPilih), onHapusSupplier)
                }
                if (rentang != RentangTanggal.SEMUA) ChipFilter(stringResource(rentang.label()), onHapusRentang)
            }
            TombolTeks(stringResource(R.string.filter_hapus_semua), onClick = onHapusSemuaFilter, sejajarMargin = true)
        }
        if (jumlahHasil != null) {
            Text(
                pluralStringResource(R.plurals.cari_ditemukan, jumlahHasil, jumlahHasil),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

/**
 * Chip filter aktif: teks + ikon tutup, ketuk untuk menghapus. Dibangun dari token yang sama dengan [ChipStatus]
 * (container-high, radius 4, teks 14), dengan tinggi sentuh 48 dp. Usulan promosi menjadi komponen di DESIGN.md.
 */
@Composable
private fun ChipFilter(teks: String, onHapus: () -> Unit) {
    val label = stringResource(R.string.filter_hapus_cd, teks)
    Row(
        Modifier
            .defaultMinSize(minHeight = Ukuran.sentuhMin)
            .clip(RoundedCornerShape(Radius.kecil))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onHapus)
            .padding(horizontal = Spasi.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spasi.xs),
    ) {
        Text(teks, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        Icon(
            Icons.Outlined.Close,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Ukuran.ikon),
        )
    }
}

private fun RentangTanggal.label(): Int = when (this) {
    RentangTanggal.SEMUA -> R.string.rentang_semua
    RentangTanggal.TUJUH_HARI -> R.string.rentang_7_hari
    RentangTanggal.TIGA_PULUH_HARI -> R.string.rentang_30_hari
    RentangTanggal.BULAN_INI -> R.string.rentang_bulan_ini
    RentangTanggal.BULAN_LALU -> R.string.rentang_bulan_lalu
}

/**
 * Lembar Filter di atas [SheetPilihan]: supplier (satu pilihan dari yang ada) dan rentang tanggal; semuanya boleh
 * "Semua". Berbeda dari pemilih produk, memilih satu baris tidak menutup sheet karena ada dua kriteria; "Selesai" menutupnya.
 */
@Composable
private fun LembarFilter(
    daftarSupplier: List<String>,
    supplierPilih: String?,
    onSupplier: (String?) -> Unit,
    rentang: RentangTanggal,
    onRentang: (RentangTanggal) -> Unit,
    onTutup: () -> Unit,
) {
    SheetPilihan(judul = stringResource(R.string.filter_judul), onTutup = onTutup) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = Spasi.xxxl * 8)) {
            item { JudulBagianFilter(stringResource(R.string.filter_supplier)) }
            item {
                BarisFilter(stringResource(R.string.filter_semua_supplier), terpilih = supplierPilih == null) { onSupplier(null) }
                PemisahBaris()
            }
            items(daftarSupplier, key = { it }) { nama ->
                BarisFilter(nama, terpilih = supplierPilih.equals(nama, ignoreCase = true)) { onSupplier(nama) }
                PemisahBaris()
            }
            item { JudulBagianFilter(stringResource(R.string.filter_tanggal)) }
            items(RentangTanggal.entries, key = { it.name }) { r ->
                BarisFilter(stringResource(r.label()), terpilih = rentang == r) { onRentang(r) }
                PemisahBaris()
            }
        }
        TombolUtama(
            stringResource(R.string.filter_selesai),
            onClick = onTutup,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        )
    }
}

@Composable
private fun JudulBagianFilter(teks: String) {
    Text(
        teks,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = Spasi.s, bottom = Spasi.xs).semantics { heading() },
    )
}

@Composable
private fun BarisFilter(teks: String, terpilih: Boolean, onKlik: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Ukuran.sentuhMin)
            .selectable(selected = terpilih, role = Role.RadioButton, onClick = onKlik)
            .padding(vertical = Spasi.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spasi.m),
    ) {
        Text(
            teks,
            style = if (terpilih) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            color = if (terpilih) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (terpilih) {
            Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(Ukuran.ikon))
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
                status = statusBaris(item, Validator.periksaBaris(i, item)),
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
