# Runbook: dari zip sampai rilis APK

Pemilik langkah: **[D]** = Dani (butuh akun, pembayaran, HP, atau keputusan), **[A]** = agen lokal
(Claude Code atau Antigravity, lewat `docs/HANDOFF-AGENT.md`), **[D+A]** = Dani mengizinkan, agen
menjalankan. Centang di sini atau di `docs/TASKS.md`.

> **Status 19 Okt 2026:** tidak ada rilis ke Play Store. APK dibagikan lewat GitHub Releases
> (Fase 6, `docs/KEPUTUSAN.md` 17). Firebase 2.3-2.4 dan Fase 4 masih ditunda (lihat `docs/TASKS.md`).
> Gemini memakai **tier gratis** AI Studio tanpa billing, jadi 2.2 dilewati dan berlaku aturan data
> nota di 2.5. Fase 3 selesai.

Fase 1 dan 2 bisa berjalan paralel.

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

- [ ] 2.2 [D] **(DILEWATI: memakai tier gratis tanpa billing.)** Buat satu project di Google Cloud
  Console, aktifkan billing, pasang **budget alert** (misalnya 5 USD per bulan) di Billing > Budgets &
  alerts. Baru diperlukan bila pindah ke tier berbayar atau deploy Cloud Run.
- [ ] 2.3 [D] Buka Firebase Console, "Add project", pilih project Google Cloud yang sama (wajib sama
  untuk Play Integrity). Tambahkan aplikasi Android dengan paket `com.danibanan.notakulakan`.
  Unduh `google-services.json`, taruh di folder `app/` (tidak di-commit).
  - Ganti paket sekarang bila ingin nama lain: `applicationId` dan `namespace` di `app/build.gradle.kts`.
    Setelah APK pertama dibagikan, paket jangan diganti: APK dengan paket lain terpasang sebagai aplikasi terpisah.
- [ ] 2.4 [D] Di Firebase: aktifkan Crashlytics dan Analytics. App Check Play Integrity tidak berlaku
  untuk APK di luar Play (`docs/KEPUTUSAN.md` 17); cara melindungi backend untuk APK belum diputuskan.
- [ ] 2.5 [D] Pakai API key Gemini **tier gratis** dari Google AI Studio (tanpa billing), dan tutup data
  pribadi di foto sebelum dikirim.
  - Salin `backend/.env.example` menjadi `backend/.env`, isi `GEMINI_API_KEY` (tidak di-commit).
  - **Aturan data nota:** di tier gratis Google boleh memakai isi permintaan untuk memperbaiki produknya,
    termasuk dibaca peninjau manusia (cek ketentuan Gemini API yang berlaku). Nota yang boleh dikirim
    hanya yang memuat nama toko, nama toko penerima, daftar barang, dan harga. Nomor telepon, alamat
    pribadi, dan nomor rekening dipotong atau ditutup dulu (crop, atau tutup dengan kertas saat memfoto).
  - Tier gratis punya batas permintaan per menit dan per hari (lihat halaman rate limit di AI Studio
    untuk model yang dipakai). Harness evaluasi memakai `--jeda` dan menunggu otomatis saat kena 429.

## Fase 3. Tonggak penentu: Gemini membaca nota asli (SELESAI, 19 Okt 2026)

> **Hasil D3:** 6 nota diuji dengan `gemini-3.1-flash-lite`; 5 sesuai dan 1 punya bagian bernilai null
> (diperiksa Dani terhadap foto). Nota 007 (dua nota dalam satu foto, uji ketahanan, di luar angka
> akurasi) digabung menjadi satu nota tanpa peringatan; dicatat sebagai batasan. Manifest:
> `data/private/toko/manifest.jsonl` (001-006) dan `manifest-ketahanan.jsonl` (007).

