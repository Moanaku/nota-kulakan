package com.danibanan.notakulakan.ui.komponen

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import com.danibanan.notakulakan.ui.theme.Elevasi
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

/** Navigasi bawah: dua area nyata aplikasi (stok dan riwayat), tidak lebih. */
@Composable
fun BarBawah(aktif: Tab, onStok: () -> Unit, onRiwayat: () -> Unit) {
    val w = MaterialTheme.colorScheme
    val warna = NavigationBarItemDefaults.colors(
        selectedIconColor = w.onPrimaryContainer,
        selectedTextColor = w.onSurface,
        indicatorColor = w.primaryContainer,
        unselectedIconColor = w.onSurfaceVariant,
        unselectedTextColor = w.onSurfaceVariant,
    )
    NavigationBar(containerColor = w.surfaceContainer, tonalElevation = Elevasi.nol) {
        NavigationBarItem(
            selected = aktif == Tab.STOK,
            onClick = onStok,
            icon = { Icon(Icons.Outlined.ShoppingCart, contentDescription = null) },
            label = { Text(stringResource(R.string.tab_stok), style = MaterialTheme.typography.bodySmall) },
            colors = warna,
        )
        NavigationBarItem(
            selected = aktif == Tab.RIWAYAT,
            onClick = onRiwayat,
            icon = { Icon(Icons.Outlined.DateRange, contentDescription = null) },
            label = { Text(stringResource(R.string.tab_riwayat), style = MaterialTheme.typography.bodySmall) },
            colors = warna,
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
