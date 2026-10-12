# Runbook: dari zip sampai Play Store

Pemilik langkah: **[D]** = Dani (butuh akun, pembayaran, HP, atau keputusan), **[A]** = agen lokal
(Claude Code atau Antigravity, lewat `docs/HANDOFF-AGENT.md`), **[D+A]** = Dani mengizinkan, agen
menjalankan. Centang di sini atau di `docs/TASKS.md`.

> **Status 12 Okt 2026:** rilis ditunda (Firebase 2.3-2.4, Fase 4, Fase 6; lihat `docs/TASKS.md`).
> Gemini memakai **tier gratis** AI Studio tanpa billing, jadi 2.2 dilewati dan berlaku aturan data
> nota di 2.5.

Fase 1 dan 2 bisa berjalan paralel. Fase 2 (akun) sebaiknya dimulai hari ini karena verifikasi
identitas dan closed test 14 hari adalah jalur terlama, bukan coding.

---

## Fase 1. Repo dan build pertama

- [ ] 1.1 [D] Ekstrak `nota-kulakan.zip`. Isinya sudah repo git dengan riwayat commit.
- [ ] 1.2 [D] Buat repo kosong di GitHub (tanpa README), lalu dari folder repo:
  ```bash
  git remote add origin https://github.com/<akun>/nota-kulakan.git
  git push -u origin main
  ```
  Repo publik lebih berguna untuk portofolio; pastikan tidak ada `.env` atau foto (sudah di `.gitignore`).
- [ ] 1.3 [D] Pasang Android Studio stable terbaru, Python 3.11+, Git, dan Claude Code atau Antigravity.
- [ ] 1.4 [A] Tempel prompt di `docs/HANDOFF-AGENT.md`. Agen membuat build hijau dan menguji alur
  dengan backend tiruan di emulator. Kamu cukup membaca laporannya.
- [ ] 1.5 [D] Setelah push, cek tab Actions di GitHub: workflow `backend` dan `android` harus hijau.

## Fase 2. Akun (paralel dengan Fase 1)

- [ ] 2.1 [D] Daftar Google Play Console sebagai **akun personal**. Selesaikan verifikasi identitas.
- [ ] 2.2 [D] **(DILEWATI: memakai tier gratis tanpa billing.)** Buat satu project di Google Cloud
  Console, aktifkan billing, pasang **budget alert** (misalnya 5 USD per bulan) di Billing > Budgets &
  alerts. Baru diperlukan bila pindah ke tier berbayar atau deploy Cloud Run.
- [ ] 2.3 [D] Buka Firebase Console, "Add project", pilih project Google Cloud yang sama (wajib sama
  untuk Play Integrity). Tambahkan aplikasi Android dengan paket `com.danibanan.notakulakan`.
  Unduh `google-services.json`, taruh di folder `app/` (tidak di-commit).
  - Ganti paket sekarang bila ingin nama lain: `applicationId` dan `namespace` di `app/build.gradle.kts`.
    Setelah unggahan pertama ke Play, paket tidak bisa diganti.
- [ ] 2.4 [D] Di Firebase: aktifkan Crashlytics dan Analytics. Di App Check, daftarkan aplikasi dengan
  provider Play Integrity (SHA-256 diisi di langkah 6.5).
- [ ] 2.5 [D] Buat API key Gemini di Google AI Studio (tier gratis, tanpa billing).
  Salin `backend/.env.example` menjadi `backend/.env`, isi `GEMINI_API_KEY`.
  - Di tier gratis, Google boleh memakai isi permintaan untuk memperbaiki produknya, termasuk dibaca
    peninjau manusia (cek ketentuan Gemini API yang berlaku). Karena itu **aturan data nota**:
    Nota yang boleh dikirim ke Gemini hanya yang memuat nama toko, nama toko penerima, daftar barang,
    dan harga. Nota yang memuat nomor telepon, alamat pribadi, atau nomor rekening harus dipotong atau
    ditutup dulu (crop, atau tutup dengan kertas saat memfoto).
  - Tier gratis punya batas permintaan per menit dan per hari (lihat halaman rate limit di AI Studio
    untuk model yang dipakai). Harness evaluasi memakai `--jeda` dan menunggu otomatis saat kena 429.
