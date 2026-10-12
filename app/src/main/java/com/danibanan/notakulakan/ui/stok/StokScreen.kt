package com.danibanan.notakulakan.ui.stok

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import com.danibanan.notakulakan.ui.komponen.BarBawah
import com.danibanan.notakulakan.ui.komponen.Ikon
import com.danibanan.notakulakan.ui.komponen.LencanaSelisih
import com.danibanan.notakulakan.ui.komponen.Tab
import com.danibanan.notakulakan.ui.komponen.formatTanggal
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class StokUi(
    val memuat: Boolean = true,
    val stok: List<StokProduk> = emptyList(),
    val notaTerakhir: RingkasNota? = null,
)

@HiltViewModel
class StokViewModel @Inject constructor(repo: NotaRepository) : ViewModel() {
    val ui: StateFlow<StokUi> = combine(repo.stok(), repo.riwayat()) { stok, riwayat ->
        StokUi(memuat = false, stok = stok, notaTerakhir = riwayat.firstOrNull())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StokUi())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StokScreen(
    onCatatNota: () -> Unit,
    onKeRiwayat: () -> Unit,
    vm: StokViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.stok_judul)) }) },
        bottomBar = { BarBawah(Tab.STOK, onStok = {}, onRiwayat = onKeRiwayat) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCatatNota,
                icon = { Ikon(R.drawable.ic_kamera, null) },
                text = { Text(stringResource(R.string.catat_nota)) },
            )
        },
    ) { padding ->
        when {
            ui.memuat -> Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { CircularProgressIndicator() }

            ui.stok.isEmpty() -> StokKosong(Modifier.padding(padding))

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ui.notaTerakhir?.let { n -> item { KartuNotaTerakhir(n) } }
                item {
                    Text(
                        stringResource(R.string.stok_penjelasan),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(ui.stok, key = { "${it.produkId}-${it.satuan}" }) { KartuStok(it) }
            }
        }
    }
}

@Composable
private fun StokKosong(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Ikon(R.drawable.ic_nota, null, warna = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.stok_kosong_judul), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.stok_kosong_isi),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun KartuNotaTerakhir(n: RingkasNota) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.stok_nota_terakhir),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    listOfNotNull(n.supplier, formatTanggal(n.dibuatPada)).joinToString(" · "),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(n.total?.let(Rupiah::format) ?: "-", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun KartuStok(s: StokProduk) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(s.nama, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.stok_total_masuk, listOfNotNull(Qty.format(s.totalMasuk), s.satuan).joinToString(" ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (s.hargaTerakhir != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(Rupiah.format(s.hargaTerakhir), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                    s.satuan?.let {
                        Text(
                            stringResource(R.string.stok_per_satuan, it),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    LencanaSelisih(s.selisihHarga, adaRiwayat = s.selisihHarga != null)
                }
            }
        }
    }
}
