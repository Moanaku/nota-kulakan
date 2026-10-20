package com.danibanan.notakulakan.katalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.danibanan.notakulakan.ui.komponen.BarAtas
import com.danibanan.notakulakan.ui.komponen.BarBawah
import com.danibanan.notakulakan.ui.komponen.BarisBarang
import com.danibanan.notakulakan.ui.komponen.BarisNota
import com.danibanan.notakulakan.ui.komponen.BarisRingkasanTotal
import com.danibanan.notakulakan.ui.komponen.ChipSelisihHarga
import com.danibanan.notakulakan.ui.komponen.ChipStatus
import com.danibanan.notakulakan.ui.komponen.DataKosong
import com.danibanan.notakulakan.ui.komponen.FotoNota
import com.danibanan.notakulakan.ui.komponen.JenisKosong
import com.danibanan.notakulakan.ui.komponen.JenisPesan
import com.danibanan.notakulakan.ui.komponen.KartuRingkasanNota
import com.danibanan.notakulakan.ui.komponen.KolomRupiah
import com.danibanan.notakulakan.ui.komponen.KolomTeks
import com.danibanan.notakulakan.ui.komponen.KondisiGalat
import com.danibanan.notakulakan.ui.komponen.PemisahBaris
import com.danibanan.notakulakan.ui.komponen.PesanPeriksa
import com.danibanan.notakulakan.ui.komponen.SheetKoreksi
import com.danibanan.notakulakan.ui.komponen.SheetPilihan
import com.danibanan.notakulakan.ui.komponen.SkeletonBarisBarang
import com.danibanan.notakulakan.ui.komponen.SpandukOffline
import com.danibanan.notakulakan.ui.komponen.StatusBaris
import com.danibanan.notakulakan.ui.komponen.Tab
import com.danibanan.notakulakan.ui.komponen.TombolCatatNota
import com.danibanan.notakulakan.ui.komponen.TombolSekunder
import com.danibanan.notakulakan.ui.komponen.TombolTeks
import com.danibanan.notakulakan.ui.komponen.TombolUtama
import com.danibanan.notakulakan.ui.theme.Spasi

// Teks terpanjang yang wajar untuk tiap label, supaya pemotongan dan pembungkusan kelihatan di sini.
private const val NAMA_PANJANG = "Minyak goreng kemasan pouch 2 liter isi 6 pcs merek lokal"
private const val QTY_HARGA_PANJANG = "12 karton isi 40 bungkus x Rp 123.456"
private const val TOKO_PANJANG = "UD Sumber Rejeki Makmur Jaya Abadi Grosir Sembako"
private const val JUMLAH_PANJANG = "Rp 12.345.678"

