# Bahan rilis Play Store

Semua isi di sini adalah DRAF. Formulir Play berubah dari waktu ke waktu; cocokkan setiap jawaban
dengan pertanyaan yang muncul di Play Console dan dengan perilaku aplikasi yang sebenarnya.

## 1. Listing toko

- **Nama aplikasi:** Nota Kulakan
- **Deskripsi singkat (maks. 80 karakter):**
  Foto nota grosir, stok & harga modal tercatat, hitungannya dicek otomatis.
- **Deskripsi lengkap:**

  Nota Kulakan membantu pemilik toko kelontong dan sembako mencatat barang masuk dari nota grosir
  atau sales tanpa mengetik ulang.

  - Foto nota, cetak maupun tulisan tangan. Daftar barang, jumlah, dan harga terbaca otomatis.
  - Setiap baris diperiksa: jumlah x harga harus sama dengan nilai baris, dan semua baris harus sama
    dengan total nota. Baris yang tidak cocok ditandai merah dan bisa diperbaiki satu per satu.
  - Barang masuk dan harga modal tercatat per produk. Kenaikan atau penurunan harga modal dari nota
    sebelumnya langsung terlihat.
  - Nama barang di nota diingat. Nota berikutnya dengan singkatan yang sama langsung masuk ke produk
    yang benar.
  - Peringatan bila nota yang sama dicatat dua kali.
  - Bisa isi manual tanpa foto.

  Data nota disimpan di HP Anda. Foto dikirim ke server hanya untuk dibaca dan tidak disimpan di server.

  Ini proyek portofolio. Penjualan belum dicatat, jadi angka stok adalah total barang masuk.

- **Kategori:** Bisnis
- **Grafis yang dibutuhkan:** ikon 512x512 (ekspor dari ikon adaptif), grafis fitur 1024x500,
  minimal 2 tangkapan layar HP. Gunakan data contoh atau nota yang nama suppliernya ditutup.

## 2. Data Safety (draf jawaban)

| Pertanyaan | Jawaban | Alasan |
|---|---|---|
| Mengumpulkan atau membagikan data? | Ya | Foto dikirim ke server; analitik dan crash |
| Data dienkripsi saat transit? | Ya | HTTPS ke Cloud Run dan Firebase |
| Pengguna bisa meminta data dihapus? | Ya | Hapus nota di aplikasi; server tidak menyimpan foto |
| Foto (Photos) | Dikumpulkan, tidak dibagikan, opsional, tujuan: fungsi aplikasi | Bisa isi manual; Gemini sebagai penyedia layanan |
| Interaksi aplikasi (App interactions) | Dikumpulkan, tujuan: analitik | Firebase Analytics, hanya angka dan status |
| Log crash dan diagnostik | Dikumpulkan, tujuan: fungsi aplikasi dan analitik | Firebase Crashlytics |
| ID perangkat atau lainnya | Dikumpulkan, tujuan: analitik | ID instalasi Firebase |
| Lokasi, kontak, keuangan, dll. | Tidak | Tidak diakses |

Catatan: "dibagikan" di Play tidak mencakup penyedia layanan yang memproses data atas nama Anda.
Cek panduan Data Safety dari Firebase untuk setiap SDK yang dipakai, karena daftar datanya bisa berubah.
Pertimbangkan pilihan "diproses sementara" untuk foto hanya bila ketentuan retensi Gemini API yang
berlaku memang mendukungnya.

## 3. Formulir lain

- **Rating konten:** kuesioner IARC, aplikasi utilitas bisnis tanpa konten sensitif.
- **Target audiens:** 18 tahun ke atas.
- **Iklan:** tidak ada.
- **Akses aplikasi:** tidak butuh login. Catat bahwa membaca nota butuh internet.
- **Pernyataan aplikasi keuangan:** bukan aplikasi layanan keuangan (tidak ada pinjaman, pembayaran,
  atau investasi). Jawab sesuai pertanyaan yang muncul.

## 4. Pesan ajakan tester (WhatsApp)

> Halo [nama], aku lagi bikin aplikasi Android buat portofolio: foto nota belanja grosir, barang dan
> harga modalnya kecatat otomatis. Google mewajibkan 12 orang ikut uji tertutup selama 14 hari sebelum
> aplikasinya boleh rilis.
>
> Boleh bantu? Caranya:
> 1. Kirim email akun Google yang dipakai di HP Android-mu.
> 2. Nanti aku kirim link, klik "Jadi penguji", lalu pasang dari Play Store.
> 3. Biarkan terpasang 14 hari, buka sesekali. Coba foto nota/struk apa saja.
> 4. Kalau ada yang aneh atau membingungkan, kabari aku. Masukanmu aku pakai buat perbaikan.
>
> Aplikasinya tidak minta login dan tidak minta data pribadi. Makasih banyak!

Rekrut 15-20 orang agar tetap di atas 12 kalau ada yang keluar.

## 5. Kuesioner akses produksi

Google menanyakan hal seperti: bagaimana tester direkrut, seberapa aktif mereka, masukan apa yang
didapat, dan perubahan apa yang dibuat. Jawab dari `docs/LOG-CLOSED-TEST.md`:
- jumlah tester dan cara rekrut (teman, keluarga, rekan kuliah);
- daftar masukan nyata dan versi yang memperbaikinya;
- metrik dari analitik dan Crashlytics (jumlah nota, crash yang diperbaiki);
- alasan aplikasi siap: alur utama stabil, crash diketahui sudah diperbaiki.

Jangan mengarang masukan atau angka. Aplikasi bisa ditolak dan diminta tes ulang.
