package com.danibanan.notakulakan.di

import android.content.Context
import androidx.room.Room
import com.danibanan.notakulakan.BuildConfig
import com.danibanan.notakulakan.PengaturanVarian
import com.danibanan.notakulakan.data.analitik.FirebaseAnalitik
import com.danibanan.notakulakan.data.foto.FotoKompresor
import com.danibanan.notakulakan.data.lokal.BasisdataNota
import com.danibanan.notakulakan.data.lokal.NotaDao
import com.danibanan.notakulakan.data.lokal.NotaRepositoryImpl
import com.danibanan.notakulakan.data.remote.AppCheckInterceptor
import com.danibanan.notakulakan.data.remote.EkstraksiRepositoryImpl
import com.danibanan.notakulakan.data.remote.NotaApi
import com.danibanan.notakulakan.domain.repo.Analitik
import com.danibanan.notakulakan.domain.repo.EkstraksiRepository
import com.danibanan.notakulakan.domain.repo.Jam
import com.danibanan.notakulakan.domain.repo.NotaRepository
import com.danibanan.notakulakan.domain.repo.PenyiapFoto
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object ModulData {

    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @IoDispatcher
    fun io(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    fun jam(): Jam = Jam { System.currentTimeMillis() }

    @Provides
    @Singleton
    fun okHttp(appCheck: AppCheckInterceptor): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        // Backend memberi model 30 detik plus satu percobaan ulang; beri ruang di atasnya.
        .readTimeout(75, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .addInterceptor(appCheck)
        .apply { PengaturanVarian.interceptorTambahan().forEach { addInterceptor(it) } }
        .build()

    @Provides
    @Singleton
    fun retrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun notaApi(retrofit: Retrofit): NotaApi = retrofit.create(NotaApi::class.java)

    @Provides
    @Singleton
    fun basisdata(@ApplicationContext ctx: Context): BasisdataNota =
        Room.databaseBuilder(ctx, BasisdataNota::class.java, "nota.db").build()

    @Provides
    fun notaDao(db: BasisdataNota): NotaDao = db.notaDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ModulBinding {
    @Binds
    abstract fun ekstraksi(impl: EkstraksiRepositoryImpl): EkstraksiRepository

    @Binds
    abstract fun nota(impl: NotaRepositoryImpl): NotaRepository

    @Binds
    abstract fun penyiapFoto(impl: FotoKompresor): PenyiapFoto

    @Binds
    abstract fun analitik(impl: FirebaseAnalitik): Analitik
}
