package com.danibanan.notakulakan.ui.riwayat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.danibanan.notakulakan.R
import com.danibanan.notakulakan.domain.Qty
import com.danibanan.notakulakan.domain.RingkasNota
import com.danibanan.notakulakan.domain.Rupiah
import com.danibanan.notakulakan.domain.repo.NotaRepository
import com.danibanan.notakulakan.domain.repo.NotaTersimpan
import com.danibanan.notakulakan.ui.komponen.BarBawah
import com.danibanan.notakulakan.ui.komponen.FotoNota
import com.danibanan.notakulakan.ui.komponen.Ikon
import com.danibanan.notakulakan.ui.komponen.Lencana
import com.danibanan.notakulakan.ui.komponen.Tab
import com.danibanan.notakulakan.ui.komponen.formatTanggal
import com.danibanan.notakulakan.ui.tema.WarnaNota
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Daftar riwayat
// ---------------------------------------------------------------------------

@HiltViewModel
class RiwayatViewModel @Inject constructor(repo: NotaRepository) : ViewModel() {
    val riwayat: StateFlow<List<RingkasNota>?> = repo.riwayat()
        .map<List<RingkasNota>, List<RingkasNota>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiwayatScreen(
    onBukaNota: (Long) -> Unit,
    onKeStok: () -> Unit,
    onCatatNota: () -> Unit,
    vm: RiwayatViewModel = hiltViewModel(),
) {
    val riwayat by vm.riwayat.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.riwayat_judul)) }) },
        bottomBar = { BarBawah(Tab.RIWAYAT, onStok = onKeStok, onRiwayat = {}) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCatatNota,
                icon = { Ikon(R.drawable.ic_kamera, null) },
                text = { Text(stringResource(R.string.catat_nota)) },
            )
        },
    ) { padding ->
        val daftar = riwayat
        when {
            daftar == null -> Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { CircularProgressIndicator() }

            daftar.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.riwayat_kosong),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(daftar, key = { it.id }) { n ->
                    Card(onClick = { onBukaNota(n.id) }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(formatTanggal(n.dibuatPada), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    listOfNotNull(n.supplier, stringResource(R.string.riwayat_baris, n.jumlahBaris)).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(n.total?.let(Rupiah::format) ?: "-", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                        }
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
    data object TidakAda : DetailUi
    data class Ada(val nota: NotaTersimpan) : DetailUi
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
        viewModelScope.launch {
            _ui.value = repo.detail(id)?.let { DetailUi.Ada(it) } ?: DetailUi.TidakAda
        }
    }

    fun hapus() {
        viewModelScope.launch {
            repo.hapus(id)
            _ui.value = DetailUi.Terhapus
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailNotaScreen(onKembali: () -> Unit, vm: DetailViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var tanyaHapus by remember { mutableStateOf(false) }

    LaunchedEffect(ui) {
        if (ui == DetailUi.Terhapus || ui == DetailUi.TidakAda) onKembali()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_judul)) },
                navigationIcon = {
                    IconButton(onClick = onKembali) { Ikon(R.drawable.ic_kembali, stringResource(R.string.kembali)) }
                },
            )
        },
    ) { padding ->
        val s = ui
        if (s !is DetailUi.Ada) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }
        val t = s.nota
        val w = WarnaNota.status
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column {
                    Text(formatTanggal(t.dibuatPada), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    t.nota.supplier?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    if (t.dikonfirmasiMeskiTidakCocok) {
                        Lencana(stringResource(R.string.detail_dikonfirmasi), w.cekLatar, w.cekTeks, Modifier.padding(top = 6.dp))
                    }
                }
            }
            if (t.fotoPath != null) item { FotoNota(t.fotoPath) }
            itemsIndexed(t.nota.items) { i, item ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(t.namaProduk.getOrNull(i) ?: item.nama, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "${item.nama} · ${item.qty?.let(Qty::format) ?: "?"}${item.satuan?.let { " $it" } ?: ""} × " +
                                    (item.hargaSatuan?.let(Rupiah::format) ?: "?"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(item.jumlah?.let(Rupiah::format) ?: "?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(stringResource(R.string.total_tertulis), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Text(t.nota.total?.let(Rupiah::format) ?: "-", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.ExtraBold)
                }
            }
            item {
                OutlinedButton(
                    onClick = { tanyaHapus = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.hapus_nota), color = MaterialTheme.colorScheme.error) }
            }
        }
    }

    if (tanyaHapus) {
        AlertDialog(
            onDismissRequest = { tanyaHapus = false },
            title = { Text(stringResource(R.string.hapus_nota_judul)) },
            text = { Text(stringResource(R.string.hapus_nota_isi)) },
            confirmButton = {
                TextButton(onClick = { tanyaHapus = false; vm.hapus() }) {
                    Text(stringResource(R.string.hapus), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { tanyaHapus = false }) { Text(stringResource(R.string.batal)) } },
        )
    }
}