@Composable
fun KatalogKomponen() {
    var teks by remember { mutableStateOf("Gula pasir kristal putih kemasan 1 kg") }
    var rupiah by remember { mutableStateOf<Long?>(1234567) }
    var sheet by remember { mutableStateOf(false) }
    var sheetPilihan by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spasi.l, vertical = Spasi.l),
        verticalArrangement = Arrangement.spacedBy(Spasi.xl),
    ) {
        Text("Katalog komponen", style = MaterialTheme.typography.headlineMedium)

        Bagian("Tipografi") {
            Text("Angka besar Rp 12.345.678", style = MaterialTheme.typography.displaySmall)
            Text("Judul layar (Newsreader)", style = MaterialTheme.typography.headlineMedium)
            Text("Judul bagian (Newsreader)", style = MaterialTheme.typography.titleLarge)
            Text("Isi 16 sp: Gula pasir kristal putih", style = MaterialTheme.typography.bodyLarge)
            Text("Isi tebal 16 sp: Gula pasir", style = MaterialTheme.typography.titleMedium)
            Text("Keterangan 14 sp", style = MaterialTheme.typography.bodySmall)
            Text("Label kecil 12 sp", style = MaterialTheme.typography.labelSmall)
        }

        Bagian("Tombol") {
            TombolUtama("Simpan nota", onClick = {}, modifier = Modifier.fillMaxWidth())
            TombolUtama("Foto nota baru", onClick = {}, ikon = Icons.Outlined.Add, modifier = Modifier.fillMaxWidth())
            TombolUtama("Simpan nota (nonaktif)", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
            TombolSekunder("Ambil ulang foto nota", onClick = {}, modifier = Modifier.fillMaxWidth())
            TombolSekunder("Ambil ulang (nonaktif)", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
            TombolTeks("Lewati pemeriksaan hitungan nota", onClick = {})
        }

        Bagian("Kolom input") {
            KolomTeks(teks, { teks = it }, label = "Nama barang di nota")
            KolomTeks("", {}, label = "Nama barang di nota", bantuan = "Salin persis seperti tertulis di nota.")
            KolomTeks(
                "Gula",
                {},
                label = "Nama barang di nota",
                galat = "Nama barang belum lengkap. Tambahkan merek atau ukuran.",
            )
            KolomRupiah(rupiah, { rupiah = it }, label = "Harga satuan")
            KolomRupiah(
                null,
                {},
                label = "Harga satuan",
                galat = "Isi harga satuan dengan angka rupiah.",
            )
        }

        Bagian("Bilah judul (BarAtas)") {
            BarAtas("Catat nota")
            BarAtas("Periksa nota", onKembali = {})
        }

        Bagian("Tombol catat nota (TombolCatatNota)") {
            TombolCatatNota(onClick = {})
        }

        Bagian("Navigasi bawah (BarBawah)") {
            BarBawah(aktif = Tab.STOK, onStok = {}, onRiwayat = {})
            BarBawah(aktif = Tab.RIWAYAT, onStok = {}, onRiwayat = {})
        }

        Bagian("Chip status") {
            ChipStatus(StatusBaris.COCOK)
            ChipStatus(StatusBaris.SELISIH)
            ChipStatus(StatusBaris.BELUM_TERBACA)
            ChipStatus(StatusBaris.SELISIH, teks = "Selisih Rp 12.345.678 dari total di nota")
        }

        Bagian("Chip harga modal (ChipSelisihHarga)") {
            ChipSelisihHarga(selisih = null, adaRiwayat = false) // kondisi: produk baru, belum ada riwayat
            ChipSelisihHarga(selisih = 5_000, adaRiwayat = true) // kondisi: naik, perlu diperhatikan
            ChipSelisihHarga(selisih = -3_000, adaRiwayat = true) // kondisi: turun
            ChipSelisihHarga(selisih = 0, adaRiwayat = true) // kondisi: tetap
        }

        Bagian("Pesan pemeriksaan (PesanPeriksa)") {
            PesanPeriksa("Total nota cocok dengan jumlah barang.", JenisPesan.COCOK)
            PesanPeriksa("Total nota selisih Rp 12.345 dari jumlah barang.", JenisPesan.GALAT)
            PesanPeriksa("Qty belum terbaca; isi manual di lembar koreksi.", JenisPesan.INFO)
        }

        Bagian("Foto nota (FotoNota, tanpa foto)") {
            FotoNota(path = null) // kondisi: berkas foto tidak ada, tampil placeholder
        }

        Bagian("Baris barang (daftar = baris + divider)") {
            BarisBarang("Gula pasir 1 kg", "10 x Rp 14.500", "Rp 145.000", StatusBaris.COCOK)
            PemisahBaris()
            BarisBarang(NAMA_PANJANG, QTY_HARGA_PANJANG, JUMLAH_PANJANG, StatusBaris.SELISIH, onClick = {})
            PemisahBaris()
            BarisBarang("Kecap manis", "Jumlah belum terbaca", "Rp 0", StatusBaris.BELUM_TERBACA, onClick = {})
        }

        Bagian("Baris ringkasan total") {
            BarisRingkasanTotal("Subtotal", "Rp 1.234.500")
            BarisRingkasanTotal("Diskon", "-Rp 34.500")
            BarisRingkasanTotal("PPN termasuk", JUMLAH_PANJANG)
            PemisahBaris()
            BarisRingkasanTotal("Total nota", JUMLAH_PANJANG, dominan = true)
        }

        Bagian("Baris nota (daftar riwayat = baris + divider)") {
            BarisNota("Toko Makmur", "20 Okt 2026, 09.15", "12 barang", "Rp 1.234.500")
            PemisahBaris()
            BarisNota(TOKO_PANJANG, "20 Okt 2026, 09.15", "128 barang", JUMLAH_PANJANG, onClick = {})
            PemisahBaris()
        }

        Bagian("Kartu ringkasan nota (satu objek berdiri sendiri, mis. Nota terakhir di Stok)") {
            KartuRingkasanNota("Toko Makmur", "20 Okt 2026, 09.15", "12 barang", "Rp 1.234.500", StatusBaris.COCOK)
            KartuRingkasanNota(
                TOKO_PANJANG,
                "20 Okt 2026, 09.15",
                "128 barang",
                JUMLAH_PANJANG,
                StatusBaris.SELISIH,
                onClick = {},
            )
        }

        Bagian("Sheet koreksi") {
            TombolSekunder("Buka lembar koreksi barang", onClick = { sheet = true }, modifier = Modifier.fillMaxWidth())
        }

        Bagian("Sheet pilihan (SheetPilihan)") {
            TombolSekunder("Buka pilihan produk", onClick = { sheetPilihan = true }, modifier = Modifier.fillMaxWidth())
        }

        Bagian("Kondisi: kosong (belum ada data)") {
            DataKosong(JenisKosong.BELUM_ADA_DATA, objek = "nota", teksAksi = "Foto nota pertama", onAksi = {})
        }
        Bagian("Kondisi: kosong (hasil filter)") {
            DataKosong(JenisKosong.HASIL_FILTER_KOSONG, objek = "nota", onAksi = {})
        }
        Bagian("Kondisi: skeleton") { SkeletonBarisBarang() }
        Bagian("Kondisi: galat") {
            KondisiGalat("Server tidak bisa dihubungi. Periksa sambungan internet, lalu coba lagi.", onCobaLagi = {})
            KondisiGalat(
                "Foto ini tidak terbaca sebagai nota. Coba foto ulang dengan cahaya lebih terang.",
                onCobaLagi = {},
                judul = "Nota belum terbaca",
                teksAksi = "Foto ulang",
            ) // kondisi: foto salah, aksi memilih foto lain (bukan mengulang panggilan)
        }
        Bagian("Spanduk offline") { SpandukOffline(tampil = true) }
    }

    if (sheet) {
        SheetKoreksi(
            judul = "Koreksi barang",
            teksSimpan = "Simpan koreksi",
            teksBatal = "Batal",
            onSimpan = { sheet = false },
            onTutup = { sheet = false },
        ) {
            KolomTeks(teks, { teks = it }, label = "Nama barang di nota")
            KolomRupiah(rupiah, { rupiah = it }, label = "Harga satuan")
        }
    }

    if (sheetPilihan) {
        SheetPilihan(judul = "Masuk ke produk apa?", onTutup = { sheetPilihan = false }) {
            Text("Tertulis di nota: Gula Pasir Kristal 1 kg", style = MaterialTheme.typography.bodySmall)
            KolomTeks("", {}, label = "Cari produk")
            Text("Buat produk baru: Gula Pasir Kristal 1 kg", style = MaterialTheme.typography.titleMedium)
            PemisahBaris()
            Text("Gula pasir kristal 1 kg", style = MaterialTheme.typography.bodyLarge)
            PemisahBaris()
            Text("Gula pasir halus 500 g", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun Bagian(judul: String, isi: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spasi.s)) {
        Text(judul, style = MaterialTheme.typography.titleLarge)
        PemisahBaris()
        isi()
    }
}
