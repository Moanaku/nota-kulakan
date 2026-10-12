package com.danibanan.notakulakan.ui.foto

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.danibanan.notakulakan.R
import com.danibanan.notakulakan.ui.komponen.Ikon
import java.io.File
import java.util.UUID

/**
 * Kamera lewat aplikasi kamera sistem (TakePicture) dan galeri lewat Photo Picker. Keduanya
 * tidak butuh izin CAMERA atau izin penyimpanan, sehingga tidak ada dialog izin yang membingungkan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FotoScreen(
    onKembali: () -> Unit,
    onFotoSiap: (String) -> Unit,
    onIsiManual: () -> Unit,
) {
    val ctx = LocalContext.current
    var uriKamera by rememberSaveable { mutableStateOf<String?>(null) }

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
            kamera.launch(uri)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(ctx, R.string.foto_tidak_ada_kamera, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.catat_nota)) },
                navigationIcon = {
                    IconButton(onClick = onKembali) { Ikon(R.drawable.ic_kembali, stringResource(R.string.kembali)) }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Text(stringResource(R.string.foto_judul), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.foto_isi),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(18.dp)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Ikon(R.drawable.ic_nota, null, Modifier.size(40.dp), warna = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.foto_bingkai),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(16.dp))
            for (tips in listOf(R.string.foto_tips_1, R.string.foto_tips_2, R.string.foto_tips_3)) {
                Text("•  " + stringResource(tips), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 3.dp))
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = ::bukaKamera, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Ikon(R.drawable.ic_kamera, null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.foto_ambil))
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { galeri.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Ikon(R.drawable.ic_galeri, null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.foto_galeri))
            }
            TextButton(onClick = onIsiManual, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.foto_manual))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.foto_privasi),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
