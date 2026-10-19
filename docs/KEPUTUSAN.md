# Catatan keputusan

Setiap keputusan teknis penting, alternatifnya, dan alasannya. Ditulis supaya bisa dijelaskan di
interview dan ditinjau ulang bila datanya berubah. Format: keputusan, alternatif, alasan, cara menguji.

## 1. Masalah: nota kulakan toko sembako
- **Alternatif:** struk restoran patungan, pengingat kedaluwarsa, pesanan WhatsApp, meteran kos.
- **Alasan:** pengguna nyata (satu toko sembako), hasil AI paling bisa diperiksa otomatis (aritmetika),
  data publik untuk separuh evaluasi (CORD), dan nota memang menumpuk di laci tanpa dipakai.
- **Uji:** Fase 3 runbook. Bila nota tulis tangan tidak terbaca, keputusan ini ditinjau ulang.
- **Catatan (19 Okt 2026):** hipotesis tulisan tangan belum bisa diuji dengan nota milik sendiri karena set utama cetak semua.

## 2. Kotlin native + Compose, bukan Flutter
- **Alasan:** lowongan Android perbankan yang ditemukan meminta Kotlin, Compose, MVVM, Clean
  Architecture, DI, Room. Akses langsung ke ML Kit untuk jalur OCR di HP pada Lapisan 2. Pengalaman
  Flutter tetap ada di CV.

## 3. Backend tipis Python, bukan Firebase AI Logic
- **Alternatif:** Firebase AI Logic memanggil Gemini langsung dari aplikasi lewat proxy terkelola.
- **Alasan:** prompt, parser, dan validator harus satu kode yang sama dengan harness evaluasi. Dengan
  AI Logic, validator tinggal di Kotlin dan evaluasi Python mengukur kode lain. Kunci API juga tetap
  hanya di server.
- **Biaya keputusan:** satu layanan lagi untuk dideploy (Cloud Run) dan latensi satu lompatan.

## 4. Lapisan 1: ekstraksi di cloud untuk semua nota
- **Alternatif:** OCR di HP (ML Kit) untuk nota cetak, Gemini Nano di HP.
- **Alasan:** setengah nota ditulis tangan; ML Kit untuk tulisan tangan dirancang untuk tinta digital,
  bukan foto kertas; Gemini Nano hanya ada di HP flagship. Jalur hybrid baru dibangun bila evaluasi
  per jenis nota menunjukkan nota cetak cukup akurat dengan OCR di HP.

## 5. Model menyalin angka, kode yang menafsirkan
- **Alternatif:** minta model mengembalikan bilangan.
- **Alasan:** kesalahan klasik "60.000" terbaca 60 terjadi saat model menafsirkan pemisah ribuan.
  Penafsiran dipindah ke `parse_rupiah` yang teruji. Model juga dilarang membetulkan hitungan, agar
  ketidakcocokan asli di nota tetap terlihat oleh validator.

## 6. Validator deterministik, dicerminkan di Kotlin, dijaga vektor uji bersama
- **Alternatif:** validasi ulang lewat panggilan server setiap kali pengguna mengedit.
- **Alasan:** sorotan merah harus berubah seketika saat mengetik, juga tanpa sinyal. Risiko dua
  implementasi menyimpang ditutup oleh `shared/test-vectors.json` yang dijalankan Pytest dan JUnit;
  hasil harapan ditulis tangan, bukan dihasilkan salah satu implementasi.
- **Batas yang diakui:** lolos validator = konsisten, bukan benar. Nama barang tidak diperiksa.

## 7. Aturan simpan: terhalang vs konfirmasi
- Data tidak lengkap (field kosong, qty tidak sah, angka tak terbaca) **terhalang**: stok tidak boleh
  berisi angka kosong.
- Ketidakcocokan hitungan boleh disimpan setelah pengguna mencentang bahwa angkanya sudah sesuai nota
  fisik, karena nota grosir asli kadang memang salah hitung. Nota seperti itu ditandai di riwayat.

