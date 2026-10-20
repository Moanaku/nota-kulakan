package com.danibanan.notakulakan.ui.komponen

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.danibanan.notakulakan.R
import com.danibanan.notakulakan.domain.Rupiah
import com.danibanan.notakulakan.ui.theme.Elevasi
import com.danibanan.notakulakan.ui.theme.Radius

/** Bilah judul layar (judul-layar, Newsreader). Tombol kembali hanya bila [onKembali] diberikan. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarAtas(judul: String, modifier: Modifier = Modifier, onKembali: (() -> Unit)? = null) {
    TopAppBar(
        title = { Text(judul, style = MaterialTheme.typography.headlineMedium) },
        modifier = modifier,
        navigationIcon = {
            if (onKembali != null) {
                IconButton(onClick = onKembali) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.kembali))
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

/** Aksi utama di layar stok dan riwayat. Datar (elevasi 0), warna aksen. */
@Composable
fun TombolCatatNota(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val w = MaterialTheme.colorScheme
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(Radius.sedang),
        containerColor = w.primary,
        contentColor = w.onPrimary,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = Elevasi.nol,
            pressedElevation = Elevasi.nol,
            focusedElevation = Elevasi.nol,
            hoveredElevation = Elevasi.nol,
        ),
        icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
        text = { Text(stringResource(R.string.catat_nota), style = MaterialTheme.typography.labelLarge) },
    )
}

/**
 * Perubahan harga modal sebagai [ChipStatus]: naik = selisih (perlu diperhatikan), turun = cocok,
 * tetap atau produk baru = info.
 */
@Composable
fun ChipSelisihHarga(selisih: Long?, adaRiwayat: Boolean, modifier: Modifier = Modifier) {
    when {
        !adaRiwayat || selisih == null ->
            ChipStatus(StatusBaris.BELUM_TERBACA, modifier, stringResource(R.string.harga_baru))
        selisih > 0 -> ChipStatus(StatusBaris.SELISIH, modifier, stringResource(R.string.harga_naik, Rupiah.format(selisih)))
        selisih < 0 -> ChipStatus(StatusBaris.COCOK, modifier, stringResource(R.string.harga_turun, Rupiah.format(-selisih)))
        else -> ChipStatus(StatusBaris.BELUM_TERBACA, modifier, stringResource(R.string.harga_tetap))
    }
}
