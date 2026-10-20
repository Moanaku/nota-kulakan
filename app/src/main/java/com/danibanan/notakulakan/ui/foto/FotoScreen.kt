package com.danibanan.notakulakan.ui.foto

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.FileProvider
import com.danibanan.notakulakan.R
import com.danibanan.notakulakan.ui.komponen.BarAtas
import com.danibanan.notakulakan.ui.komponen.JenisPesan
import com.danibanan.notakulakan.ui.komponen.PesanPeriksa
import com.danibanan.notakulakan.ui.komponen.SpandukOffline
import com.danibanan.notakulakan.ui.komponen.TombolSekunder
import com.danibanan.notakulakan.ui.komponen.TombolTeks
import com.danibanan.notakulakan.ui.komponen.TombolUtama
import com.danibanan.notakulakan.ui.komponen.rememberTersambung
import com.danibanan.notakulakan.ui.theme.Radius
import com.danibanan.notakulakan.ui.theme.Spasi
import com.danibanan.notakulakan.ui.theme.Ukuran
import java.io.File
import java.util.UUID

/**
 * Kamera lewat aplikasi kamera sistem (TakePicture) dan galeri lewat Photo Picker. Keduanya
 * tidak butuh izin CAMERA atau izin penyimpanan, sehingga tidak ada dialog izin yang membingungkan.
 *
 * Layar ini tidak memuat data, jadi kondisi kosong dan memuat tidak berlaku. Kondisi yang ada: offline
 * (spanduk; foto tetap bisa diambil, tetapi nota belum bisa dibaca) dan galat (tidak ada aplikasi kamera).
 */
@Composable
fun FotoScreen(
    onKembali: () -> Unit,
    onFotoSiap: (String) -> Unit,
    onIsiManual: () -> Unit,
) {
    val ctx = LocalContext.current
    var uriKamera by rememberSaveable { mutableStateOf<String?>(null) }
    var kameraTidakAda by rememberSaveable { mutableStateOf(false) }
    val tersambung = rememberTersambung()

    val kamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { berhasil ->
        val uri = uriKamera
        if (berhasil && uri != null) onFotoSiap(uri)
    }
    val galeri = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onFotoSiap(uri.toString())
    }

    fun bukaKamera() {
        val berkas = File(ctx.cacheDir, "foto/${UUID.randomUUID()}.jpg").apply { parentFile?.mkdirs() }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", berkas)
        uriKamera = uri.toString()
        try {
            kameraTidakAda = false
            kamera.launch(uri)
        } catch (e: ActivityNotFoundException) {
            kameraTidakAda = true
        }
    }

    Scaffold(topBar = { BarAtas(stringResource(R.string.catat_nota), onKembali = onKembali) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SpandukOffline(tampil = !tersambung)
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spasi.l, vertical = Spasi.s),
                verticalArrangement = Arrangement.spacedBy(Spasi.s),
            ) {
                Text(
                    stringResource(R.string.foto_judul),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(R.string.foto_isi),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    Modifier
                        .padding(vertical = Spasi.s)
                        .fillMaxWidth()
                        .height(Spasi.xxxl * 4)
                        .border(BorderStroke(Ukuran.garis, MaterialTheme.colorScheme.outline), RoundedCornerShape(Radius.besar))
                        .padding(Spasi.l),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.foto_bingkai),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                for (tips in listOf(R.string.foto_tips_1, R.string.foto_tips_2, R.string.foto_tips_3)) {
                    Text(
                        "•  " + stringResource(tips),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (kameraTidakAda) {
                    PesanPeriksa(stringResource(R.string.foto_tidak_ada_kamera), JenisPesan.GALAT, Modifier.padding(top = Spasi.s))
                }
                TombolUtama(
                    stringResource(R.string.foto_ambil),
                    onClick = ::bukaKamera,
                    modifier = Modifier.fillMaxWidth().padding(top = Spasi.m),
                )
                TombolSekunder(
                    stringResource(R.string.foto_galeri),
                    onClick = { galeri.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                TombolTeks(stringResource(R.string.foto_manual), onClick = onIsiManual, modifier = Modifier.fillMaxWidth())
                Text(
                    stringResource(R.string.foto_privasi),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spasi.xl),
                )
            }
        }
    }
}
