package com.danibanan.notakulakan.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.danibanan.notakulakan.R

val SourceSans3 = FontFamily(
    Font(R.font.source_sans_3_regular, FontWeight.Normal),
    Font(R.font.source_sans_3_semibold, FontWeight.SemiBold),
)

val Newsreader = FontFamily(Font(R.font.newsreader_semibold, FontWeight.SemiBold))

/** Enam ukuran docs/DESIGN.md bagian 2. Angka memakai Source Sans 3 (angka bawaannya sudah tabular). */
object GayaNota {
    val angkaBesar = TextStyle(
        fontFamily = SourceSans3, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp,
    )
    val judulLayar = TextStyle(
        fontFamily = Newsreader, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp,
    )
    val judulBagian = TextStyle(
        fontFamily = Newsreader, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp,
    )
    val isi = TextStyle(
        fontFamily = SourceSans3, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp,
    )
    val isiTebal = TextStyle(
        fontFamily = SourceSans3, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp,
    )
    val keterangan = TextStyle(
        fontFamily = SourceSans3, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp,
    )
    val labelKecil = TextStyle(
        fontFamily = SourceSans3, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp,
    )
}

/**
 * Seluruh 15 peran Material di-override ke enam ukuran DESIGN.md:
 * display* -> angka-besar; headline dan titleLarge -> judul-layar atau judul-bagian (Newsreader);
 * titleMedium/Small, labelLarge -> isi-tebal; body Large/Medium -> isi; bodySmall, labelMedium -> keterangan;
 * labelSmall -> label-kecil.
 */
val TipografiNota = Typography(
    displayLarge = GayaNota.angkaBesar,
    displayMedium = GayaNota.angkaBesar,
    displaySmall = GayaNota.angkaBesar,
    headlineLarge = GayaNota.judulLayar,
    headlineMedium = GayaNota.judulLayar,
    headlineSmall = GayaNota.judulBagian,
    titleLarge = GayaNota.judulBagian,
    titleMedium = GayaNota.isiTebal,
    titleSmall = GayaNota.isiTebal,
    bodyLarge = GayaNota.isi,
    bodyMedium = GayaNota.isi,
    bodySmall = GayaNota.keterangan,
    labelLarge = GayaNota.isiTebal,
    labelMedium = GayaNota.keterangan,
    labelSmall = GayaNota.labelKecil,
)