- [x] 3.1 [D] Foto 5 nota beragam (cetak jelas, cetak buram, tulis rapi, tulis berantakan, campuran)
  ke `data/private/toko/`. Ikuti aturan data nota di 2.5: potong atau tutup nomor telepon, alamat
  pribadi, dan nomor rekening sebelum foto dipakai.
- [x] 3.2 [A] Buat `data/private/toko/manifest.jsonl` untuk 5 foto itu (format di `docs/EVALUASI.md`),
  lalu `cd backend && python -m nota_eval pralabel --manifest ../data/private/toko/manifest.jsonl --model gemini-3.1-flash-lite --jeda 10`.
  `--jeda` memberi jarak antar panggilan; saat kuota habis (429), harness menunggu 30, 60, lalu 120 detik
  (`--ulang-sibuk`, `--tunggu-sibuk`), dan berhenti tanpa menulis draf kosong bila tetap habis.
- [x] 3.3 [D] Buka 5 file JSON hasil dan bandingkan dengan fotonya. Catat: berapa baris benar, angka
  mana yang salah, dan apakah validator menandai yang salah.
- [x] 3.4 [D] Keputusan: lanjut, atau ganti arah bila nota tulisan tangan sama sekali tidak terbaca
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

## Fase 6. Rilis APK lewat GitHub Releases

Tidak ada Play Store, tester, atau closed test (`docs/KEPUTUSAN.md` 17). Prasyarat: backend yang bisa
dihubungi APK (Fase 4) dan `nota.api.url.release` di `local.properties`; tanpa itu build rilis memakai
`https://contoh.invalid/`.

- [ ] 6.1 [D] Buat kunci tanda tangan. Tanpa Play App Signing, kunci ini **satu-satunya** kunci aplikasi:
  bila hilang, versi baru tidak bisa dipasang menimpa versi lama. Simpan file dan kata sandinya di
  tempat aman, di luar repo:
  ```bash
  keytool -genkeypair -v -keystore nota-rilis.jks -keyalg RSA -keysize 2048 -validity 10000 -alias rilis
  ```
  Lalu buat `keystore.properties` di akar repo (jangan di-commit; pastikan `git status` tidak
  menampilkannya):
  ```
  storeFile=../nota-rilis.jks
  storePassword=...
  keyAlias=rilis
  keyPassword=...
  ```
- [ ] 6.2 [A] Naikkan `versionCode` dan `versionName` di `app/build.gradle.kts` untuk setiap rilis, lalu
  `./gradlew assembleRelease` -> `app/build/outputs/apk/release/app-release.apk`.
  Cek tanda tangannya: `apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk`.
- [ ] 6.3 [D+A] Buat rilis di GitHub dengan APK sebagai lampiran:
  ```bash
  gh release create v0.1.0 app/build/outputs/apk/release/app-release.apk#nota-kulakan-0.1.0.apk     --title "Nota Kulakan 0.1.0" --notes "<perubahan, batasan yang diketahui, cara pasang>"
  ```
  Catatan rilis menyebut: foto nota dikirim ke server dan Gemini, aturan data nota (2.5), dan bahwa
  pemasangan butuh izin "pasang aplikasi dari sumber tidak dikenal".
- [ ] 6.4 [D] Unduh APK dari halaman rilis ke HP sendiri, pasang, uji satu nota asli.

## Fase 7. Studi kasus

- [ ] 7.1 [D] Ukur dengan stopwatch: 20 nota diinput manual (buku atau spreadsheet) vs lewat aplikasi.
- [ ] 7.2 [D] Bila pemilik toko memakai aplikasi: catat berapa nota dan berapa yang perlu dikoreksi
  (analitik `nota_disimpan` juga mencatat ini, tanpa isi nota).
- [ ] 7.3 [A] Isi README: tabel evaluasi uji, biaya per nota (tulis tanggal harga), hasil 7.1, batasan.
- [ ] 7.4 [D] Rekam video demo 30-60 detik, tautkan di README bersama tautan rilis APK di GitHub Releases.
