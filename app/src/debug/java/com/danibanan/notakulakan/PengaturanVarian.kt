package com.danibanan.notakulakan

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Versi DEBUG. Provider App Check debug mencetak token debug di Logcat (cari "DebugAppCheckProvider");
 * daftarkan token itu di Firebase Console > App Check > Manage debug tokens.
 * Pasangan versi rilis ada di src/release dengan nama dan fungsi yang sama.
 */
object PengaturanVarian {
    fun pasangAppCheck() {
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
    }

    // BASIC: hanya baris permintaan dan status, tidak pernah isi foto atau header token.
    fun interceptorTambahan(): List<Interceptor> =
        listOf(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
}
