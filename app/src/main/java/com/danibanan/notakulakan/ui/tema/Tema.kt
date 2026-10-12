package com.danibanan.notakulakan.ui.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Warna status di luar skema Material: cocok, perlu dicek, galat. Kontras teks >= 4.5:1. */
@Immutable
data class WarnaStatus(
    val cocokLatar: Color,
    val cocokTeks: Color,
    val cekLatar: Color,
    val cekTeks: Color,
    val galatLatar: Color,
    val galatTeks: Color,
    val netralLatar: Color,
    val netralTeks: Color,
)

private val StatusTerang = WarnaStatus(
    cocokLatar = Color(0xFFDCF2E8), cocokTeks = Color(0xFF17573F),
    cekLatar = Color(0xFFFEF3C7), cekTeks = Color(0xFF92400E),
    galatLatar = Color(0xFFFDECEA), galatTeks = Color(0xFF8A1C12),
    netralLatar = Color(0xFFE8ECE9), netralTeks = Color(0xFF44504A),
)

private val StatusGelap = WarnaStatus(
    cocokLatar = Color(0xFF173B2D), cocokTeks = Color(0xFFA7E3C6),
    cekLatar = Color(0xFF3D2E0B), cekTeks = Color(0xFFFCD98A),
    galatLatar = Color(0xFF3E1512), galatTeks = Color(0xFFFFB4AB),
    netralLatar = Color(0xFF2A322E), netralTeks = Color(0xFFC3CCC6),
)

val LocalWarnaStatus = staticCompositionLocalOf { StatusTerang }

private val SkemaTerang = lightColorScheme(
    primary = Color(0xFF1F6F54),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCF2E8),
    onPrimaryContainer = Color(0xFF0B3A2A),
    secondary = Color(0xFF4E6359),
    background = Color(0xFFF6F7F4),
    onBackground = Color(0xFF17211C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17211C),
    surfaceVariant = Color(0xFFE8ECE9),
    onSurfaceVariant = Color(0xFF5B6760),
    outline = Color(0xFFAEB9B2),
    outlineVariant = Color(0xFFE3E8E4),
    error = Color(0xFFB42318),
    onError = Color.White,
)

private val SkemaGelap = darkColorScheme(
    primary = Color(0xFF7FD3AE),
    onPrimary = Color(0xFF003824),
    primaryContainer = Color(0xFF175340),
    onPrimaryContainer = Color(0xFFDCF2E8),
    secondary = Color(0xFFB4CCC0),
    background = Color(0xFF111614),
    onBackground = Color(0xFFE1E4E1),
    surface = Color(0xFF181E1B),
    onSurface = Color(0xFFE1E4E1),
    surfaceVariant = Color(0xFF2A322E),
    onSurfaceVariant = Color(0xFFC3CCC6),
    outline = Color(0xFF8C9790),
    outlineVariant = Color(0xFF3A433F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun TemaNota(gelap: Boolean = isSystemInDarkTheme(), isi: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalWarnaStatus provides if (gelap) StatusGelap else StatusTerang,
    ) {
        MaterialTheme(colorScheme = if (gelap) SkemaGelap else SkemaTerang, content = isi)
    }
}

object WarnaNota {
    val status: WarnaStatus
        @Composable get() = LocalWarnaStatus.current
}
