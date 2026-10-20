package com.danibanan.notakulakan.ui.komponen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.danibanan.notakulakan.R
import com.danibanan.notakulakan.ui.theme.Durasi
import com.danibanan.notakulakan.ui.theme.Radius
import com.danibanan.notakulakan.ui.theme.Spasi
import com.danibanan.notakulakan.ui.theme.Ukuran
import com.danibanan.notakulakan.ui.theme.spekGerak

/** Dua keadaan kosong yang berbeda: belum pernah ada data, atau filter/pencarian tidak menemukan apa pun. */
enum class JenisKosong { BELUM_ADA_DATA, HASIL_FILTER_KOSONG }

/**
 * Kondisi data kosong. [objek] adalah kata benda tanpa artikel ("nota", "barang"). [teksAksi] mis. "Foto nota"
 * untuk BELUM_ADA_DATA; untuk HASIL_FILTER_KOSONG bawaannya "Hapus filter".
 */
@Composable
fun DataKosong(
    jenis: JenisKosong,
    objek: String,
    modifier: Modifier = Modifier,
    teksAksi: String? = null,
    onAksi: (() -> Unit)? = null,
) {
    val belumAda = jenis == JenisKosong.BELUM_ADA_DATA
    PesanTengah(
        ikon = if (belumAda) Icons.Outlined.Info else Icons.Outlined.Search,
        judul = stringResource(if (belumAda) R.string.kosong_belum_ada_judul else R.string.kosong_filter_judul, objek),
        isi = stringResource(if (belumAda) R.string.kosong_belum_ada_isi else R.string.kosong_filter_isi, objek),
        modifier = modifier,
        teksAksi = teksAksi ?: if (belumAda) null else stringResource(R.string.kosong_hapus_filter),
        onAksi = onAksi,
    )
}

/** Galat: sebab dalam kalimat Indonesia (bukan pesan exception) + tombol "Coba lagi". */
@Composable
fun KondisiGalat(
    sebab: String,
    onCobaLagi: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PesanTengah(
        ikon = Icons.Outlined.Warning,
        judul = stringResource(R.string.galat_judul),
        isi = sebab,
        modifier = modifier,
        teksAksi = stringResource(R.string.galat_coba_lagi),
        onAksi = onCobaLagi,
        galat = true,
    )
}

@Composable
private fun PesanTengah(
    ikon: ImageVector,
    judul: String,
    isi: String,
    modifier: Modifier,
    teksAksi: String?,
    onAksi: (() -> Unit)?,
    galat: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Spasi.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spasi.m),
    ) {
        Icon(
            ikon,
            contentDescription = null,
            tint = if (galat) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Ukuran.ikon),
        )
        Text(
            judul,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            isi,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (teksAksi != null && onAksi != null) {
            if (galat) TombolUtama(teksAksi, onClick = onAksi) else TombolSekunder(teksAksi, onClick = onAksi)
        }
    }
}

/**
 * Skeleton berbentuk baris barang (nama, qty x harga, jumlah, chip), statis. Dibaca pembaca layar sebagai
 * satu keterangan "Memuat daftar barang".
 */
@Composable
fun SkeletonBarisBarang(modifier: Modifier = Modifier, jumlahBaris: Int = 3) {
    val keterangan = stringResource(R.string.skeleton_memuat_barang)
    Column(modifier.fillMaxWidth().semantics { contentDescription = keterangan }) {
        repeat(jumlahBaris) { i ->
            Column(
                Modifier.fillMaxWidth().padding(vertical = Spasi.s),
                verticalArrangement = Arrangement.spacedBy(Spasi.xs),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spasi.l)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spasi.xs)) {
                        Blok(Modifier.fillMaxWidth(0.6f).height(Spasi.xl))
                        Blok(Modifier.fillMaxWidth(0.4f).height(Spasi.l))
                    }
                    Blok(Modifier.width(Spasi.xxxl + Spasi.xl).height(Spasi.xl))
                }
                Blok(Modifier.width(Spasi.xxxl + Spasi.xxl).height(Spasi.xl))
            }
            if (i < jumlahBaris - 1) PemisahBaris()
        }
    }
}

@Composable
private fun Blok(modifier: Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(Radius.kecil)))
}

/** Spanduk offline di atas daftar. Ikon + kalimat, warna container-high (peran info). */
@Composable
fun SpandukOffline(tampil: Boolean, modifier: Modifier = Modifier, teks: String = stringResource(R.string.offline_spanduk)) {
    AnimatedVisibility(
        visible = tampil,
        enter = fadeIn(spekGerak(Durasi.sedang)) + expandVertically(spekGerak(Durasi.sedang)),
        exit = fadeOut(spekGerak(Durasi.cepat)) + shrinkVertically(spekGerak(Durasi.cepat)),
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = Spasi.l, vertical = Spasi.m),
            horizontalArrangement = Arrangement.spacedBy(Spasi.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Ukuran.ikon),
            )
            Text(teks, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
