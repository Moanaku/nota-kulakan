package com.danibanan.notakulakan.ui.komponen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.danibanan.notakulakan.ui.theme.Elevasi
import com.danibanan.notakulakan.ui.theme.Radius
import com.danibanan.notakulakan.ui.theme.Spasi

/**
 * Kerangka sheet bawah untuk koreksi satu baris. Hanya tata letak: judul, isi (slot, biasanya [KolomTeks] dan
 * [KolomRupiah]), dan dua aksi. Pemilik state menentukan apa yang dikoreksi dan kapan sheet tampil.
 * Labelnya menyebut objek: [teksSimpan] = "Simpan koreksi", bukan "Simpan".
 * Judul dan isi menggulir; baris aksi disematkan di dasar sheet supaya "Simpan" selalu terjangkau (juga di atas
 * keyboard dan di skala font besar). Sheet selalu tampil sepenuh isinya (tanpa setengah tinggi).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetKoreksi(
    judul: String,
    teksSimpan: String,
    teksBatal: String,
    onSimpan: () -> Unit,
    onTutup: () -> Unit,
    modifier: Modifier = Modifier,
    simpanAktif: Boolean = true,
    isi: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onTutup,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier,
        shape = RoundedCornerShape(topStart = Radius.besar, topEnd = Radius.besar),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = Elevasi.tinggi,
    ) {
        Column(Modifier.fillMaxWidth().imePadding().navigationBarsPadding()) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spasi.l),
                verticalArrangement = Arrangement.spacedBy(Spasi.l),
            ) {
                Text(judul, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                isi()
            }
            val aksiBatal: @Composable (Modifier) -> Unit = { TombolSekunder(teksBatal, onClick = onTutup, modifier = it) }
            val aksiSimpan: @Composable (Modifier) -> Unit = {
                TombolUtama(teksSimpan, onClick = onSimpan, modifier = it, enabled = simpanAktif)
            }
            if (LocalDensity.current.fontScale >= SKALA_FONT_SUSUN_VERTIKAL) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = Spasi.l).padding(top = Spasi.m, bottom = Spasi.l),
                    verticalArrangement = Arrangement.spacedBy(Spasi.s),
                ) {
                    aksiSimpan(Modifier.fillMaxWidth())
                    aksiBatal(Modifier.fillMaxWidth())
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Spasi.l).padding(top = Spasi.m, bottom = Spasi.l),
                    horizontalArrangement = Arrangement.spacedBy(Spasi.m),
                ) {
                    aksiBatal(Modifier.weight(1f))
                    aksiSimpan(Modifier.weight(1f))
                }
            }
        }
    }
}
