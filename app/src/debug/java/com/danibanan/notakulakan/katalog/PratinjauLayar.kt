package com.danibanan.notakulakan.katalog

import androidx.compose.runtime.Composable
import com.danibanan.notakulakan.domain.repo.JenisGagal
import com.danibanan.notakulakan.ui.foto.FotoScreen
import com.danibanan.notakulakan.ui.hasil.HasilUi
import com.danibanan.notakulakan.ui.hasil.LayarGagal
import com.danibanan.notakulakan.ui.hasil.LayarMemproses

/**
 * Layar sungguhan dengan data palsu untuk potret verifikasi. Nama dipakai di `adb shell am start ... --es layar <nama>`:
 * foto-server, foto-contoh, memproses, gagal-<jaringan|tak-terjangkau|waktu|sibuk|bukan-nota|foto|besar|ditolak|server>.
 */
@Composable
fun PratinjauLayar(nama: String) {
    when {
        nama == "foto-server" -> FotoScreen({}, {}, {}, {}, punyaServer = true)
        nama == "foto-contoh" -> FotoScreen({}, {}, {}, {}, punyaServer = false)
        nama == "memproses" -> LayarMemproses(onKembali = {})
        nama.startsWith("gagal-") -> {
            val (jenis, tersambung) = when (nama.removePrefix("gagal-")) {
                "jaringan" -> JenisGagal.TANPA_JARINGAN to false
                "tak-terjangkau" -> JenisGagal.TANPA_JARINGAN to true
                "waktu" -> JenisGagal.WAKTU_HABIS to true
                "sibuk" -> JenisGagal.LAYANAN_SIBUK to true
                "bukan-nota" -> JenisGagal.BUKAN_NOTA to true
                "foto" -> JenisGagal.FOTO_TIDAK_VALID to true
                "besar" -> JenisGagal.FOTO_TERLALU_BESAR to true
                "ditolak" -> JenisGagal.APLIKASI_DITOLAK to true
                else -> JenisGagal.GANGGUAN_SERVER to true
            }
            val bisaUlang = jenis !in setOf(JenisGagal.BUKAN_NOTA, JenisGagal.FOTO_TIDAK_VALID, JenisGagal.FOTO_TERLALU_BESAR)
            LayarGagal(HasilUi.Gagal(jenis, bisaUlang), {}, {}, {}, {}, tersambung = tersambung)
        }
    }
}