- [ ] 2.6 [D] Kumpulkan 15-20 calon tester (HP Android, akun Google masing-masing). Pesan ajakan ada di
  `docs/RILIS.md` bagian 4.

## Fase 3. Tonggak penentu: Gemini membaca nota asli

- [ ] 3.1 [D] Foto 5 nota beragam (cetak jelas, cetak buram, tulis rapi, tulis berantakan, campuran)
  ke `data/private/toko/`. Ikuti aturan data nota di 2.5: potong atau tutup nomor telepon, alamat
  pribadi, dan nomor rekening sebelum foto dipakai.
- [ ] 3.2 [A] Buat `data/private/toko/manifest.jsonl` untuk 5 foto itu (format di `docs/EVALUASI.md`),
  lalu `cd backend && python -m nota_eval pralabel --manifest ../data/private/toko/manifest.jsonl --model gemini-3.1-flash-lite --jeda 10`.
  `--jeda` memberi jarak antar panggilan; saat kuota habis (429), harness menunggu 30, 60, lalu 120 detik
  (`--ulang-sibuk`, `--tunggu-sibuk`), dan berhenti tanpa menulis draf kosong bila tetap habis.
- [ ] 3.3 [D] Buka 5 file JSON hasil dan bandingkan dengan fotonya. Catat: berapa baris benar, angka
  mana yang salah, dan apakah validator menandai yang salah.
- [ ] 3.4 [D] Keputusan: lanjut, atau ganti arah bila nota tulisan tangan sama sekali tidak terbaca
  (cadangan: Pesanan WhatsApp). Kirim hasil 3.3 ke Claude untuk dibahas.

## Fase 4. Deploy backend ke Cloud Run

- [ ] 4.1 [D] Pasang Google Cloud CLI, lalu `gcloud auth login` dan `gcloud config set project <ID>`.
- [ ] 4.2 [D+A] Simpan kunci di Secret Manager, bukan di environment biasa:
  ```bash
  printf "%s" "<API_KEY>" | gcloud secrets create gemini-api-key --data-file=-
  ```
- [ ] 4.3 [D+A] Deploy (region Jakarta):
  ```bash
  gcloud run deploy nota-api --source backend --region asia-southeast2 \
    --allow-unauthenticated \
    --set-secrets GEMINI_API_KEY=gemini-api-key:latest \
    --set-env-vars NOTA_MODEL=gemini-3.1-flash-lite,APP_CHECK=enforce,APP_CHECK_CONSUME=true \
    --max-instances 2 --memory 512Mi --timeout 120
  ```
  `--allow-unauthenticated` diperlukan karena aplikasi memanggil langsung; perlindungannya App Check.
  `--max-instances 2` membatasi biaya terburuk bila ada lonjakan.
- [ ] 4.4 [D+A] Beri akun layanan Cloud Run peran **Secret Manager Secret Accessor** dan
  **Firebase App Check Token Verifier** (dibutuhkan untuk token sekali pakai). Bila verifikasi tetap
  gagal, set `APP_CHECK_CONSUME=false` sementara dan catat di README sebagai batasan.
- [ ] 4.5 [A] Uji: `curl <URL>/healthz` harus `ok`; `curl -F foto=@nota.jpg <URL>/v1/ekstrak` tanpa token
  harus 401 (bukti App Check aktif).
- [ ] 4.6 [D] Isi `local.properties` di akar repo:
  ```
  nota.api.url.release=<URL Cloud Run>/
  ```

## Fase 5. Evaluasi (angka untuk README)

