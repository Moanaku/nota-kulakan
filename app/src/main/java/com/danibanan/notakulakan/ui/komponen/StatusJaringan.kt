package com.danibanan.notakulakan.ui.komponen

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

private fun ConnectivityManager.tersambung(): Boolean =
    getNetworkCapabilities(activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

/**
 * True bila HP punya jaringan yang bisa ke internet. Dipakai hanya untuk menampilkan [SpandukOffline];
 * hasilnya tidak mengubah apa yang boleh dilakukan pengguna.
 */
@Composable
fun rememberTersambung(): Boolean {
    val konteks = LocalContext.current
    val cm = remember(konteks) { konteks.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager }
    var tersambung by remember { mutableStateOf(cm?.tersambung() ?: true) }
    DisposableEffect(cm) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { tersambung = true }
            override fun onLost(network: Network) { tersambung = cm?.tersambung() ?: true }
        }
        cm?.registerDefaultNetworkCallback(callback)
        onDispose { cm?.unregisterNetworkCallback(callback) }
    }
    return tersambung
}
