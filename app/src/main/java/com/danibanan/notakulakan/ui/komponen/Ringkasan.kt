package com.danibanan.notakulakan.ui.komponen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.danibanan.notakulakan.ui.theme.Elevasi
import com.danibanan.notakulakan.ui.theme.GayaNota
import com.danibanan.notakulakan.ui.theme.Radius
import com.danibanan.notakulakan.ui.theme.Spasi
import com.danibanan.notakulakan.ui.theme.Ukuran

/**
 * Baris label + nilai rupiah (subtotal, diskon, PPN). Dengan [dominan] = true dipakai untuk total nota:
 * angka-besar, satu-satunya elemen dominan di layar hasil.
 */
@Composable
fun BarisRingkasanTotal(
    label: String,
    nilai: String,
    modifier: Modifier = Modifier,
    dominan: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = Spasi.xs),
        horizontalArrangement = Arrangement.spacedBy(Spasi.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = if (dominan) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            color = if (dominan) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            nilai,
            style = if (dominan) GayaNota.angkaBesar else MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
        )
    }
}

/**
 * Ringkasan satu nota (objek yang berdiri sendiri, jadi boleh berbentuk kartu): toko, tanggal, jumlah barang,
 * total, status ([status] null = tidak ditampilkan, mis. riwayat yang belum menyimpan status). Kartu = outline `pemisah`, elevasi 0, tanpa bayangan.
 */
@Composable
fun KartuRingkasanNota(
    toko: String,
    tanggal: String,
    jumlahBarang: String,
    total: String,
    status: StatusBaris?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val bentuk = RoundedCornerShape(Radius.sedang)
    Surface(
        modifier = modifier.fillMaxWidth().clip(bentuk).let { if (onClick != null) it.clickable(onClick = onClick) else it },
        shape = bentuk,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = Elevasi.nol,
        shadowElevation = Elevasi.nol,
        border = BorderStroke(Ukuran.garis, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(Spasi.l), verticalArrangement = Arrangement.spacedBy(Spasi.s)) {
            Text(toko, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "$tanggal · $jumlahBarang",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spasi.l),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    total,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (status != null) ChipStatus(status)
            }
        }
    }
}

/**
 * Satu nota dalam daftar riwayat (daftar = baris + divider, bukan kartu per nota): toko (isi-tebal), tanggal dan
 * jumlah barang (keterangan), total rata kanan. Beri [PemisahBaris] di antara baris.
 */
@Composable
fun BarisNota(
    toko: String,
    tanggal: String,
    jumlahBarang: String,
    total: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.sedang))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .defaultMinSize(minHeight = Ukuran.sentuhMin)
            .padding(vertical = Spasi.s),
        horizontalArrangement = Arrangement.spacedBy(Spasi.l),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spasi.xs)) {
            Text(toko, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "$tanggal · $jumlahBarang",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            total,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = Spasi.xxxl),
        )
    }
}
