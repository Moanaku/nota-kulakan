package com.danibanan.notakulakan.ui.komponen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.danibanan.notakulakan.ui.theme.Radius
import com.danibanan.notakulakan.ui.theme.Spasi

enum class JenisPesan { GALAT, INFO, COCOK }

/** Pesan pemeriksaan satu baris atau nota: ikon + kalimat + warna, tidak pernah warna saja. */
@Composable
fun PesanPeriksa(pesan: String, jenis: JenisPesan, modifier: Modifier = Modifier) {
    val w = MaterialTheme.colorScheme
    val (latar, isi, ikon) = when (jenis) {
        JenisPesan.GALAT -> Triple(w.errorContainer, w.onErrorContainer, Icons.Outlined.Warning)
        JenisPesan.COCOK -> Triple(w.primaryContainer, w.onPrimaryContainer, Icons.Outlined.Check)
        JenisPesan.INFO -> Triple(w.surfaceContainerHigh, w.onSurfaceVariant, Icons.Outlined.Info)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(latar, RoundedCornerShape(Radius.kecil))
            .padding(Spasi.m),
        horizontalArrangement = Arrangement.spacedBy(Spasi.s),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(ikon, contentDescription = null, tint = isi)
        Text(pesan, style = MaterialTheme.typography.bodySmall, color = isi, modifier = Modifier.weight(1f))
    }
}
