package com.danibanan.notakulakan.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Skala spasi docs/DESIGN.md bagian 3. Tidak ada nilai lain. Gutter layar = [Spasi.l]. */
object Spasi {
    val xs: Dp = 4.dp
    val s: Dp = 8.dp
    val m: Dp = 12.dp
    val l: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val xxxl: Dp = 48.dp
}

/** 4 dp untuk input dan chip, 8 dp untuk tombol dan baris yang bisa diketuk, 16 dp untuk sheet dan dialog. */
object Radius {
    val kecil: Dp = 4.dp
    val sedang: Dp = 8.dp
    val besar: Dp = 16.dp
}

/** 0 dp bawaan (kartu = outline pemisah), 2 dp hanya sheet bawah dan dialog. */
object Elevasi {
    val nol: Dp = 0.dp
    val tinggi: Dp = 2.dp
}

object Ukuran {
    /** Target sentuh minimum. */
    val sentuhMin: Dp = 48.dp

    /** Ukuran dasar ikon. */
    val ikon: Dp = 24.dp

    /** Tebal garis outline (kartu, input, tombol sekunder, divider). */
    val garis: Dp = 1.dp

    /** Tinggi minimum bilah navigasi bawah (DESIGN.md `BarBawahTengah`). */
    val bilahNavigasi: Dp = 72.dp

    /** Penanda tab aktif: pil 64 x 32 dp, radius [Radius.besar]. */
    val penandaLebar: Dp = 64.dp
    val penandaTinggi: Dp = 32.dp

    /** Tombol aksi tengah bilah: persegi bulat 56 dp, cincin 4 dp warna latar, terangkat 24 dp. */
    val tombolTengah: Dp = 56.dp
    val cincinTombolTengah: Dp = 4.dp

    /** Titik penanda status "Harga tetap". */
    val titikStatus: Dp = 8.dp
}

/** Satu easing untuk semua transisi dalam layar: cubic-bezier(0.2, 0, 0, 1). */
val EasingNota = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** Durasi transisi dalam milidetik. */
object Durasi {
    const val cepat = 150
    const val sedang = 200
    const val lambat = 300
}

/** Spesifikasi animasi bergaya Nota; durasi 0 bila pengguna mematikan animasi di sistem. */
@Composable
fun <T> spekGerak(durasi: Int = Durasi.sedang): TweenSpec<T> {
    val konteks = LocalContext.current
    val animasiMati = remember(konteks) {
        Settings.Global.getFloat(konteks.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    return tween(durationMillis = if (animasiMati) 0 else durasi, easing = EasingNota)
}
