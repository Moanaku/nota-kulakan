package com.danibanan.notakulakan.ui.komponen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danibanan.notakulakan.ui.theme.Elevasi
import com.danibanan.notakulakan.ui.theme.Radius
import com.danibanan.notakulakan.ui.theme.Spasi

/**
 * Kerangka sheet bawah untuk memilih satu pilihan (mis. produk). Tanpa Simpan/Batal: memilih satu baris langsung
 * menutup sheet lewat pemilik state. Gayanya turunan [SheetKoreksi]; sheet selalu tampil penuh agar daftar terbaca.
 * Isi (slot) memuat teks pengantar, kolom cari, dan daftar. Daftar di dalam isi tidak boleh di-scroll sendiri
 * (tanpa verticalScroll di sini), karena bisa bertabrakan dengan LazyColumn.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetPilihan(
    judul: String,
    onTutup: () -> Unit,
    modifier: Modifier = Modifier,
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
        Column(
            Modifier.fillMaxWidth().padding(horizontal = Spasi.l).imePadding(),
            verticalArrangement = Arrangement.spacedBy(Spasi.m),
        ) {
            Text(judul, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            isi()
        }
    }
}
