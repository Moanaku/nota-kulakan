package com.danibanan.notakulakan.katalog

import androidx.compose.runtime.Composable
import com.danibanan.notakulakan.domain.RentangTanggal
import com.danibanan.notakulakan.domain.RingkasNota
import com.danibanan.notakulakan.domain.model.ItemBaris
import com.danibanan.notakulakan.domain.model.Nota
import com.danibanan.notakulakan.domain.repo.HasilSimpan
import com.danibanan.notakulakan.domain.repo.NotaTersimpan
import com.danibanan.notakulakan.domain.repo.PerubahanTersimpan
import com.danibanan.notakulakan.ui.hasil.DialogDuplikat
import com.danibanan.notakulakan.ui.hasil.LayarTersimpan
import com.danibanan.notakulakan.domain.repo.StokProduk
import com.danibanan.notakulakan.ui.riwayat.DetailLayar
import com.danibanan.notakulakan.ui.stok.StokLayar
import com.danibanan.notakulakan.ui.stok.StokUi
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
 * tersimpan, tersimpan-tanpa-perubahan, duplikat (data palsu, tidak menyimpan apa pun), riwayat-<memuat|galat|kosong|ada|ada-contoh|offline>, riwayat-cari-<kosong-kolom|hasil|kosong|filter|filter-kosong|lembar>, dialog-hapus-contoh,
 * detail-<memuat|galat|ada|ada-konfirmasi>, dialog-hapus-nota, stok-<memuat|galat|kosong|ada|ada-panjang|offline>.
 */

private val WAKTU = 1_792_000_000_000L // Okt 2026
private const val SUPPLIER_PANJANG = "UD Sumber Rezeki Makmur Sejahtera Abadi"

private fun ringkas(id: Long, supplier: String?, total: Long?, baris: Int, vararg barang: String) =
    RingkasNota(
        id = id,
        total = total,
        jumlahBaris = baris,
        dibuatPada = WAKTU - (10 - id) * 86_400_000L, // id besar = lebih baru, sesuai urutan daftar
        supplier = supplier,
        namaBarang = barang.toList(),
    )

// Nota sintetis; nama barang dipakai untuk memotret pencarian ("susu" menemukan nota 3 dan 2 lewat barang).
private val DAFTAR_RIWAYAT = listOf(
    ringkas(4, SUPPLIER_PANJANG, 895_000, 4, "Minyak Goreng Pouch 2 L", "Gula Pasir 1 kg", "Kecap Manis", "Teh Celup"),
    ringkas(3, "Grosir Contoh Makmur", 422_000, 3, "Gula Pasir 1 kg", "Kopi Sachet", "Susu Kental Manis"),
    ringkas(2, "Toko Sinar Pagi", 1_250_500, 12, "Susu UHT 1 L", "Susu Bubuk 400 g", "Susu Kedelai", "Mi Instan"),
    ringkas(1, null, null, 1, "Kecap Manis"),
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

private fun stok(nama: String, satuan: String?, masuk: String, harga: Long?, selisih: Long?) =
    StokProduk(produkId = nama.hashCode().toLong(), nama = nama, satuan = satuan, totalMasuk = BigDecimal(masuk), hargaTerakhir = harga, selisihHarga = selisih)

private val DAFTAR_STOK = listOf(
    stok("Air Mineral 600ml", "dus", "9", 45_000, 0),
    stok("Golda", "botol", "2", 3_500, null),
    stok("Minyak Goreng Kemasan Pouch 2 Liter Isi 12 Merek Lokal", "dus", "24", 112_000, 2_000),
    stok("Gula Pasir 1 kg", "kg", "120", 14_000, -500),
    stok("Kecap Manis", null, "6", null, null),
)

@Composable
fun PratinjauLayar(nama: String) {
    when {
        nama == "foto-server" -> FotoScreen({}, {}, {}, {}, punyaServer = true)
        nama == "foto-contoh" -> FotoScreen({}, {}, {}, {}, punyaServer = false)
        nama == "memproses" -> LayarMemproses(onKembali = {})
        nama == "duplikat" -> {
            // Latar: layar Memproses supaya dialog tampil di atas isi sungguhan; tidak ada nota yang disimpan.
            LayarMemproses(onKembali = {})
            DialogDuplikat(ringkas(4, SUPPLIER_PANJANG, 895_000, 4), onTetapSimpan = {}, onBatal = {})
        }
        nama.startsWith("stok-") -> {
            val v = nama.removePrefix("stok-")
            val ui = when (v) {
                "memuat" -> StokUi.Memuat
                "galat" -> StokUi.Galat
                "kosong" -> StokUi.Ada(emptyList(), null)
                else -> StokUi.Ada(DAFTAR_STOK, DAFTAR_RIWAYAT.first())
            }
            StokLayar(ui, tersambung = v != "offline", onCatatNota = {}, onKeRiwayat = {}, onCobaLagi = {})
        }
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
        nama.startsWith("riwayat-cari-") -> {
            val v = nama.removePrefix("riwayat-cari-")
            RiwayatLayar(
                RiwayatUi.Ada(DAFTAR_RIWAYAT), galatHapusContoh = false, tersambung = true, {}, {}, {}, {}, {},
                kataAwal = when (v) {
                    "hasil" -> "susu"
                    "kosong" -> "sabun"
                    "filter-kosong" -> "sabun"
                    else -> ""
                },
                supplierAwal = if (v == "filter" || v == "lembar") "Toko Sinar Pagi" else null,
                rentangAwal = if (v == "filter" || v == "lembar" || v == "filter-kosong") RentangTanggal.BULAN_INI else RentangTanggal.SEMUA,
                filterTerbukaAwal = v == "lembar",
            )
        }
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