## 8. Representasi angka
- Uang `int`/`Long` rupiah, tidak pernah float. Qty `Decimal`/`BigDecimal`.
- Stok tidak disimpan sebagai angka yang diperbarui, tetapi dijumlahkan dari baris nota (qty x 1000
  sebagai bilangan bulat). Menghapus nota otomatis membatalkan stoknya; tidak ada angka yang bisa
  melenceng dari riwayat.

## 9. App Check dengan token sekali pakai
- **Alasan:** tanpa App Check siapa pun yang tahu URL bisa menghabiskan kuota Gemini. Token sekali
  pakai dikonsumsi saat verifikasi sehingga tidak bisa diputar ulang (didukung `firebase-admin` 7.7.0,
  dicek langsung di SDK).
- **Biaya:** satu panggilan jaringan tambahan per permintaan; bisa dimatikan dengan `APP_CHECK_CONSUME=false`.
- **Risiko terbuka:** APK yang dipasang di luar Play mungkin tidak lolos Play Integrity.

## 10. Foto
- Dikompres di HP ke sisi 1600 px, JPEG 85, sebelum diunggah. Parameter ini variabel evaluasi; harness
  meniru kompresi yang sama.
- Kamera lewat aplikasi kamera sistem dan galeri lewat Photo Picker: tanpa izin CAMERA dan izin
  penyimpanan.
- Server tidak menyimpan foto. Di HP, foto disimpan bersama nota untuk audit, tetapi tidak ikut cadangan
  cloud (batas 25 MB Auto Backup).

## 11. Pemetaan nama nota ke produk: alias persis, bukan AI
- **Alternatif:** minta model mencocokkan nama ke katalog.
- **Alasan:** pencocokan persis atas nama yang dinormalkan tidak pernah salah tebak; pengguna memetakan
  sekali, nota berikutnya otomatis. Pencocokan kabur dengan AI masuk backlog bila datanya menunjukkan
  banyak variasi ejaan.

## 12. Deteksi nota ganda
- Total sama, jumlah baris sama, dalam 14 hari = peringatan, bukan larangan. Heuristik sederhana yang
  bisa dijelaskan, mencegah stok terhitung dua kali.

## 13. Versi dependensi
- Satu set rilis stabil 2025 yang saling kompatibel (AGP 8.13, Kotlin 2.2.20, Compose BOM 2025.09),
  bukan yang terbaru, karena penulis tidak bisa me-resolve dependensi di lingkungannya. Upgrade besar
  dikerjakan terpisah setelah Lapisan 1 stabil.

## 14. Evaluasi
- Set dev untuk menyetel, set uji untuk melapor; uji tidak pernah dipindah.
- Interval Wilson karena n kecil. Validator dinilai dengan matriks kebingungan hanya pada nota yang
  kebenarannya konsisten.
- Pra-label oleh model diberi penanda dan ditolak harness sampai diperiksa manual, untuk menekan bias jangkar.

## 15. Analitik hanya angka
- Peristiwa `nota_diekstrak`, `nota_gagal`, `nota_disimpan` dengan jumlah baris, baris dikoreksi, dan
  durasi. Ini alat ukur hipotesis nilai (waktu per nota, tingkat koreksi), bukan pelacakan isi nota.
  Build debug tidak mengirim analitik.

## 16. Gemini tier gratis, dengan aturan isi nota
- **Keputusan (12 Okt 2026):** API key Gemini dari AI Studio tier gratis, tanpa billing. Nota yang
  boleh dikirim hanya yang memuat nama toko, nama toko penerima, daftar barang, dan harga. Nomor
  telepon, alamat pribadi, dan nomor rekening dipotong atau ditutup sebelum dikirim.
