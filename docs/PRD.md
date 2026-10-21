# PRD: Nota Kulakan (Lapisan 1)

Status: draf untuk ditinjau Dani. Yang bertanda **(HIPOTESIS)** belum diukur dan tidak boleh ditulis sebagai fakta di README.

## 1. Masalah dan pengguna

- **Pengguna:** pemilik toko sembako (toko pengguna), memakai HP Android.
- **Cara sekarang:** nota dari grosir atau sales (cetak dan tulisan tangan) dicatat tangan di buku, dan kertasnya disimpan di laci. Stok dan harga modal tidak dihitung dari nota itu.
- **Masalah:** perubahan harga modal tidak terlihat, dan jumlah barang yang masuk tidak tercatat.
- **Posisi project:** studi kasus produk dan rekayasa, bukan pesaing aplikasi kasir.

## 2. Ukuran nilai (semua HIPOTESIS sampai diukur)

| Ukuran | Cara mengukur | Pengukur |
|---|---|---|
| Waktu per nota | Stopwatch 20 nota: input manual vs lewat aplikasi (termasuk koreksi) | Dani |
| Tingkat kesalahan | Baris salah per 100 baris: ketik manual vs hasil aplikasi setelah koreksi | Dani |
| Ketukan per nota | Hitung ketukan layar per nota | Dani |
| Biaya AI per nota | Token input dan output dari respons API x harga model, catat tanggal harga | otomatis di backend |
| Latensi dan data | Waktu dari foto sampai hasil; ukuran unggahan per foto | otomatis |

Biaya AI per nota harus masuk akal dibanding waktu yang dihemat. Kalau tidak, arsitektur diubah, dan perubahan itu menjadi bahan studi kasus.

## 3. Lingkup

**Masuk Lapisan 1:** F1 sampai F5 di bawah.

**Di luar Lapisan 1** (tidak dikerjakan sebelum Lapisan 1 berjalan, terukur, terdokumentasi):
peringatan margin, konversi satuan (dus ke pcs), OCR di HP, scan barcode, batch dan tanggal kedaluwarsa, payment gateway, FCM, akun pengguna, sinkronisasi cloud. Stok disimpan lokal saja.

## 4. Spesifikasi fitur

### F1. Ambil foto nota
- Kamera sistem dan pemilih foto. CameraX tidak dipakai di Lapisan 1.
- Foto dikecilkan dan dikompres sebelum unggah. Sisi terpanjang dan kualitas JPEG adalah parameter konfigurasi, dan nilainya diputuskan dari evaluasi (akurasi vs ukuran data).
- Selesai kalau: foto dari kamera maupun galeri sampai ke F2 dengan ukuran terukur.

### F2. Ekstraksi
- Kirim foto ke backend (`POST /v1/ekstrak`, header `X-Firebase-AppCheck`). Terima `Nota` dan daftar isu validasi.
- Status layar: memuat, berhasil, perlu koreksi, gagal.
- Kegagalan yang ditangani: timeout, tanpa jaringan, JSON dari AI tidak sesuai skema, foto tidak terbaca. Tiap kegagalan punya pesan dan jalan keluar (ulangi, ganti foto, atau isi manual).
- Tidak ada percobaan ulang otomatis tanpa batas.
- Selesai kalau: semua jenis kegagalan di atas bisa dipicu dan ditampilkan dengan benar.

### F3. Periksa dan koreksi
- Baris yang punya isu disorot, dengan pesan isu dari validator (contoh: "2 × Rp 60 = Rp 120, tapi tertulis Rp 120.000").
- Pengguna bisa mengedit nama, qty, satuan, harga satuan, dan jumlah, serta menambah dan menghapus baris.
- Setelah edit, status baris dan total diperbarui seketika (validator dicerminkan di Kotlin).
- Aturan simpan (`statusSimpan`): data tidak lengkap (kosong, qty tidak sah, angka tak terbaca,
  nama kosong) **terhalang**; ketidakcocokan hitungan boleh disimpan setelah pengguna mencentang
  bahwa angka sudah sesuai nota fisik, dan nota itu ditandai di riwayat.
- Foto nota bisa dibuka di layar yang sama untuk dicocokkan.
- Selesai kalau: nota dengan kesalahan buatan bisa diperbaiki sampai lolos tanpa mengetik ulang baris yang benar.

### F4. Simpan ke stok
- Room, offline-first. Tabel: produk, nota, baris nota (stok masuk).
- Pencocokan nama ke produk dilakukan pengguna (pilih dari daftar atau buat baru) dan diingat sebagai
  alias (nama dinormalkan, pencocokan persis). Nota berikutnya dengan nama yang sama terpetakan otomatis.
  Pencocokan kabur dengan AI ada di luar Lapisan 1.
- Peringatan bila nota mirip (total dan jumlah baris sama, 14 hari terakhir) sudah tercatat.
- Stok dicatat dalam satuan seperti tertulis di nota. Harga modal tersimpan per baris nota.
- Selesai kalau: nota tersimpan, stok produk bertambah, dan data bertahan setelah aplikasi ditutup.

### F5. Riwayat dan stok
- Daftar nota tersimpan dan detailnya.
- Daftar stok per produk dengan harga modal terakhir dan selisihnya terhadap nota sebelumnya (naik atau turun). Ini hanya tampilan selisih, bukan peringatan margin.
- Selesai kalau: selisih harga modal benar pada data uji dengan dua nota untuk produk yang sama.

## 5. Arsitektur ringkas

- **Klien (Kotlin, Compose):** presentation (layar dan ViewModel), domain (model dan use case), data (Retrofit ke backend, Room).
- **Backend (Python):** `/v1/ekstrak` memanggil model multimodal, mengubah hasil lewat `parse_rupiah`, lalu menjalankan `validasi`. Kunci API hanya ada di sini.
- **Evaluasi:** harness Pytest mengimpor modul `nota` yang sama dengan backend, jadi yang diukur adalah kode yang dipakai aplikasi.

