package com.danibanan.notakulakan.ui.komponen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.stringResource
import com.danibanan.notakulakan.R
import com.danibanan.notakulakan.domain.Rupiah
import com.danibanan.notakulakan.ui.theme.Elevasi
import com.danibanan.notakulakan.ui.theme.Radius
import com.danibanan.notakulakan.ui.theme.Spasi

/**
 * Bilah judul layar (judul-layar, Newsreader). Tombol kembali hanya bila [onKembali] diberikan. Tinggi minimum,
 * bukan tetap: judul panjang atau skala font besar membungkus ke baris berikutnya, tidak terpotong.
 */
@Composable
fun BarAtas(judul: String, modifier: Modifier = Modifier, onKembali: (() -> Unit)? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .heightIn(min = Spasi.xxxl + Spasi.l)
            .padding(horizontal = if (onKembali != null) Spasi.xs else Spasi.l, vertical = Spasi.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onKembali != null) {
            IconButton(onClick = onKembali) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.kembali),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.width(Spasi.xs))
        }
        Text(
            judul,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
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
 * Padding bawah daftar di layar ber-FAB: tinggi [TombolCatatNota] (membesar bersama skala font) + margin FAB
 * dari bilah bawah + 16 dp, supaya baris terakhir bisa digulir sampai di atas tombol.
 */
@Composable
fun paddingBawahFab(): Dp {
    val tinggiTeks = with(LocalDensity.current) { MaterialTheme.typography.labelLarge.lineHeight.toDp() }
    val tinggiFab = maxOf(Spasi.xxxl + Spasi.s, tinggiTeks + Spasi.xl)
    return tinggiFab + Spasi.l + Spasi.l
}

/** Status chip untuk perubahan harga modal. */
class StatusHarga(val status: StatusBaris, val teks: String)

/**
 * Perubahan harga modal sebagai status: naik = selisih (perlu diperhatikan), turun = cocok,
 * tetap atau produk baru = info.
 */
@Composable
fun statusHarga(selisih: Long?, adaRiwayat: Boolean): StatusHarga = when {
    !adaRiwayat || selisih == null -> StatusHarga(StatusBaris.BELUM_TERBACA, stringResource(R.string.harga_baru))
    selisih > 0 -> StatusHarga(StatusBaris.SELISIH, stringResource(R.string.harga_naik, Rupiah.format(selisih)))
    selisih < 0 -> StatusHarga(StatusBaris.COCOK, stringResource(R.string.harga_turun, Rupiah.format(-selisih)))
    else -> StatusHarga(StatusBaris.BELUM_TERBACA, stringResource(R.string.harga_tetap))
}

@Composable
fun ChipSelisihHarga(selisih: Long?, adaRiwayat: Boolean, modifier: Modifier = Modifier) {
    val h = statusHarga(selisih, adaRiwayat)
    ChipStatus(h.status, modifier, h.teks)
}
