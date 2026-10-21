package com.danibanan.notakulakan.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Peran warna persis dari docs/DESIGN.md bagian 1 (v2). Nama mengikuti tabel di sana.
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
    latar = Color(0xFFFBF7EF),
    container = Color(0xFFF1EADB),
    containerHigh = Color(0xFFE6DDC8),
    teks = Color(0xFF231F18),
    teksSekunder = Color(0xFF4A4234),
    garisKontrol = Color(0xFF756B58),
    pemisah = Color(0xFFDDD3BE),
    aksen = Color(0xFF38683F),
    diAtasAksen = Color(0xFFFFFDF8),
    aksenContainer = Color(0xFFDCEBD6),
    diAtasAksenContainer = Color(0xFF234A28),
    galat = Color(0xFFB3361E),
    diAtasGalat = Color(0xFFFFF8F6),
    galatContainer = Color(0xFFFBE3DC),
    diAtasGalatContainer = Color(0xFF7A1F10),
)

val PaletGelap = Palet(
    latar = Color(0xFF17140F),
    container = Color(0xFF211D16),
    containerHigh = Color(0xFF2B261D),
    teks = Color(0xFFF3ECDD),
    teksSekunder = Color(0xFFD2C8B3),
    garisKontrol = Color(0xFFA09580),
    pemisah = Color(0xFF3A3328),
    aksen = Color(0xFF8FCB9B),
    diAtasAksen = Color(0xFF0F2A18),
    aksenContainer = Color(0xFF24412B),
    diAtasAksenContainer = Color(0xFFD6EBD8),
    galat = Color(0xFFF2A596),
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
