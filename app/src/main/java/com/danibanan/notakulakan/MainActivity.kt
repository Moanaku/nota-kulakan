package com.danibanan.notakulakan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.danibanan.notakulakan.ui.nav.NavigasiNota
import com.danibanan.notakulakan.ui.tema.TemaNota
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TemaNota {
                NavigasiNota()
            }
        }
    }
}
