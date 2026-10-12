package com.danibanan.notakulakan

import android.app.Application
import com.danibanan.notakulakan.data.foto.bersihkanCacheFoto
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NotaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // FirebaseApp hanya ada bila google-services.json dipasang saat build.
        if (FirebaseApp.getApps(this).isNotEmpty()) {
            PengaturanVarian.pasangAppCheck()
            // Data crash dan analitik dari build debug tidak dikirim, agar angka closed test bersih.
            FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
            FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(!BuildConfig.DEBUG)
        }
        bersihkanCacheFoto(this)
    }
}
