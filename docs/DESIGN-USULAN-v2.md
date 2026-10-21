# DESIGN-USULAN-v2.md: token usulan untuk redesain (DISETUJUI)

Status: DISETUJUI Dani 2026-10-21; sudah dipindahkan ke `docs/DESIGN.md` (sumber kebenaran). Berkas ini disimpan sebagai riwayat usulan.
Sumber arah: kanvas "Nota Kulakan Redesign" (Opsi A cerah, tombol tengah). Struktur dan bentuk dipakai; nilai warna
dinaikkan kontrasnya (keluhan Dani 2026-10-20: teks dan latar krem terasa menyatu, mata capek).
Halaman uji coba: Stok dan harga modal (kanvas "Nota Kulakan: uji kontras layar", dua layar terakhir).

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

Perubahan dari DESIGN.md: aksen hijau botol #1F5A45 menjadi hijau daun #38683F (gelap #7FC3A3 menjadi #8FCB9B);
teks-sekunder jauh lebih gelap; garis-kontrol dinaikkan agar batas kotak input >= 3:1; latar sedikit lebih terang.
Alasan aksen tetap: hijau dibaca "tercatat / cocok"; kalau alasan itu tidak berlaku, aksennya yang diganti.

## 2. Rasio kontras (WCAG 2.x, dihitung)

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
Pemisah (1,39 / 1,47) sengaja tipis karena dekoratif. Baru dihitung dengan rumus, belum diuji oleh KontrasTemaTest.

## 3. Tipografi (2 keluarga, 6 ukuran)

Newsreader diganti Source Serif 4 (SemiBold) untuk judul; Source Sans 3 untuk isi. Keduanya OFL (berkas font dan lisensi
di `licenses/` perlu diperbarui). Bobot hanya 400 dan 600 (referensi memakai 700; tidak dipakai).

| Peran | Ukuran / tinggi baris | Keluarga, bobot |
|---|---|---|
| angka-besar | 28 / 36 | Source Sans 3, 600 |
| judul-layar | 24 / 32 | Source Serif 4, 600 |
| judul-bagian | 20 / 28 | Source Serif 4, 600 |
| isi, isi-tebal | 16 / 24 | Source Sans 3, 400 / 600 |
| keterangan | 14 / 20 | Source Sans 3, 400 (dan 600 untuk chip) |
| label-kecil | 12 / 16 | Source Sans 3, 600 (hanya label bilah navigasi) |

Teks keputusan (angka, status, galat, satuan) tidak lebih kecil dari 14 sp. Ukuran 13, 13,5, 15, 17, 18, 22 pada referensi dihapus.

## 4. Spasi, bentuk, elevasi, ikon

- Spasi 4 / 8 / 12 / 16 / 24 / 32 / 48 (nilai 6, 10, 14, 18, 20, 28, 36 pada referensi dibulatkan). Gutter layar 16.
- Radius 4 (chip, input) / 8 (tombol, baris yang bisa diketuk) / 16 (kartu terisi, sheet, jendela, tombol tengah). Lingkaran dan pil dihapus.
- Elevasi 0 (default) dan 2 (hanya sheet dan dialog). Tombol tengah TIDAK berbayangan: terangkat 24 dp dengan cincin warna latar.
- Ikon: Icons.Outlined, 24 dp, tebal garis seragam. Ikon khusus (bingkai bidik) digambar sebagai vektor sendiri bergaya sama.
- Target sentuh >= 48 dp (referensi 44 dinaikkan).

## 5. Komponen baru yang butuh izin

- `BarBawahTengah`: bilah navigasi tiga posisi (Stok, Catat nota di tengah, Riwayat). Posisi tengah adalah AKSI (membuka alur foto),
  bukan tujuan; ia tidak menandai area aktif. Menggantikan `TombolCatatNota` (FAB) di Stok dan Riwayat.
- Status harga (`statusHarga`): "Harga tetap" = titik + teks; "Harga baru" = chip aksen-container (mengikuti referensi; DESIGN.md
  sebelumnya menjadikan "baru" netral). Perlu keputusan Dani.

## 6. Halaman uji coba dan yang harus dicek Dani

Stok dan harga modal, terang dan gelap, di kanvas. Cek: (1) apakah teks dan latar masih terasa menyatu; (2) apakah tombol tengah jelas
sebagai aksi tanpa bayangan; (3) apakah chip "Harga baru" hijau tidak tertukar dengan "cocok"; (4) skala huruf besar 1,3 dan 2,0 (belum diuji, mockup).
Belum ada: empat kondisi data, layar lain, uji di HP. Penerapan ke aplikasi menunggu persetujuan berkas ini.
