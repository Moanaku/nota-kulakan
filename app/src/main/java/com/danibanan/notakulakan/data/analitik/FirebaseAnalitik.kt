package com.danibanan.notakulakan.data.analitik

import android.content.Context
import android.os.Bundle
import com.danibanan.notakulakan.domain.repo.Analitik
import com.danibanan.notakulakan.domain.repo.JenisGagal
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Peristiwa ini adalah alat ukur hipotesis nilai di PRD (waktu per nota, berapa nota perlu
 * dikoreksi). Isinya hanya angka dan status. Tanpa Firebase, semua panggilan diabaikan.
 */
@Singleton
class FirebaseAnalitik @Inject constructor(@ApplicationContext ctx: Context) : Analitik {

    private val fa: FirebaseAnalytics? =
        if (FirebaseApp.getApps(ctx).isNotEmpty()) FirebaseAnalytics.getInstance(ctx) else null

    override fun ekstraksiSelesai(lolos: Boolean, jumlahBaris: Int, latensiMs: Long, model: String) {
        fa?.logEvent(
            "nota_diekstrak",
            Bundle().apply {
                putString("lolos", lolos.toString())
                putLong("jumlah_baris", jumlahBaris.toLong())
                putLong("latensi_ms", latensiMs)
                putString("model", model.take(100))
            },
        )
    }

    override fun ekstraksiGagal(jenis: JenisGagal) {
        fa?.logEvent("nota_gagal", Bundle().apply { putString("jenis", jenis.name.lowercase()) })
    }

    override fun notaDisimpan(
        jumlahBaris: Int,
        barisDikoreksi: Int,
        dikonfirmasi: Boolean,
        durasiDetik: Long,
        manual: Boolean,
    ) {
        fa?.logEvent(
            "nota_disimpan",
            Bundle().apply {
                putLong("jumlah_baris", jumlahBaris.toLong())
                putLong("baris_dikoreksi", barisDikoreksi.toLong())
                putString("dikonfirmasi", dikonfirmasi.toString())
                putLong("durasi_detik", durasiDetik)
                putString("manual", manual.toString())
            },
        )
    }
}
