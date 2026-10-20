package com.danibanan.notakulakan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable

private val BentukNota = Shapes(
    extraSmall = RoundedCornerShape(Radius.kecil),
    small = RoundedCornerShape(Radius.kecil),
    medium = RoundedCornerShape(Radius.sedang),
    large = RoundedCornerShape(Radius.besar),
    extraLarge = RoundedCornerShape(Radius.besar),
)

/** Tema aplikasi menurut docs/DESIGN.md. Mengikuti mode gelap sistem; tanpa dynamic color. */
@Composable
fun NotaTheme(gelap: Boolean = isSystemInDarkTheme(), isi: @Composable () -> Unit) {
    val skema = if (gelap) PaletGelap.skema(lawan = PaletTerang) else PaletTerang.skema(lawan = PaletGelap)
    MaterialTheme(colorScheme = skema, typography = TipografiNota, shapes = BentukNota, content = isi)
}
