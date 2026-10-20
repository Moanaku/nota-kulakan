package com.danibanan.notakulakan.ui.nav

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.danibanan.notakulakan.ui.foto.FotoScreen
import com.danibanan.notakulakan.ui.hasil.HasilScreen
import com.danibanan.notakulakan.ui.riwayat.DetailNotaScreen
import com.danibanan.notakulakan.ui.riwayat.RiwayatScreen
import com.danibanan.notakulakan.ui.stok.StokScreen
import com.danibanan.notakulakan.ui.theme.Durasi
import com.danibanan.notakulakan.ui.theme.spekGerak
import kotlinx.serialization.Serializable

// Rute bertipe (Navigation 2.8+). Nama properti = kunci di SavedStateHandle ("uri", "id").
@Serializable object RuteStok
@Serializable object RuteRiwayat
@Serializable object RuteFoto
@Serializable data class RuteHasil(val uri: String? = null)
@Serializable data class RuteDetail(val id: Long)

@Composable
fun NavigasiNota(nav: NavHostController = rememberNavController()) {
    // Transisi antarlayar: pudar dengan easing dan durasi token (0 ms bila animasi dimatikan di sistem).
    val masuk = fadeIn(spekGerak(Durasi.sedang))
    val keluar = fadeOut(spekGerak(Durasi.cepat))
    NavHost(
        navController = nav,
        startDestination = RuteStok,
        enterTransition = { masuk },
        exitTransition = { keluar },
        popEnterTransition = { masuk },
        popExitTransition = { keluar },
    ) {
        composable<RuteStok> {
            StokScreen(
                onCatatNota = { nav.navigate(RuteFoto) },
                onKeRiwayat = { nav.keTab(RuteRiwayat) },
            )
        }
        composable<RuteRiwayat> {
            RiwayatScreen(
                onBukaNota = { id -> nav.navigate(RuteDetail(id)) },
                onKeStok = { nav.keTab(RuteStok) },
                onCatatNota = { nav.navigate(RuteFoto) },
            )
        }
        composable<RuteFoto> {
            FotoScreen(
                onKembali = { nav.popBackStack() },
                onFotoSiap = { uri ->
                    nav.navigate(RuteHasil(uri)) { popUpTo<RuteFoto> { inclusive = true } }
                },
                onIsiManual = {
                    nav.navigate(RuteHasil(null)) { popUpTo<RuteFoto> { inclusive = true } }
                },
            )
        }
        composable<RuteHasil> {
            HasilScreen(
                onKembali = { nav.popBackStack() },
                onGantiFoto = { nav.navigate(RuteFoto) { popUpTo<RuteHasil> { inclusive = true } } },
                onLihatStok = { nav.popBackStack(RuteStok, inclusive = false) },
                onCatatLagi = { nav.navigate(RuteFoto) { popUpTo<RuteHasil> { inclusive = true } } },
            )
        }
        composable<RuteDetail> {
            DetailNotaScreen(onKembali = { nav.popBackStack() })
        }
    }
}

/** Pindah tab bawah tanpa menumpuk back stack. */
private fun NavHostController.keTab(rute: Any) {
    navigate(rute) {
        popUpTo(RuteStok) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
