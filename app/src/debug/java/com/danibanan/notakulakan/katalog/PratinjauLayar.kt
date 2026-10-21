package com.danibanan.notakulakan.katalog

import androidx.compose.runtime.Composable
import com.danibanan.notakulakan.domain.RingkasNota
import com.danibanan.notakulakan.domain.model.ItemBaris
import com.danibanan.notakulakan.domain.model.Nota
import com.danibanan.notakulakan.domain.repo.HasilSimpan
import com.danibanan.notakulakan.domain.repo.NotaTersimpan
import com.danibanan.notakulakan.domain.repo.PerubahanTersimpan
import com.danibanan.notakulakan.ui.hasil.LayarTersimpan
import com.danibanan.notakulakan.ui.riwayat.DetailLayar
import com.danibanan.notakulakan.ui.riwayat.DetailUi
import com.danibanan.notakulakan.ui.riwayat.DialogHapusContoh
import com.danibanan.notakulakan.ui.riwayat.DialogHapusNota
import com.danibanan.notakulakan.ui.riwayat.RiwayatLayar
import com.danibanan.notakulakan.ui.riwayat.RiwayatUi
import java.math.BigDecimal
import com.danibanan.notakulakan.domain.repo.JenisGagal
import com.danibanan.notakulakan.ui.foto.FotoScreen
import com.danibanan.notakulakan.ui.hasil.HasilUi
import com.danibanan.notakulakan.ui.hasil.LayarGagal
import com.danibanan.notakulakan.ui.hasil.LayarMemproses

/**
 * Layar sungguhan dengan data palsu untuk potret verifikasi. Nama dipakai di `adb shell am start ... --es layar <nama>`:
 * foto-server, foto-contoh, memproses, gagal-<jaringan|tak-terjangkau|waktu|sibuk|bukan-nota|foto|besar|ditolak|server>,
 * tersimpan, tersimpan-tanpa-perubahan, riwayat-<memuat|galat|kosong|ada|ada-contoh|offline>, dialog-hapus-contoh,
 * detail-<memuat|galat|ada|ada-konfirmasi>, dialog-hapus-nota.
 */

private val WAKTU = 1_792_000_000_000L // Okt 2026
private const val SUPPLIER_PANJANG = "UD Sumber Rezeki Makmur Sejahtera Abadi"

private fun ringkas(id: Long, supplier: String?, total: Long?, baris: Int) =
    RingkasNota(id = id, total = total, jumlahBaris = baris, dibuatPada = WAKTU - id * 86_400_000L, supplier = supplier)

private val DAFTAR_RIWAYAT = listOf(
    ringkas(4, SUPPLIER_PANJANG, 895_000, 4),
    ringkas(3, "Grosir Contoh Makmur", 422_000, 3),
    ringkas(2, "Toko Sinar Pagi", 1_250_500, 12),
    ringkas(1, null, null, 1),
)

private val NOTA_DETAIL = NotaTersimpan(
    id = 4,
    nota = Nota(
        items = listOf(
            ItemBaris("Minyak Goreng Kemasan Pouch 2 Liter Isi 12", BigDecimal("2"), "dus", 110_000, 220_000),
            ItemBaris("Gula Pasir 1 kg", BigDecimal("10"), "kg", 14_500, 145_000),
            ItemBaris("Kecap Manis", null, null, null, null),
        ),
        total = 365_000,
        supplier = SUPPLIER_PANJANG,
    ),
    namaProduk = listOf("Minyak Goreng 2 L", "Gula Pasir 1 kg", "Kecap Manis"),
    dibuatPada = WAKTU,
    fotoPath = null,
    dikonfirmasiMeskiTidakCocok = false,
)

@Composable
fun PratinjauLayar(nama: String) {
    when {
        nama == "foto-server" -> FotoScreen({}, {}, {}, {}, punyaServer = true)
        nama == "foto-contoh" -> FotoScreen({}, {}, {}, {}, punyaServer = false)
        nama == "memproses" -> LayarMemproses(onKembali = {})
        nama == "tersimpan" || nama == "tersimpan-tanpa-perubahan" -> LayarTersimpan(
            HasilUi.Tersimpan(
                HasilSimpan(
                    1,
                    if (nama == "tersimpan") {
                        listOf(
                            PerubahanTersimpan("Minyak Goreng 2 L", "dus", 112_000, 110_000),
                            PerubahanTersimpan("Gula Pasir 1 kg", "kg", 14_000, 14_500),
                            PerubahanTersimpan("Kecap Manis", "botol", 9_000, 9_000),
                            PerubahanTersimpan("Kopi Sachet", "renceng", 28_000, null),
                        )
                    } else {
                        emptyList()
                    },
                ),
                jumlahBaris = 4,
                total = 895_000,
            ),
            {}, {}, {},
        )
        nama.startsWith("riwayat-") || nama == "dialog-hapus-contoh" -> {
            val v = nama.removePrefix("riwayat-")
            val ui = when (v) {
                "memuat" -> RiwayatUi.Memuat
                "galat" -> RiwayatUi.Galat
                "kosong" -> RiwayatUi.Ada(emptyList())
                "ada-contoh", "dialog-hapus-contoh" ->
                    RiwayatUi.Ada(listOf(ringkas(5, "(Contoh) Grosir Contoh Makmur", 422_000, 3)) + DAFTAR_RIWAYAT)
                else -> RiwayatUi.Ada(DAFTAR_RIWAYAT)
            }
            RiwayatLayar(ui, galatHapusContoh = false, tersambung = v != "offline", {}, {}, {}, {}, {})
            if (nama == "dialog-hapus-contoh") DialogHapusContoh({}, {})
        }
        nama.startsWith("detail-") || nama == "dialog-hapus-nota" -> {
            val ui = when (nama.removePrefix("detail-")) {
                "memuat" -> DetailUi.Memuat
                "galat" -> DetailUi.Galat
                "ada-konfirmasi" -> DetailUi.Ada(NOTA_DETAIL.copy(dikonfirmasiMeskiTidakCocok = true))
                else -> DetailUi.Ada(NOTA_DETAIL)
            }
            DetailLayar(ui, {}, {}, {})
            if (nama == "dialog-hapus-nota") DialogHapusNota({}, {})
        }
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