Ikuti `docs/EVALUASI.md`. Ringkasnya:
- [ ] 5.1 [D] Foto sekitar 50 nota (cetak dan tulis) dengan aturan data nota di 2.5.
- [ ] 5.2 [A] Manifest, lalu `pralabel`.
- [ ] 5.3 [D] Periksa setiap angka draf terhadap foto, hapus baris `_status`. Ini satu-satunya pekerjaan
  evaluasi yang tidak bisa didelegasikan.
- [ ] 5.4 [A] `bagi --dev 15`, konversi CORD, `jalankan --split dev`, setel prompt bila perlu (naikkan `VERSI_PROMPT`).
- [ ] 5.5 [A] `jalankan --split uji` dengan 2 model dan 2 ukuran foto. Simpan `ringkasan.md`.
  Di tier gratis pakai `--jeda` agar tidak kena batas per menit; 2 model x 2 ukuran x n kasus bisa
  melewati batas harian, jadi bagi per model bila perlu.
- [ ] 5.6 [D] Putuskan model dan ukuran foto dari tabel, lalu set `NOTA_MODEL` di Cloud Run dan
  `FOTO_MAX_SISI` di `app/build.gradle.kts` bila berubah.

## Fase 6. Rilis ke Play Store

- [ ] 6.1 [D] Buat kunci upload (simpan file dan kata sandinya di tempat aman, di luar repo):
  ```bash
  keytool -genkeypair -v -keystore upload-nota.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
  ```
  Lalu buat `keystore.properties` di akar repo (tidak di-commit):
  ```
  storeFile=../upload-nota.jks
  storePassword=...
  keyAlias=upload
  keyPassword=...
  ```
- [ ] 6.2 [A] `./gradlew bundleRelease` -> `app/build/outputs/bundle/release/app-release.aab`.
- [ ] 6.3 [D] Host kebijakan privasi (`docs/KEBIJAKAN-PRIVASI.md`, isi email kontak) di GitHub Pages
  atau Google Sites, dapatkan URL publik.
- [ ] 6.4 [D] Play Console > buat aplikasi. Isi listing, Data Safety, rating konten, target audiens
  (18+), iklan: tidak ada. Semua jawaban ada di `docs/RILIS.md`.
- [ ] 6.5 [D] Unggah AAB ke **Internal testing** dulu. Di Play Console > Release > App integrity:
  tautkan project Cloud yang sama untuk Play Integrity, lalu salin SHA-256 **app signing key** ke
  Firebase App Check. Pasang dari Play di HP sendiri, uji satu nota asli.
- [ ] 6.6 [D] Buat **Closed testing**, tambahkan email tester (lebih mudah lewat Google Group), bagikan
  tautan opt-in. Hitungan 14 hari dimulai saat 12 tester sudah opt-in.
- [ ] 6.7 [D+A] Selama 14 hari: catat setiap masukan di `docs/LOG-CLOSED-TEST.md`, perbaiki bug, unggah
  versi baru (naikkan `versionCode`). Jangan biarkan tester keluar sebelum 14 hari.
- [ ] 6.8 [D] Ajukan akses produksi. Jawab kuesioner dari isi `docs/LOG-CLOSED-TEST.md`, bukan karangan.

## Fase 7. Studi kasus

- [ ] 7.1 [D] Ukur dengan stopwatch: 20 nota diinput manual (buku atau spreadsheet) vs lewat aplikasi.
- [ ] 7.2 [D] Bila pemilik toko memakai aplikasi: catat berapa nota dan berapa yang perlu dikoreksi
  (analitik `nota_disimpan` juga mencatat ini, tanpa isi nota).
- [ ] 7.3 [A] Isi README: tabel evaluasi uji, biaya per nota (tulis tanggal harga), hasil 7.1, batasan.
- [ ] 7.4 [D] Rekam video demo 30-60 detik, tautkan di README bersama tautan Play Store dan APK.
