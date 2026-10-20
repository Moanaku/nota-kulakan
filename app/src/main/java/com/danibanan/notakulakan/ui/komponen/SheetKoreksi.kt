package com.danibanan.notakulakan.ui.komponen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danibanan.notakulakan.ui.theme.Elevasi
import com.danibanan.notakulakan.ui.theme.Radius
import com.danibanan.notakulakan.ui.theme.Spasi

/**
 * Kerangka sheet bawah untuk koreksi satu baris. Hanya tata letak: judul, isi (slot, biasanya [KolomTeks] dan
 * [KolomRupiah]), dan dua aksi. Pemilik state menentukan apa yang dikoreksi dan kapan sheet tampil.
 * Labelnya menyebut objek: [teksSimpan] = "Simpan koreksi", bukan "Simpan".
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
        modifier = modifier,
        shape = RoundedCornerShape(topStart = Radius.besar, topEnd = Radius.besar),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = Elevasi.tinggi,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spasi.l)
                .padding(bottom = Spasi.xl),
            verticalArrangement = Arrangement.spacedBy(Spasi.l),
        ) {
            Text(judul, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            isi()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spasi.m)) {
                TombolSekunder(teksBatal, onClick = onTutup, modifier = Modifier.weight(1f))
                TombolUtama(teksSimpan, onClick = onSimpan, modifier = Modifier.weight(1f), enabled = simpanAktif)
            }
        }
    }
}
