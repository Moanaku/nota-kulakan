package com.danibanan.notakulakan.ui.komponen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.danibanan.notakulakan.R
import com.danibanan.notakulakan.domain.model.Isu
import com.danibanan.notakulakan.domain.model.ItemBaris
import com.danibanan.notakulakan.domain.model.Tingkat
import com.danibanan.notakulakan.ui.theme.Radius
import com.danibanan.notakulakan.ui.theme.Spasi
import com.danibanan.notakulakan.ui.theme.Ukuran

/** Status hasil pemeriksaan aturan hitungan. Selalu tampil sebagai ikon + teks + warna. */
enum class StatusBaris { COCOK, SELISIH, BELUM_TERBACA }

/**
 * Status chip satu baris, dipakai Periksa dan Detail supaya sama. Baris dengan qty, harga, atau jumlah yang belum
 * terbaca selalu "belum terbaca" (isu "belum terisi" dari validator tetap tampil sebagai pesan di Periksa);
 * selain itu salah satu isu galat berarti selisih, kalau tidak cocok.
 */
fun statusBaris(item: ItemBaris, isu: List<Isu>): StatusBaris = when {
    item.qty == null || item.hargaSatuan == null || item.jumlah == null -> StatusBaris.BELUM_TERBACA
    isu.any { it.tingkat == Tingkat.GALAT } -> StatusBaris.SELISIH
    else -> StatusBaris.COCOK
}

/**
 * Chip status. cocok = aksen-container; selisih = galat-container; belum terbaca = container-high + teks-sekunder
 * (peran "info" tidak punya warna sendiri). [teks] mengganti label bawaan, mis. "Selisih Rp 500".
 */
@Composable
fun ChipStatus(status: StatusBaris, modifier: Modifier = Modifier, teks: String? = null) {
    val w = MaterialTheme.colorScheme
    val (latar, isi, ikon: ImageVector, label) = when (status) {
        StatusBaris.COCOK -> Spec(w.primaryContainer, w.onPrimaryContainer, Icons.Outlined.Check, R.string.chip_cocok)
        StatusBaris.SELISIH -> Spec(w.errorContainer, w.onErrorContainer, Icons.Outlined.Warning, R.string.chip_selisih)
        StatusBaris.BELUM_TERBACA ->
            Spec(w.surfaceContainerHigh, w.onSurfaceVariant, Icons.Outlined.Info, R.string.chip_belum_terbaca)
    }
    Row(
        modifier = modifier
            .background(latar, RoundedCornerShape(Radius.kecil))
            .padding(horizontal = Spasi.s, vertical = Spasi.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(ikon, contentDescription = null, tint = isi, modifier = Modifier.size(Ukuran.ikon))
        Text(
            text = teks ?: stringResource(label),
            color = isi,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = Spasi.xs),
        )
    }
}

private data class Spec(
    val latar: androidx.compose.ui.graphics.Color,
    val isi: androidx.compose.ui.graphics.Color,
    val ikon: ImageVector,
    val label: Int,
)
