# Studi kasus: Nota Kulakan

Ringkasan satu halaman. Setiap angka dan keputusan ada sumbernya di repo; tautan di tiap bagian.

## Masalah dan pengguna
Toko sembako keluarga menerima nota kulakan (cetak dan tulisan tangan) dari grosir dan sales. Nota hanya
disimpan di rak. Stok dan harga modal tidak dihitung dari nota, sehingga kenaikan harga modal tidak terlihat.
Pengguna: satu toko, satu pemakai (Dani). Aplikasi ini studi kasus produk dan rekayasa, bukan pesaing aplikasi
kasir. ([`PRD.md`](PRD.md), [`KEPUTUSAN.md`](KEPUTUSAN.md) 1)

## Keputusan arsitektur dan trade-off
- **Model menyalin angka sebagai teks, kode yang menafsirkan.** Kesalahan "60.000" terbaca 60 terjadi saat model
  menafsirkan pemisah ribuan; penafsiran dipindah ke `parse_rupiah`/`parse_qty` yang teruji. Model juga dilarang
  membetulkan hitungan, supaya ketidakcocokan asli di nota tetap terlihat. (KEPUTUSAN 5)
- **Validator deterministik, Python sebagai acuan dan Kotlin sebagai cermin**, dijaga `shared/test-vectors.json`
  yang dijalankan Pytest dan JUnit (hasil harapan ditulis tangan). Sorotan berubah seketika saat mengetik, juga
  tanpa sinyal. Batasnya: lolos validator berarti konsisten, bukan benar; nama barang tidak diperiksa. (KEPUTUSAN 6)
- **Kunci API tidak di aplikasi; backend tipis (FastAPI).** Prompt, parser, dan validator satu kode dengan harness
  evaluasi. Biayanya: satu layanan lagi dan satu lompatan latensi. (KEPUTUSAN 3)
- **Gemini tier gratis dan konsekuensi privasi.** Di tier gratis Google boleh memakai isi permintaan untuk
  memperbaiki produknya, jadi foto evaluasi ditutup kotak hitam (nama, alamat, HP, NPWP, rekening pembeli) dan
  rilis ke pengguna lain butuh tier berbayar. (KEPUTUSAN 16)
- **Kotlin native, bukan Flutter.** Dipilih karena target peran Android (Kotlin, Compose, MVVM, Clean Architecture,
  Room) dan akses ML Kit untuk Lapisan 2. (KEPUTUSAN 2)
- **Mode contoh pada APK publik.** APK tidak terhubung ke backend publik (privasi, penyalahgunaan tanpa Play
  Integrity, biaya); ia membawa nota sintetis berlabel "contoh". AI asli ditunjukkan lewat video dan angka evaluasi,
  atau dengan menjalankan backend sendiri. (KEPUTUSAN 20)

## Evaluasi
Set uji 12 nota cetak milik sendiri, `gemini-3.1-flash-lite`, foto 1024 px, prompt v2, satu putaran resmi:
**5/12 nota sempurna (42%, Wilson 95% 19-68%)**; interval lebar karena n kecil. Aturan pemilihan model ditetapkan
sebelum uji dan diterapkan apa adanya: 3.5-flash-lite unggul 1 nota (syarat 2), jadi model tidak berganti.
Salah yang tercatat: diskon persen per baris tidak terbaca, total tulisan tangan terpakai sebagai total, PPN yang
sudah termasuk terbaca sebagai pajak tambahan, dan satu hasil rusak total. Pada nota yang kuncinya konsisten,
semua nota yang salah ditandai validator (pembagi kecil). `gemini-3.8-flash` tidak selesai dievaluasi (waktu habis
dan kuota). ([`EVALUASI.md`](EVALUASI.md), [`hasil/uji-2026-10-20-putaran2.md`](hasil/uji-2026-10-20-putaran2.md))

## Hasil lapangan (15 nota)
15 nota (10 cetak, 5 tulis tangan), 40 baris, 28-29 Okt 2026. Waktu input median 20 detik (rentang 16-100); baris
dikoreksi 5 dari 40; nota tanpa koreksi 13 dari 15 (87%, Wilson 95% 62%-96%); salah lolos 0. Dua nota berdiskon
(N07, N08) yang dikoreksi hanyalah petunjuk. Beberapa catatan menyebut server lambat, jadi waktu tidak
dibandingkan antar jenis nota. ([`hasil/lapangan-2026-10-29.md`](hasil/lapangan-2026-10-29.md))

## Fitur cari nota
Riwayat dapat dicari lewat satu kolom teks bebas (supplier, nama barang, tanggal) dan filter opsional (supplier,
rentang tanggal). Tidak ada kolom yang wajib diisi; kriteria kosong diabaikan dan yang terisi digabung dengan DAN.
Pencocokan berupa fungsi murni di lapisan domain dengan tes unit; lokal saja (Room), tanpa jaringan atau AI.
Pencocokan salah ketik belum ada.

## Batasan
Satu pengguna dan satu toko; n kecil; belum ada pengguna nyata lain. Waktu menemukan kembali nota belum diukur
(protokol di [`LAPANGAN.md`](LAPANGAN.md)). Pembanding "menulis ulang di buku" tidak dipakai. Set uji dan set dev
berbagi template pemasok. Backend belum di-deploy (dijalankan lokal). Tidak ada klaim hemat waktu, pengguna, atau
dampak bisnis.

## Pelajaran
- Memisahkan penyalinan (model) dari penafsiran dan pemeriksaan (kode) membuat kesalahan angka bisa diperiksa
  dan diuji tanpa memanggil model.
- Aturan pemilihan yang ditulis sebelum uji menghasilkan keputusan yang bisa dijelaskan, walau hasilnya tidak
  mengubah konfigurasi.
- Rancangan pembanding waktu "dibanding menulis di buku" ternyata tidak sesuai kebiasaan toko; revisinya dicatat
  setelah data dikumpulkan, bukan diam-diam diganti ([`EVALUASI.md`](EVALUASI.md) bagian 13).
- Data lapangan menemukan jenis salah baca yang tidak ada di set uji (N08: "isi per box" terbaca sebagai "jumlah
  box"); validator menandai baris itu.
