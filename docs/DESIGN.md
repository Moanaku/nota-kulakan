# DESIGN.md: token visual Nota Kulakan (arah A, "Buku kas")

Status: DISETUJUI Dani (2026-10-20, lewat TUGAS-6). Sumber kebenaran token;
`Theme.kt` (Compose) hanya menerjemahkan berkas ini. Nilai di luar berkas ini tidak boleh dipakai di
layar: kalau butuh nilai baru, ubah berkas ini dulu.

**Pengguna dan situasi.** Pemilik toko sembako (orang tua Dani), HP Android, memotret nota kapan saja
(tidak harus saat toko buka), lalu memeriksa angka. Prioritas: angka rupiah mudah dibaca, koreksi
tidak mudah salah tekan, status "cocok / tidak cocok" jelas tanpa bergantung pada warna saja.

**Alasan aksen.** Hijau botol dipilih karena aplikasi ini soal catatan dagang: hijau dibaca sebagai
"tercatat / cocok" (seperti tinta buku kas), dan jauh dari merah galat sehingga status tidak tertukar.
Kalau alasan ini tidak berlaku lagi, aksennya yang harus diganti, bukan alasannya diubah diam-diam.

**Larangan tetap.** Tanpa gradien latar, tanpa dynamic color, tanpa `fromSeed` (seluruh `ColorScheme`
ditulis eksplisit), tanpa Inter/Poppins/Roboto, tanpa emoji sebagai ikon, tanpa hex mentah di widget.

## 1. Warna (satu aksen: hijau botol)

| Peran | Terang | Gelap |
|---|---|---|
| latar (permukaan 1) | #F4F0E7 | #1B1712 |
| container (permukaan 2) | #EBE6DA | #241F18 |
| container-high (permukaan 3) | #E0DACB | #2E281F |
| teks | #2A2118 | #EDE6D8 |
| teks-sekunder | #5E5446 | #B7AC99 |
| garis-kontrol (batas input, tombol sekunder) | #867B69 | #8F8472 |
| pemisah (divider, dekoratif) | #D3CCBB | #3A3328 |
| aksen | #1F5A45 | #7FC3A3 |
| di-atas-aksen | #F7F4EC | #0F2A20 |
| aksen-container | #D3E5DA | #1F3F31 |
| di-atas-aksen-container | #12382A | #CDE8DA |
| galat | #A63A2B | #F0A294 |
| di-atas-galat | #FFF8F6 | #3A0F08 |
| galat-container | #F3D9D3 | #4A1D15 |
| di-atas-galat-container | #5E1A10 | #F7D6CF |

Warna galat bukan aksen kedua: ia peran status. Status "info" tidak punya warna sendiri: container-high
+ teks-sekunder + ikon. Teks sekunder selalu lewat peran `teks-sekunder`, bukan Opacity.

Rasio kontras (dihitung dengan rumus WCAG 2.x; teks normal >= 4,5, batas kontrol >= 3):

| Pasangan | Terang | Gelap |
|---|---|---|
| teks / latar | 13,89 | 14,36 |
| teks / container | 12,69 | 13,17 |
| teks / container-high | 11,33 | 11,75 |
| teks-sekunder / latar | 6,52 | 7,96 |
| teks-sekunder / container | 5,95 | 7,30 |
| teks-sekunder / container-high | 5,32 | 6,51 |
| garis-kontrol / latar | 3,66 | 4,85 |
| garis-kontrol / container | 3,34 | 4,45 |
| aksen (teks/ikon) / latar | 7,08 | 8,69 |
| aksen / container | 6,47 | 7,97 |
| di-atas-aksen / aksen | 7,33 | 7,46 |
| di-atas-aksen-container / aksen-container | 9,84 | 8,91 |
| galat (teks/ikon) / latar | 5,66 | 8,75 |
| galat / container | 5,17 | 8,02 |
| di-atas-galat / galat | 6,14 | 8,23 |
| di-atas-galat-container / galat-container | 9,61 | 10,47 |

Pemisah (1,41 / 1,43) sengaja tipis karena dekoratif, bukan batas komponen interaktif.

## 2. Tipografi (2 keluarga, 6 ukuran, seluruh skala di-override)

- **Newsreader** (berseri): hanya judul layar dan judul bagian. **Source Sans 3**: semua lainnya.
  Keduanya berlisensi OFL; berkas font dibundel di aplikasi.
- Angka (rupiah, qty, total): Source Sans 3. Fitur `tnum` TIDAK ada di font ini (dicek di tabel GSUB
  2026-10-20), tetapi lebar glyph angka 0-9 identik (Regular 497, SemiBold 513), jadi angka bawaannya
  sudah tabular. Jangan mengaktifkan `pnum` (proporsional). Rata kanan di kolom jumlah.

| Peran | Ukuran / tinggi baris | Keluarga, bobot |
|---|---|---|
| angka-besar (total nota) | 32 / 40 sp | Source Sans 3, 600 |
| judul-layar | 24 / 32 sp | Newsreader, 600 |
| judul-bagian | 20 / 28 sp | Newsreader, 600 |
| isi | 16 / 24 sp | Source Sans 3, 400 |
| isi-tebal (nama barang, label tombol) | 16 / 24 sp | Source Sans 3, 600 |
| keterangan | 14 / 20 sp | Source Sans 3, 400 |
| label-kecil | 12 / 16 sp | Source Sans 3, 600 |

Teks tidak lebih kecil dari 12 sp, dan teks yang harus dibaca pengguna untuk mengambil keputusan
(angka, status, pesan galat) tidak lebih kecil dari 14 sp.

## 3. Spasi, bentuk, elevasi, motion

- **Spasi**: 4 / 8 / 12 / 16 / 24 / 32 / 48 dp. Tidak ada nilai lain. Gutter layar 16 dp.
- **Radius** (3 nilai): 4 dp (input, chip), 8 dp (tombol, baris yang bisa diketuk), 16 dp (sheet bawah,
  jendela dialog).
- **Elevasi** (2 tingkat): 0 dp (default; kartu = outline `pemisah`, tanpa bayangan) dan 2 dp (hanya
  sheet bawah dan dialog).
- **Hierarki baris barang** (tiga tingkat, tidak lebih): nama barang 16 sp isi-tebal `teks`; qty x harga
  14 sp keterangan `teks-sekunder`; jumlah 16 sp isi-tebal rata kanan, tabular; status sebagai chip.
  Jarak antar baris (16 dp) lebih besar daripada jarak di dalam baris (4 dp).
- **Daftar = baris + divider**, bukan kartu satu per baris. Kartu dipakai hanya untuk satu objek yang
  berdiri sendiri (mis. ringkasan nota).
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
- `BarAtas`: bilah judul layar; tombol kembali hanya bila `onKembali` diberikan.
- `TombolCatatNota`: aksi utama di Stok dan Riwayat; datar, warna aksen.
- `BarBawah`: navigasi dua area (Stok, Riwayat); `Tab` menandai area aktif.
- `PesanPeriksa` (`JenisPesan` GALAT / INFO / COCOK): pesan pemeriksaan satu baris atau nota.
- `statusHarga` dan `ChipSelisihHarga`: perubahan harga modal. Naik = selisih, turun = cocok, tetap atau
  produk baru = info.
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
dibangun. Berkas font dan `Theme.kt` dibuat setelah berkas ini disetujui.
