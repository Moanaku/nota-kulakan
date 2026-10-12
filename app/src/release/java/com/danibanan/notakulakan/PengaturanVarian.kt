package com.danibanan.notakulakan

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import okhttp3.Interceptor

/** Versi RILIS: App Check memakai Play Integrity, tanpa log jaringan. */
object PengaturanVarian {
    fun pasangAppCheck() {
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
    }

    fun interceptorTambahan(): List<Interceptor> = emptyList()
}
