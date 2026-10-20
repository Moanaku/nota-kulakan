package com.danibanan.notakulakan.ui.komponen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.danibanan.notakulakan.ui.theme.Radius
import com.danibanan.notakulakan.ui.theme.Spasi
import com.danibanan.notakulakan.ui.theme.Ukuran
import com.danibanan.notakulakan.ui.theme.angka

/**
 * Satu baris barang dengan tiga tingkat: nama (isi-tebal), qty x harga (keterangan, teks-sekunder),
 * jumlah (isi-tebal, tabular, rata kanan); status sebagai chip. Jarak di dalam baris 4 dp, antar baris 16 dp
 * (8 dp atas + 8 dp bawah). Teks sudah diformat oleh pemanggil, mis. [qtyHarga] = "2 dus x Rp 45.000".
 * Berikan [onClick] bila baris bisa diketuk (membuka sheet koreksi).
 */
@Composable
fun BarisBarang(
    nama: String,
    qtyHarga: String,
    jumlah: String,
    status: StatusBaris,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val dasar = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(Radius.sedang))
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .defaultMinSize(minHeight = Ukuran.sentuhMin)
        .padding(vertical = Spasi.s)
    Column(dasar, verticalArrangement = Arrangement.spacedBy(Spasi.xs)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spasi.l), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spasi.xs)) {
                Text(nama, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    qtyHarga,
                    style = MaterialTheme.typography.bodySmall.angka(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                jumlah,
                style = MaterialTheme.typography.titleMedium.angka(),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(min = Spasi.xxxl),
            )
        }
        ChipStatus(status)
    }
}

/** Pemisah antar baris daftar (daftar = baris + divider, bukan kartu per baris). */
@Composable
fun PemisahBaris(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier, thickness = Ukuran.garis, color = MaterialTheme.colorScheme.outlineVariant)
}
