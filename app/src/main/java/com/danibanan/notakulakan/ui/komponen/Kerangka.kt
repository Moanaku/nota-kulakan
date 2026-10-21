package com.danibanan.notakulakan.ui.komponen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.danibanan.notakulakan.R
import com.danibanan.notakulakan.domain.Rupiah
import com.danibanan.notakulakan.ui.theme.GayaNota
import com.danibanan.notakulakan.ui.theme.Radius
import com.danibanan.notakulakan.ui.theme.Spasi
import com.danibanan.notakulakan.ui.theme.Ukuran

/**
 * Bilah judul layar (judul-layar, Source Serif 4). Tombol kembali hanya bila [onKembali] diberikan. Tinggi minimum,
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

/** Perubahan harga modal sebagai status; [jenis] menentukan tampilannya di [StatusHargaBaris]. */
class StatusHarga(val jenis: JenisHarga, val teks: String)

enum class JenisHarga { BARU, TETAP, NAIK, TURUN }

/**
 * Perubahan harga modal sebagai status: naik = selisih (perlu diperhatikan), turun = cocok,
 * tetap = titik + teks (netral), produk baru = chip aksen-container.
 */
@Composable
fun statusHarga(selisih: Long?, adaRiwayat: Boolean): StatusHarga = when {
    !adaRiwayat || selisih == null -> StatusHarga(JenisHarga.BARU, stringResource(R.string.harga_baru))
    selisih > 0 -> StatusHarga(JenisHarga.NAIK, stringResource(R.string.harga_naik, Rupiah.format(selisih)))
    selisih < 0 -> StatusHarga(JenisHarga.TURUN, stringResource(R.string.harga_turun, Rupiah.format(-selisih)))
    else -> StatusHarga(JenisHarga.TETAP, stringResource(R.string.harga_tetap))
}

/**
 * Tampilan status harga: "Harga tetap" = titik 8 dp + teks 14 sp teks-sekunder; "Harga baru" = chip aksen-container
 * tanpa ikon; naik/turun memakai [ChipStatus] (selisih/cocok). Selalu ada teks, tidak pernah warna saja.
 */
@Composable
fun StatusHargaBaris(h: StatusHarga, modifier: Modifier = Modifier) {
    val w = MaterialTheme.colorScheme
    when (h.jenis) {
        JenisHarga.NAIK -> ChipStatus(StatusBaris.SELISIH, modifier, h.teks)
        JenisHarga.TURUN -> ChipStatus(StatusBaris.COCOK, modifier, h.teks)
        JenisHarga.BARU -> Text(
            h.teks,
            style = GayaNota.keteranganTebal,
            color = w.onPrimaryContainer,
            modifier = modifier
                .background(w.primaryContainer, RoundedCornerShape(Radius.kecil))
                .padding(horizontal = Spasi.s, vertical = Spasi.xs),
        )
        JenisHarga.TETAP -> Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(Ukuran.titikStatus).background(w.onSurfaceVariant, CircleShape))
            Text(
                h.teks,
                style = MaterialTheme.typography.bodySmall,
                color = w.onSurfaceVariant,
                modifier = Modifier.padding(start = Spasi.s),
            )
        }
    }
}

@Composable
fun ChipSelisihHarga(selisih: Long?, adaRiwayat: Boolean, modifier: Modifier = Modifier) {
    StatusHargaBaris(statusHarga(selisih, adaRiwayat), modifier)
}
