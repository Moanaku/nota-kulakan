package com.danibanan.notakulakan.ui.komponen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.danibanan.notakulakan.ui.theme.Radius
import com.danibanan.notakulakan.ui.theme.Spasi
import com.danibanan.notakulakan.ui.theme.Ukuran

private val BentukTombol = RoundedCornerShape(Radius.sedang)
private val PaddingTombol = PaddingValues(horizontal = Spasi.l, vertical = Spasi.s)

@Composable
private fun IsiTombol(teks: String, ikon: ImageVector?) {
    if (ikon != null) {
        // Ikon dekoratif: teks di sebelahnya sudah menjelaskan aksi.
        Icon(ikon, contentDescription = null, modifier = Modifier.size(Ukuran.ikon))
        Spacer(Modifier.width(Spasi.s))
    }
    Text(teks, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
}

/** Aksi utama layar. Label menyebut objeknya ("Simpan nota", bukan "Simpan"). */
@Composable
fun TombolUtama(
    teks: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    ikon: ImageVector? = null,
) {
    val w = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = Ukuran.sentuhMin),
        enabled = enabled,
        shape = BentukTombol,
        colors = ButtonDefaults.buttonColors(
            containerColor = w.primary,
            contentColor = w.onPrimary,
            disabledContainerColor = w.surfaceContainerHigh,
            disabledContentColor = w.onSurfaceVariant,
        ),
        elevation = null,
        contentPadding = PaddingTombol,
    ) { IsiTombol(teks, ikon) }
}

/** Aksi pendamping: outline `garis-kontrol` (kontras >= 3:1 terhadap latar). */
@Composable
fun TombolSekunder(
    teks: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    ikon: ImageVector? = null,
) {
    val w = MaterialTheme.colorScheme
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = Ukuran.sentuhMin),
        enabled = enabled,
        shape = BentukTombol,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = w.onSurface,
            disabledContentColor = w.onSurfaceVariant,
        ),
        border = BorderStroke(Ukuran.garis, if (enabled) w.outline else w.outlineVariant),
        contentPadding = PaddingTombol,
    ) { IsiTombol(teks, ikon) }
}

/** Aksi ringan tanpa wadah (mis. "Batal"). */
@Composable
fun TombolTeks(
    teks: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    ikon: ImageVector? = null,
) {
    val w = MaterialTheme.colorScheme
    TextButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = Ukuran.sentuhMin),
        enabled = enabled,
        shape = BentukTombol,
        colors = ButtonDefaults.textButtonColors(
            contentColor = w.primary,
            disabledContentColor = w.onSurfaceVariant,
        ),
        contentPadding = PaddingTombol,
    ) { IsiTombol(teks, ikon) }
}
