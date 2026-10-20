package com.danibanan.notakulakan.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Peran warna persis dari docs/DESIGN.md bagian 1. Nama mengikuti tabel di sana.
 *
 * Pemetaan ke peran Material (lihat [Palet.skema]):
 * - latar -> background/surface; container -> surfaceContainer/surfaceVariant; container-high -> surfaceContainerHigh/Highest
 * - teks -> onSurface; teks-sekunder -> onSurfaceVariant (satu-satunya cara membuat teks sekunder)
 * - garis-kontrol -> outline; pemisah -> outlineVariant
 * - semua aksen -> primary; semua galat -> error dan errorContainer
 * - secondary dipetakan ke netral (teks-sekunder/container-high), tertiary ke aksen; tidak ada hue baru.
 * - Status "info" tidak punya warna: container-high + teks-sekunder + ikon.
 */
class Palet(
    val latar: Color,
    val container: Color,
    val containerHigh: Color,
    val teks: Color,
    val teksSekunder: Color,
    val garisKontrol: Color,
    val pemisah: Color,
    val aksen: Color,
    val diAtasAksen: Color,
    val aksenContainer: Color,
    val diAtasAksenContainer: Color,
    val galat: Color,
    val diAtasGalat: Color,
    val galatContainer: Color,
    val diAtasGalatContainer: Color,
)

val PaletTerang = Palet(
    latar = Color(0xFFF4F0E7),
    container = Color(0xFFEBE6DA),
    containerHigh = Color(0xFFE0DACB),
    teks = Color(0xFF2A2118),
    teksSekunder = Color(0xFF5E5446),
    garisKontrol = Color(0xFF867B69),
    pemisah = Color(0xFFD3CCBB),
    aksen = Color(0xFF1F5A45),
    diAtasAksen = Color(0xFFF7F4EC),
    aksenContainer = Color(0xFFD3E5DA),
    diAtasAksenContainer = Color(0xFF12382A),
    galat = Color(0xFFA63A2B),
    diAtasGalat = Color(0xFFFFF8F6),
    galatContainer = Color(0xFFF3D9D3),
    diAtasGalatContainer = Color(0xFF5E1A10),
)

val PaletGelap = Palet(
    latar = Color(0xFF1B1712),
    container = Color(0xFF241F18),
    containerHigh = Color(0xFF2E281F),
    teks = Color(0xFFEDE6D8),
    teksSekunder = Color(0xFFB7AC99),
    garisKontrol = Color(0xFF8F8472),
    pemisah = Color(0xFF3A3328),
    aksen = Color(0xFF7FC3A3),
    diAtasAksen = Color(0xFF0F2A20),
    aksenContainer = Color(0xFF1F3F31),
    diAtasAksenContainer = Color(0xFFCDE8DA),
    galat = Color(0xFFF0A294),
    diAtasGalat = Color(0xFF3A0F08),
    galatContainer = Color(0xFF4A1D15),
    diAtasGalatContainer = Color(0xFFF7D6CF),
)

// Scrim bukan token visual; hitam standar untuk lapisan di belakang sheet dan dialog.
private val Scrim = Color(0xFF000000)

/** Semua peran `ColorScheme` ditulis eksplisit; tidak memakai `lightColorScheme()`, `darkColorScheme()`, atau seed. */
fun Palet.skema(lawan: Palet): ColorScheme = ColorScheme(
    primary = aksen,
    onPrimary = diAtasAksen,
    primaryContainer = aksenContainer,
    onPrimaryContainer = diAtasAksenContainer,
    inversePrimary = lawan.aksen,
    secondary = teksSekunder,
    onSecondary = latar,
    secondaryContainer = containerHigh,
    onSecondaryContainer = teks,
    tertiary = aksen,
    onTertiary = diAtasAksen,
    tertiaryContainer = aksenContainer,
    onTertiaryContainer = diAtasAksenContainer,
    background = latar,
    onBackground = teks,
    surface = latar,
    onSurface = teks,
    surfaceVariant = container,
    onSurfaceVariant = teksSekunder,
    surfaceTint = aksen,
    inverseSurface = teks,
    inverseOnSurface = latar,
    error = galat,
    onError = diAtasGalat,
    errorContainer = galatContainer,
    onErrorContainer = diAtasGalatContainer,
    outline = garisKontrol,
    outlineVariant = pemisah,
    scrim = Scrim,
    surfaceBright = latar,
    surfaceDim = latar,
    surfaceContainerLowest = latar,
    surfaceContainerLow = latar,
    surfaceContainer = container,
    surfaceContainerHigh = containerHigh,
    surfaceContainerHighest = containerHigh,
)
