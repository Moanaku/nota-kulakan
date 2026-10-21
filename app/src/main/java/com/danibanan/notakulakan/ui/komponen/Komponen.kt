package com.danibanan.notakulakan.ui.komponen

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.danibanan.notakulakan.ui.theme.Ukuran
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.danibanan.notakulakan.R
import com.danibanan.notakulakan.domain.UkuranFoto
import com.danibanan.notakulakan.ui.theme.Radius
import com.danibanan.notakulakan.ui.theme.Spasi
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val FORMAT_TANGGAL: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy, HH.mm", Locale.forLanguageTag("id-ID"))

fun formatTanggal(milidetik: Long): String =
    FORMAT_TANGGAL.format(Instant.ofEpochMilli(milidetik).atZone(ZoneId.systemDefault()))

enum class Tab { STOK, RIWAYAT }

/**
 * Navigasi bawah tiga posisi (DESIGN.md `BarBawahTengah`): Stok dan Riwayat adalah tab; "Catat nota" di tengah
 * adalah AKSI (membuka alur foto), bukan tab, jadi tidak pernah ditandai aktif. Tombol tengah terangkat di atas
 * bilah tanpa bayangan, dikelilingi cincin warna latar. Bagian yang terangkat ada di dalam batas komponen (jalur
 * transparan setinggi [Spasi.xxl] di atas bilah) supaya tetap bisa diketuk.
 */
@Composable
fun BarBawahTengah(aktif: Tab, onStok: () -> Unit, onRiwayat: () -> Unit, onCatatNota: () -> Unit) {
    val w = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Spacer(Modifier.height(Spasi.xxl))
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(w.surfaceContainer)
                    .windowInsetsPadding(
                        WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                    ),
            ) {
                HorizontalDivider(thickness = Ukuran.garis, color = w.outlineVariant)
                Row(Modifier.fillMaxWidth().heightIn(min = Ukuran.bilahNavigasi)) {
                    ItemTab(
                        Icons.Outlined.ShoppingCart, stringResource(R.string.tab_stok),
                        aktif == Tab.STOK, onStok, Modifier.weight(1f),
                    )
                    Spacer(Modifier.weight(1f))
                    ItemTab(
                        Icons.Outlined.DateRange, stringResource(R.string.tab_riwayat),
                        aktif == Tab.RIWAYAT, onRiwayat, Modifier.weight(1f),
                    )
                }
            }
        }
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .widthIn(min = Ukuran.penandaLebar)
                .clickable(role = Role.Button, onClick = onCatatNota)
                .semantics(mergeDescendants = true) {},
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Cincin: kotak bulat warna latar mengelilingi tombol; tombol naik 24 dp di atas tepi bilah.
            Box(
                Modifier
                    .padding(top = Spasi.xs)
                    .size(Ukuran.tombolTengah + Ukuran.cincinTombolTengah * 2)
                    .background(w.background, RoundedCornerShape(Radius.besar))
                    .padding(Ukuran.cincinTombolTengah)
                    .background(w.primary, RoundedCornerShape(Radius.besar)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = null,
                    tint = w.onPrimary,
                    modifier = Modifier.size(Ukuran.ikon),
                )
            }
            Text(
                stringResource(R.string.catat_nota),
                style = MaterialTheme.typography.labelSmall,
                color = w.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spasi.s), // sebaris dengan label tab
            )
        }
    }
}

@Composable
private fun ItemTab(ikon: ImageVector, label: String, aktif: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val w = MaterialTheme.colorScheme
    Column(
        modifier
            .heightIn(min = Ukuran.bilahNavigasi)
            .selectable(selected = aktif, onClick = onClick, role = Role.Tab)
            .padding(top = Spasi.s),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(Ukuran.penandaLebar, Ukuran.penandaTinggi)
                .background(if (aktif) w.primaryContainer else Color.Transparent, RoundedCornerShape(Radius.besar)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                ikon,
                contentDescription = null,
                tint = if (aktif) w.onPrimaryContainer else w.onSurfaceVariant,
                modifier = Modifier.size(Ukuran.ikon),
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (aktif) w.onSurface else w.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spasi.xs),
        )
    }
}

/** Foto nota dari file lokal, didekode kecil di thread IO agar tidak memakan memori. */
@Composable
fun FotoNota(path: String?, modifier: Modifier = Modifier, lebarMaks: Int = 1200) {
    val gambar by produceState<ImageBitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) {
            path?.let { File(it) }?.takeIf { it.exists() }?.let { f ->
                val ukuran = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(f.absolutePath, ukuran)
                val opsi = BitmapFactory.Options().apply {
                    inSampleSize = UkuranFoto.inSampleSize(ukuran.outWidth, ukuran.outHeight, lebarMaks)
                }
                BitmapFactory.decodeFile(f.absolutePath, opsi)?.asImageBitmap()
            }
        }
    }
    val foto = gambar
    if (foto != null) {
        Image(
            bitmap = foto,
            contentDescription = stringResource(R.string.lihat_foto),
            contentScale = ContentScale.FillWidth,
            modifier = modifier.fillMaxWidth(),
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(Radius.sedang))
                .padding(Spasi.xl),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = stringResource(R.string.foto_tidak_tersedia),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