## 6. Evaluasi (Dani merancang)

- Data: ~50 nota toko (cetak dan tulis dipisah) dan ~50 struk dari split test CORD (CC BY 4.0). Jawaban benar dalam format skema yang sama.
- Pisahkan set dev (boleh dipakai menyetel prompt) dan set uji (tidak boleh). Angka di README hanya dari set uji.
- Usulan metrik: benar per field; nota benar seluruhnya; persen ekstraksi salah yang ditandai validator; alarm palsu validator.
- Bandingkan minimal dua ukuran model dan dua resolusi foto terhadap akurasi, biaya, latensi, dan ukuran data.

## 7. Keputusan

Sudah diputuskan (alasan di `docs/KEPUTUSAN.md`):
1. Validasi ulang saat koreksi: aturan hitungan dicerminkan di Kotlin, dijaga
   `shared/test-vectors.json` yang dijalankan Pytest dan JUnit.
2. Aturan simpan: terhalang untuk data tidak lengkap, konfirmasi untuk ketidakcocokan hitungan.
3. Distribusi lewat GitHub Releases, bukan Play Store. Tidak ada tester dan tidak ada closed test.

Masih terbuka:
4. **Model ekstraksi dan ukuran foto.** Diputuskan: `gemini-3.1-flash-lite`, 1024 px (`docs/KEPUTUSAN.md` 19).
5. **Perlindungan backend untuk APK di luar Play.** App Check Play Integrity tidak berlaku
   (`docs/KEPUTUSAN.md` 17). Dijawab dengan tidak menyediakan backend publik: APK rilis memakai mode contoh
   (`docs/KEPUTUSAN.md` 20; sudah diimplementasikan).
6. **Skema `Nota` v1** (`backend/src/nota/schema.py`) menunggu tinjauan Dani.

## 7a. Tambahan di dalam Lapisan 1

Tambahan kecil yang memperkuat alur utama tanpa membuka lapisan baru:
- Alias nama nota ke produk (lihat F4): mengurangi ketukan di nota berikutnya.
- Deteksi nota ganda: mencegah stok terhitung dua kali.
- Isi manual tanpa foto: jalan keluar saat foto tidak terbaca atau tidak ada internet.
- Foto disimpan bersama nota dan bisa dibuka saat koreksi dan di riwayat.
- Hapus nota dari riwayat; stok ikut berkurang karena stok dihitung dari baris nota.
- Database dicadangkan ke akun Google pengguna (foto tidak).
- Analitik angka saja (`nota_disimpan.durasi_detik`, `baris_dikoreksi`) sebagai alat ukur hipotesis nilai.
- Mode server tiruan (`NOTA_KLIEN=tiruan`) untuk mengembangkan aplikasi tanpa API key.
- Pra-label evaluasi (`python -m nota_eval pralabel`) untuk mempercepat pelabelan.

## 7b. Backlog (Lapisan 2 ke atas, belum dikerjakan)

Urut menurut nilai per usaha, menunggu Lapisan 1 terukur:
1. **Peringatan margin.** Butuh harga jual per produk; peringatan bila harga modal baru mendekati harga jual.
2. **Ekspor CSV / bagikan rekap** harga modal per bulan ke WhatsApp atau spreadsheet.
3. **Jalur hybrid:** OCR ML Kit di HP untuk nota cetak (offline, tanpa biaya AI), cloud untuk tulisan
   tangan. Hanya bila evaluasi per jenis nota mendukung.
4. **Scan barcode** untuk mencocokkan produk.
5. **Batch dan tanggal kedaluwarsa** saat barang datang (bila kedaluwarsa memang masalah di toko).
6. **Penjualan** (stok berkurang), Lapisan 3.
7. Sinkronisasi antarperangkat dan akun pengguna.

## 8. Risiko

- Prompt bisa overfit ke set evaluasi, sehingga pemisahan dev dan uji wajib.
- Perlindungan replay App Check tidak tersedia untuk Python. Tulis sebagai batasan.
- APK di luar Play tidak lolos App Check Play Integrity, jadi tidak ada backend publik (`docs/KEPUTUSAN.md` 20).
- Satu foto berisi dua nota digabung menjadi satu nota tanpa peringatan (nota 007, D3).
- Harga dan daftar model berubah. Catat tanggal pengukuran biaya.
- Foto nota dikirim ke server dan layanan AI. Catatan rilis di GitHub Releases dan README harus menyebutnya. Selama memakai tier gratis Gemini (Google boleh memakai isinya untuk memperbaiki produk), nota yang dikirim hanya yang memuat nama toko, nama toko penerima, daftar barang, dan harga; nomor telepon, alamat pribadi, dan nomor rekening dipotong atau ditutup dulu.

## 9. Definisi selesai Lapisan 1

- Alur F1 sampai F5 berjalan di HP asli terhadap backend asli di laptop (hari lapangan, `docs/LAPANGAN.md`), dan APK rilis
  menjalankannya dalam mode contoh tanpa server, tombol "Coba dengan nota contoh" di layar Foto (`docs/KEPUTUSAN.md` 20).
- Unit test backend dan Android (ditulis Claude, ditinjau dan dipahami Dani) hijau di CI.
- Tabel akurasi dari set uji ada di README, per jenis nota dan per model.
- Biaya per nota terukur dari penggunaan token nyata.
- APK bertanda tangan bisa diunduh dari GitHub Releases.
- README memuat batas yang diketahui dan prior work (repo receipt-parsing berbasis CORD).
