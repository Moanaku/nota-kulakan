package com.danibanan.notakulakan.katalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.danibanan.notakulakan.ui.theme.NotaTheme

/**
 * Hanya source set debug: tidak ada di APK rilis dan tidak terhubung ke navigasi aplikasi.
 * Tanpa ekstra tampil katalog komponen; dengan `--es layar <nama>` tampil satu layar sungguhan dengan data palsu
 * (lihat [PratinjauLayar]) supaya kondisi yang sulit dipicu di HP (kosong, galat, skeleton) bisa dipotret.
 */
class KatalogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val layar = intent.getStringExtra("layar")
        setContent { NotaTheme { if (layar == null) KatalogKomponen() else PratinjauLayar(layar) } }
    }
}
