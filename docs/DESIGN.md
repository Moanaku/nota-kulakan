# DESIGN.md: token visual Nota Kulakan (arah A, "Buku kas", v2)

Status: DISETUJUI Dani 2026-10-21, v2 (token dari `DESIGN-USULAN-v2.md`, diterapkan di TUGAS-19; v1 disetujui 2026-10-20
lewat TUGAS-6). Sumber kebenaran token;
`Theme.kt` (Compose) hanya menerjemahkan berkas ini. Nilai di luar berkas ini tidak boleh dipakai di
layar: kalau butuh nilai baru, ubah berkas ini dulu.

**Pengguna dan situasi.** Pemilik toko sembako (orang tua Dani), HP Android, memotret nota kapan saja
(tidak harus saat toko buka), lalu memeriksa angka. Prioritas: angka rupiah mudah dibaca, koreksi
tidak mudah salah tekan, status "cocok / tidak cocok" jelas tanpa bergantung pada warna saja.

**Alasan aksen.** Hijau daun (#38683F terang, #8FCB9B gelap) dipilih karena aplikasi ini soal catatan dagang: hijau
dibaca sebagai "tercatat / cocok" (seperti tinta buku kas), dan jauh dari merah galat sehingga status tidak
tertukar. v2 menggantikan hijau botol #1F5A45: nilainya lebih cerah dan lebih hidup, kontrasnya terhadap latar
tetap >= 4,5 (6,10 terang, 9,78 gelap). Kalau alasan ini tidak berlaku lagi, aksennya yang harus diganti, bukan
alasannya diubah diam-diam. Chip "Harga baru" memakai aksen-container (bukan netral); ia dibedakan dari "cocok"
oleh teks dan tidak memakai ikon centang.

**Alasan v2.** Keluhan Dani 2026-10-20: teks dan latar krem terasa menyatu, mata capek. v2 menaikkan kontras
(teks-sekunder jauh lebih gelap, garis-kontrol >= 3:1 terhadap semua permukaan terang, latar sedikit lebih terang).

**Larangan tetap.** Tanpa gradien latar, tanpa dynamic color, tanpa `fromSeed` (seluruh `ColorScheme`
ditulis eksplisit), tanpa Inter/Poppins/Roboto, tanpa emoji sebagai ikon, tanpa hex mentah di widget.

## 1. Warna (satu aksen: hijau daun)

| Peran | Terang | Gelap |
|---|---|---|
| latar (permukaan 1) | #FBF7EF | #17140F |
| container (permukaan 2) | #F1EADB | #211D16 |
| container-high (permukaan 3) | #E6DDC8 | #2B261D |
| teks | #231F18 | #F3ECDD |
| teks-sekunder | #4A4234 | #D2C8B3 |
| garis-kontrol (batas input, tombol sekunder) | #756B58 | #A09580 |
| pemisah (dekoratif) | #DDD3BE | #3A3328 |
| aksen | #38683F | #8FCB9B |
| di-atas-aksen | #FFFDF8 | #0F2A18 |
| aksen-container | #DCEBD6 | #24412B |
| di-atas-aksen-container | #234A28 | #D6EBD8 |
| galat | #B3361E | #F2A596 |
| di-atas-galat | #FFF8F6 | #3A0F08 |
| galat-container | #FBE3DC | #4A1D15 |
| di-atas-galat-container | #7A1F10 | #F7D6CF |

Warna galat bukan aksen kedua: ia peran status. Status "info" tidak punya warna sendiri: container-high
+ teks-sekunder + ikon. Teks sekunder selalu lewat peran `teks-sekunder`, bukan Opacity.

Rasio kontras (dihitung dengan rumus WCAG 2.x; teks normal >= 4,5, batas kontrol >= 3):

| Pasangan | Terang | Gelap |
|---|---|---|
| teks / latar | 15,35 | 15,61 |
| teks / container | 13,69 | 14,26 |
| teks / container-high | 12,14 | 12,77 |
| teks-sekunder / latar | 9,27 | 11,07 |
| teks-sekunder / container | 8,27 | 10,11 |
| teks-sekunder / container-high | 7,33 | 9,05 |
| garis-kontrol / latar | 4,91 | 6,21 |
| garis-kontrol / container | 4,38 | 5,68 |
| garis-kontrol / container-high | 3,88 | 5,08 |
| aksen (teks/ikon) / latar | 6,10 | 9,78 |
| aksen / container | 5,44 | 8,93 |
| aksen / container-high | 4,83 | 8,00 |
| di-atas-aksen / aksen | 6,42 | 8,20 |
| di-atas-aksen-container / aksen-container | 8,12 | 8,97 |
| aksen / aksen-container | 5,25 | 5,99 |
| galat / latar | 5,67 | 9,27 |
| di-atas-galat-container / galat-container | 8,43 | 10,47 |
Pemisah (1,39 / 1,47) sengaja tipis karena dekoratif, bukan batas komponen interaktif.

## 2. Tipografi (2 keluarga, 6 ukuran, seluruh skala di-override)

- **Source Serif 4** (berseri, SemiBold statis): hanya judul layar dan judul bagian. **Source Sans 3**: semua lainnya.
  Keduanya berlisensi OFL; berkas font dibundel di aplikasi.
- Angka (rupiah, qty, total): Source Sans 3. Fitur `tnum` TIDAK ada di font ini (dicek di tabel GSUB
  2026-10-20), tetapi lebar glyph angka 0-9 identik (Regular 497, SemiBold 513), jadi angka bawaannya
  sudah tabular. Jangan mengaktifkan `pnum` (proporsional). Rata kanan di kolom jumlah.

| Peran | Ukuran / tinggi baris | Keluarga, bobot |
|---|---|---|
| angka-besar (total nota) | 28 / 36 sp | Source Sans 3, 600 |
| judul-layar | 24 / 32 sp | Source Serif 4, 600 |
| judul-bagian | 20 / 28 sp | Source Serif 4, 600 |
| isi | 16 / 24 sp | Source Sans 3, 400 |
| isi-tebal (nama barang, label tombol) | 16 / 24 sp | Source Sans 3, 600 |
| keterangan | 14 / 20 sp | Source Sans 3, 400 |
| label-kecil | 12 / 16 sp | Source Sans 3, 600 |

Teks tidak lebih kecil dari 12 sp (12 sp hanya label bilah navigasi), dan teks yang harus dibaca pengguna untuk mengambil keputusan
(angka, status, pesan galat) tidak lebih kecil dari 14 sp.

## 3. Spasi, bentuk, elevasi, motion

- **Spasi**: 4 / 8 / 12 / 16 / 24 / 32 / 48 dp. Tidak ada nilai lain. Gutter layar 16 dp.
- **Radius** (3 nilai): 4 dp (input, chip), 8 dp (tombol, baris yang bisa diketuk), 16 dp (kartu terisi, sheet
  bawah, jendela dialog, tombol tengah bilah navigasi). Tidak ada lingkaran atau pil, kecuali penanda aktif bilah (pil 64x32, radius 16).
- **Elevasi** (2 tingkat): 0 dp (default; kartu = terisi `container` + garis `pemisah`, tanpa bayangan) dan 2 dp (hanya
  sheet bawah dan dialog).
- **Hierarki baris barang** (tiga tingkat, tidak lebih): nama barang 16 sp isi-tebal `teks`; qty x harga
  14 sp keterangan `teks-sekunder`; jumlah 16 sp isi-tebal rata kanan, tabular; status sebagai chip.
  Jarak antar baris (16 dp) lebih besar daripada jarak di dalam baris (4 dp).
- **Daftar = baris + divider**, bukan kartu satu per baris. Kartu dipakai hanya untuk satu objek yang
  berdiri sendiri (mis. "Nota terakhir" di Stok). Riwayat memakai `BarisNota`: toko 16 sp isi-tebal, tanggal
  dan jumlah baris 14 sp keterangan, total 16 sp isi-tebal rata kanan; divider di antara baris.
- **Motion**: satu easing `cubic-bezier(0.2, 0, 0, 1)`; durasi 150 / 200 / 300 ms untuk transisi dalam
  layar. Hormati pengaturan "hapus animasi" sistem.
- **Target sentuh** >= 48 dp. **Ikon**: satu keluarga (Material Symbols Outlined, bobot 400), satu
  ukuran dasar 24 dp. Setiap ikon tanpa teks punya `contentDescription`.

## 4. Komponen inti (dibangun sebelum layar mana pun)

Tombol (utama, sekunder, teks), kolom input (angka rupiah dan teks), baris barang (nama, qty x harga,
jumlah, status), baris ringkasan total, chip status (cocok / selisih / belum terbaca), kartu ringkasan
nota, sheet koreksi, serta tiga kondisi data: kosong, skeleton, galat (sebab + tombol "Coba lagi"), dan
spanduk offline. Layar hanya merakit komponen ini. Komponen baru butuh izin Dani.

Komponen yang sudah disetujui dan dipakai (kondisinya ada di katalog debug, `KatalogKomponen`):
- `BarAtas`: bilah judul layar; tombol kembali hanya bila `onKembali` diberikan. Tinggi minimum, bukan tetap:
  judul panjang atau skala font besar membungkus ke baris berikutnya.
- `BarisNota`: satu nota di daftar Riwayat (baris + divider). `KartuRingkasanNota` tetap untuk satu nota yang
  berdiri sendiri.
- `BarBawahTengah`: bilah navigasi tiga posisi, tinggi 72 dp, latar container, garis atas pemisah. Stok dan Riwayat
  adalah tab (penanda aktif = pil aksen-container 64x32, radius 16); "Catat nota" di tengah adalah AKSI, bukan
  tab: persegi bulat 56x56 radius 16, isi aksen, ikon plus 24 di-atas-aksen, cincin 4 dp warna latar, terangkat
  24 dp di atas bilah, tanpa bayangan, label 12 sp. Semantik: tombol berlabel "Catat nota". Menggantikan
  `TombolCatatNota` (FAB) dan `BarBawah` (dihapus).
- `PesanPeriksa` (`JenisPesan` GALAT / INFO / COCOK): pesan pemeriksaan satu baris atau nota.
- `statusHarga` dan `ChipSelisihHarga`: perubahan harga modal. Naik = selisih, turun = cocok, tetap = titik 8 dp +
  teks 14 sp (netral), produk baru = chip aksen-container.
- `FotoNota`: foto nota dari berkas lokal; tanpa berkas tampil placeholder.
- `SheetPilihan`: sheet bawah untuk memilih satu pilihan (mis. produk), turunan gaya `SheetKoreksi`. Tanpa
  Simpan/Batal; sheet selalu tampil penuh. Isi memuat daftar tanpa `verticalScroll`.
- Parameter tambahan `KondisiGalat(teksAksi = ...)`: bawaannya "Coba lagi". Aksi yang bukan mengulang
  panggilan memakai label lain: "Foto ulang" untuk foto yang salah.

Chip status pada kartu Riwayat dan Stok belum ada: kolom `dikonfirmasi` belum tersimpan (ditolak sementara).

Status baris selalu ganda: ikon + teks + warna (tidak pernah warna saja).

## 5. Keputusan desain sadar

Angka dianggap isi utama aplikasi, bukan hiasan: semua rupiah memakai angka tabular rata kanan dan
bobot 600 pada kolom jumlah, jadi selisih hitungan terlihat dari bentuk kolomnya sebelum dibaca.
Hijau dipakai hanya untuk "cocok" dan aksi utama; semua masalah memakai galat + ikon + kalimat
penjelas, bukan sekadar merah.

## 6. Do dan Don't (ringkas; aturan lengkap ada di instruksi project UI)

Do: aksen hanya untuk "cocok" dan aksi utama; angka tabular rata kanan; ikon `Icons.Outlined.*`;
daftar = baris + divider; skeleton berbentuk baris barang; pesan galat menyebut sebab + "Coba lagi";
satu elemen dominan per layar (di layar hasil: total nota); label diuji dengan teks Indonesia terpanjang
dan skala font 130-200%.
Don't: gradien, Inter/Poppins/Roboto, dynamic color, `fromSeed`, `Icons.Default`/`Icons.Filled`
(campur gaya), `Card` untuk setiap baris, tiga kartu seragam berjajar, spinner tengah layar sebagai
satu-satunya loading, `Opacity` untuk teks sekunder, "Oops"/"No data"/"Submit", pesan exception mentah.

## 7. Yang belum diputuskan

Teks Indonesia terpanjang untuk uji pemotongan label, dan nama layar akhir, ditentukan saat komponen
dibangun. 
