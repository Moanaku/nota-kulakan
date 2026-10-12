package com.danibanan.notakulakan.data.remote

import android.content.Context
import android.util.Log
import com.danibanan.notakulakan.di.IoDispatcher
import com.danibanan.notakulakan.domain.repo.EkstraksiRepository
import com.danibanan.notakulakan.domain.repo.HasilEkstraksi
import com.danibanan.notakulakan.domain.repo.JenisGagal
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response as OkResponse
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface NotaApi {
    @Multipart
    @POST("v1/ekstrak")
    suspend fun ekstrak(@Part foto: MultipartBody.Part): Response<EkstrakRespons>
}

/**
 * Token App Check SEKALI PAKAI (limited-use). Backend mengonsumsinya saat verifikasi, jadi token
 * yang disadap tidak bisa dipakai ulang. Tanpa Firebase (build tanpa google-services.json) tidak
 * ada token, dan backend harus berjalan dengan APP_CHECK=off.
 */
@Singleton
class PenyediaTokenAppCheck @Inject constructor(@ApplicationContext private val ctx: Context) {
    fun tokenSekaliPakai(): String? {
        if (FirebaseApp.getApps(ctx).isEmpty()) return null
        return try {
            Tasks.await(FirebaseAppCheck.getInstance().limitedUseAppCheckToken, 15, TimeUnit.SECONDS).token
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        } catch (e: Exception) {
            Log.w("AppCheck", "token gagal diambil: ${e.javaClass.simpleName}")
            null
        }
    }
}

/** Berjalan di thread OkHttp (bukan main thread), jadi boleh menunggu token. */
class AppCheckInterceptor @Inject constructor(private val penyedia: PenyediaTokenAppCheck) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): OkResponse {
        val token = penyedia.tokenSekaliPakai()
        val permintaan = if (token != null) {
            chain.request().newBuilder().header("X-Firebase-AppCheck", token).build()
        } else {
            chain.request()
        }
        return chain.proceed(permintaan)
    }
}

class EkstraksiRepositoryImpl @Inject constructor(
    private val api: NotaApi,
    private val json: Json,
    @IoDispatcher private val io: CoroutineDispatcher,
) : EkstraksiRepository {

    override suspend fun ekstrak(foto: File): HasilEkstraksi = withContext(io) {
        val bagian = MultipartBody.Part.createFormData(
            "foto", foto.name, foto.asRequestBody("image/jpeg".toMediaType()),
        )
        try {
            val respons = api.ekstrak(bagian)
            val badan = respons.body()
            if (respons.isSuccessful && badan != null) {
                badan.keDomain()
            } else {
                HasilEkstraksi.Gagal(jenisGagalDari(respons.code(), bacaKodeGalat(respons.errorBody()?.string())))
            }
        } catch (e: InterruptedIOException) {
            // Termasuk SocketTimeoutException dan batas callTimeout.
            HasilEkstraksi.Gagal(JenisGagal.WAKTU_HABIS)
        } catch (e: IOException) {
            HasilEkstraksi.Gagal(JenisGagal.TANPA_JARINGAN)
        } catch (e: SerializationException) {
            HasilEkstraksi.Gagal(JenisGagal.GANGGUAN_SERVER)
        }
    }

    private fun bacaKodeGalat(teks: String?): String? =
        teks?.let { runCatching { json.decodeFromString(GalatRespons.serializer(), it).galat.kode }.getOrNull() }
}
