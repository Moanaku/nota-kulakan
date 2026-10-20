package com.danibanan.notakulakan.katalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.danibanan.notakulakan.ui.theme.NotaTheme

/** Hanya source set debug: tidak ada di APK rilis dan tidak terhubung ke navigasi aplikasi. */
class KatalogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { NotaTheme { KatalogKomponen() } }
    }
}
