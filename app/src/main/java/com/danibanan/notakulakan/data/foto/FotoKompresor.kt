package com.danibanan.notakulakan.data.foto

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.danibanan.notakulakan.BuildConfig
import com.danibanan.notakulakan.di.IoDispatcher
import com.danibanan.notakulakan.domain.UkuranFoto
import com.danibanan.notakulakan.domain.repo.PenyiapFoto
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Mengecilkan foto sebelum diunggah: sisi terpanjang FOTO_MAX_SISI, JPEG FOTO_KUALITAS.
 * Foto kamera 12-50 MP (3-10 MB) menjadi beberapa ratus KB, sehingga hemat kuota dan cepat di
 * jaringan seluler. Parameternya sama dengan harness evaluasi agar akurasi yang diukur berlaku
 * untuk foto yang benar-benar dikirim aplikasi.
 */
class FotoKompresor @Inject constructor(
    @ApplicationContext private val ctx: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) : PenyiapFoto {

    override suspend fun siapkan(uri: String): File = withContext(io) {
        kompres(Uri.parse(uri), BuildConfig.FOTO_MAX_SISI, BuildConfig.FOTO_KUALITAS)
    }

    private fun kompres(uri: Uri, maxSisi: Int, kualitas: Int): File {
        val resolver = ctx.contentResolver

        val ukuran = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, ukuran) }
            ?: throw IOException("foto tidak bisa dibuka")
        if (ukuran.outWidth <= 0 || ukuran.outHeight <= 0) throw IOException("bukan foto")

        val opsi = BitmapFactory.Options().apply {
            inSampleSize = UkuranFoto.inSampleSize(ukuran.outWidth, ukuran.outHeight, maxSisi)
        }
        val asli = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opsi) }
            ?: throw IOException("foto tidak bisa dibaca")

        val rotasi = resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0

        val (lebar, tinggi) = UkuranFoto.target(asli.width, asli.height, maxSisi)
        var hasil: Bitmap = if (lebar != asli.width || tinggi != asli.height) {
            Bitmap.createScaledBitmap(asli, lebar, tinggi, true).also { if (it !== asli) asli.recycle() }
        } else {
            asli
        }
        if (rotasi != 0) {
            val matriks = Matrix().apply { postRotate(rotasi.toFloat()) }
            val diputar = Bitmap.createBitmap(hasil, 0, 0, hasil.width, hasil.height, matriks, true)
            if (diputar !== hasil) hasil.recycle()
            hasil = diputar
        }

        val folder = File(ctx.cacheDir, "unggah").apply { mkdirs() }
        val berkas = File(folder, "${UUID.randomUUID()}.jpg")
        FileOutputStream(berkas).use { hasil.compress(Bitmap.CompressFormat.JPEG, kualitas, it) }
        hasil.recycle()
        return berkas
    }
}

/** Hapus file sementara (foto kamera dan hasil kompresi) yang lebih tua dari satu hari. */
fun bersihkanCacheFoto(ctx: Context, sekarang: Long = System.currentTimeMillis()) {
    val batas = sekarang - 24L * 60 * 60 * 1000
    for (nama in listOf("foto", "unggah")) {
        File(ctx.cacheDir, nama).listFiles()?.filter { it.lastModified() < batas }?.forEach { it.delete() }
    }
}
