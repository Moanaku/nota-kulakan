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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import com.danibanan.notakulakan.ui.theme.Elevasi
import com.danibanan.notakulakan.ui.theme.GayaNota
import com.danibanan.notakulakan.ui.theme.Radius
import com.danibanan.notakulakan.ui.theme.Spasi
import com.danibanan.notakulakan.ui.theme.Ukuran

internal const val SKALA_FONT_SUSUN_VERTIKAL = 1.5f

/** Kartu dan judul bagian dengan keterangan di kanan lebih sempit daripada baris total: susun lebih awal. */
internal const val SKALA_FONT_SUSUN_SEMPIT = 1.25f

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
    // Label total tetap isi 16 teks-sekunder: yang dominan hanya angka-besar, bukan label serifnya.
    val labelGaya = MaterialTheme.typography.bodyLarge
    val labelWarna = MaterialTheme.colorScheme.onSurfaceVariant
    val nilaiGaya = if (dominan) GayaNota.angkaBesar else MaterialTheme.typography.bodyLarge
    if (LocalDensity.current.fontScale >= SKALA_FONT_SUSUN_VERTIKAL) {
        // Skala besar: label di atas, angka di bawahnya, supaya label tidak terpecah menjadi baris yatim.
        Column(
            modifier = modifier.fillMaxWidth().padding(vertical = Spasi.xs),
            verticalArrangement = Arrangement.spacedBy(Spasi.xs),
        ) {
            Text(label, style = labelGaya, color = labelWarna)
            Text(nilai, style = nilaiGaya, color = MaterialTheme.colorScheme.onSurface)
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth().padding(vertical = Spasi.xs),
            horizontalArrangement = Arrangement.spacedBy(Spasi.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, modifier = Modifier.weight(1f), style = labelGaya, color = labelWarna)
            Text(nilai, style = nilaiGaya, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End)
        }
    }
}

/**
 * Ringkasan satu nota (objek yang berdiri sendiri, jadi boleh berbentuk kartu): kartu terisi `container` dengan garis
 * `pemisah`, radius 16, padding 16, elevasi 0. Toko (isi-tebal); tanggal dan jumlah barang (keterangan) di kiri,
 * total (isi-tebal) rata kanan; di skala font besar total pindah ke baris sendiri. [status] null = tidak ditampilkan
 * (mis. riwayat yang belum menyimpan status).
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
    val bentuk = RoundedCornerShape(Radius.besar)
    val w = MaterialTheme.colorScheme
    val rincian: @Composable (Modifier) -> Unit = {
        Text("$tanggal · $jumlahBarang", style = MaterialTheme.typography.bodySmall, color = w.onSurfaceVariant, modifier = it)
    }
    val jumlah: @Composable (Modifier) -> Unit = {
        Text(total, style = MaterialTheme.typography.titleMedium, color = w.onSurface, textAlign = TextAlign.End, modifier = it)
    }
    Surface(
        modifier = modifier.fillMaxWidth().clip(bentuk).let { if (onClick != null) it.clickable(onClick = onClick) else it },
        shape = bentuk,
        color = w.surfaceContainer,
        tonalElevation = Elevasi.nol,
        shadowElevation = Elevasi.nol,
        border = BorderStroke(Ukuran.garis, w.outlineVariant),
    ) {
        Column(Modifier.padding(Spasi.l), verticalArrangement = Arrangement.spacedBy(Spasi.xs)) {
            Text(toko, style = MaterialTheme.typography.titleMedium, color = w.onSurface)
            if (LocalDensity.current.fontScale >= SKALA_FONT_SUSUN_SEMPIT) {
                rincian(Modifier)
                jumlah(Modifier)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Spasi.l), verticalAlignment = Alignment.Top) {
                    rincian(Modifier.weight(1f))
                    jumlah(Modifier)
                }
            }
            if (status != null) ChipStatus(status, Modifier.padding(top = Spasi.xs))
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
    val dasar = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(Radius.sedang))
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .defaultMinSize(minHeight = Ukuran.sentuhMin)
        .padding(vertical = Spasi.s)
    val kiri: @Composable (Modifier) -> Unit = {
        Column(it, verticalArrangement = Arrangement.spacedBy(Spasi.xs)) {
            Text(toko, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "$tanggal · $jumlahBarang",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    val kanan: @Composable (Modifier) -> Unit = {
        Text(
            total,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = it,
        )
    }
    if (LocalDensity.current.fontScale >= SKALA_FONT_SUSUN_VERTIKAL) {
        // Skala besar: total pindah ke bawah, supaya nama toko tidak terhimpit menjadi satu kata per baris.
        Column(dasar, verticalArrangement = Arrangement.spacedBy(Spasi.xs)) {
            kiri(Modifier)
            kanan(Modifier)
        }
    } else {
        Row(dasar, horizontalArrangement = Arrangement.spacedBy(Spasi.l), verticalAlignment = Alignment.Top) {
            kiri(Modifier.weight(1f))
            kanan(Modifier.widthIn(min = Spasi.xxxl))
        }
    }
}