- **Alternatif:** tier berbayar (aturan sebelumnya: foto asli hanya lewat billing aktif).
- **Alasan:** Proyek ini memutuskan memakai tier gratis tanpa billing. Di tier gratis Google boleh memakai isi permintaan
  untuk memperbaiki produknya, jadi yang dikirim dibatasi pada data dagang, bukan data pribadi.
- **Akibat:** kuota per menit dan per hari terbatas; harness evaluasi memberi jeda dan menunggu saat 429.
  Ketentuan ini tidak cocok untuk rilis ke pengguna lain (mereka tidak menyensor fotonya), jadi rilis
  butuh tier berbayar; kebijakan privasi masih menulis layanan berbayar.
- **Cara menguji:** sebelum `pralabel` atau `jalankan`, buka setiap foto di `data/private/toko/` dan
  pastikan tidak ada nomor telepon, alamat pribadi, atau nomor rekening yang terbaca.

## 17. Distribusi via GitHub Releases, bukan Play Store
- **Keputusan (19 Okt 2026):** APK bertanda tangan dibagikan lewat GitHub Releases. Tidak ada rilis
  ke Play Store, tidak ada tester, dan tidak ada closed test.
- **Alternatif:** Play Store lewat akun Play Console personal (closed test 12 tester selama 14 hari
  sebelum akses produksi). Bahannya diarsipkan di `docs/arsip/`.
- **Alasan:** Proyek ini adalah studi kasus, bukan produk untuk pengguna luas. Jalur Play menuntut
  akun, verifikasi identitas, dan 12 tester aktif 14 hari, yang memakan waktu lebih banyak daripada
  pembuktian intinya (membaca nota dan memeriksa hitungannya). APK di GitHub Releases cukup untuk
  demo dan bisa diunduh siapa pun yang membaca README.
- **Akibat:** App Check Play Integrity tidak berlaku untuk APK yang dipasang di luar Play, jadi
  backend dengan `APP_CHECK=enforce` akan menolak APK ini; perlindungan backend untuk APK masih
  terbuka (PRD 7 butir 5). Tanpa Play App Signing, kunci tanda tangan rilis adalah satu-satunya kunci
  aplikasi: bila hilang, pembaruan tidak bisa dipasang menimpa versi lama. README dan studi kasus
  tidak boleh memakai label "dipublikasikan di Play Store".
- **Cara menguji:** APK dari halaman rilis terpasang di HP yang tidak memasang dari Play, lalu
  membaca satu nota asli terhadap backend (RUNBOOK 6.4).

## 18. Lanjut ke evaluasi 50 nota
- **Keputusan (19 Okt 2026):** arah nota kulakan diteruskan; berikutnya evaluasi sekitar 50 nota
  (RUNBOOK Fase 5, `docs/EVALUASI.md`). Cadangan Pesanan WhatsApp (RUNBOOK 3.4) tidak dipakai.
- **Dasar:** D3 dengan `gemini-3.1-flash-lite` pada 6 nota toko (cetak, tulis, campuran): 5 sesuai
  dengan foto, 1 punya bagian bernilai null (tidak terbaca, bukan salah tebak). Nota 007, dua nota
  dalam satu foto, digabung menjadi satu nota tanpa peringatan; ini batasan yang dicatat, di luar
  hitungan 6 nota.
- **Batas bukti:** n=6, diperiksa satu orang (Dani) terhadap foto, dari draf pra-label. Ini cukup
  untuk memutuskan arah, **BUKAN angka akurasi untuk README**. Angka README hanya dari `--split uji`
  (aturan CLAUDE.md 7 dan 9).
- **Alternatif:** ganti arah ke Pesanan WhatsApp bila tulisan tangan tidak terbaca; tidak terjadi.
- **Cara menguji:** evaluasi set uji di Fase 5. Bila akurasi nota tulis jauh di bawah nota cetak,
  keputusan 1 dan 18 ditinjau ulang.
- **Catatan (19 Okt 2026):** hipotesis tulisan tangan belum bisa diuji dengan nota milik sendiri karena set utama cetak semua.
