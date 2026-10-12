# Daftar tugas Nota Kulakan

Pemilik: **C** = Claude (sesi cloud), **A** = agen lokal (Claude Code / Antigravity di laptop),
**D** = Dani. Centang hanya bila kriteria "selesai kalau" terpenuhi.
Langkah operasional rinci (perintah, urutan) ada di `docs/RUNBOOK.md`.

> **Status 12 Okt 2026: rilis ditunda.** Fokus sekarang membaca nota asli secara lokal (D3, lalu E).
> Tugas bertanda **(DITUNDA)** adalah jalur rilis Play Store (Firebase, Cloud Run, Play Console,
> tester) dan tidak dikerjakan sampai Dani membukanya lagi. Cara menjalankan aplikasi di HP terhadap
> backend lokal ada di `docs/HANDOFF-AGENT.md` bagian "Menjalankan lagi".

## A. Dokumen dan fondasi
- [x] A1 (C) Kerangka repo, CLAUDE.md, parser rupiah dan qty, skema, validator.
- [x] A2 (C) PRD, daftar tugas, catatan keputusan, runbook, bahan rilis.
- [ ] A3 (D) Tinjau `backend/src/nota/schema.py`, `docs/EVALUASI.md`, dan `docs/KEPUTUSAN.md`.
  Selesai kalau: setiap keputusan bisa dijelaskan dengan kata sendiri, atau revisinya dikirim.
- [x] A4 (C) Unit test backend (78) dan domain Kotlin (19), termasuk vektor uji bersama. Lolos di
  lingkungan Claude. **Perubahan kesepakatan:** test ditulis Claude atas permintaan Dani; Dani
  meninjau dan wajib memahaminya.

## B. Prototipe
- [x] B1 (C) Prototipe 5 layar (kanvas Design).
- [x] B2 (D) Prototipe disetujui ("cocok").

## C. Akun dan data (Dani, paralel)
- [ ] C1 (D) **(DITUNDA)** Play Console personal + verifikasi identitas. (RUNBOOK 2.1)
- [ ] C2 (D) Project Google Cloud dengan billing dan budget alert (2.2), wajib untuk C3.
  Bagian Firebase (2.3-2.4) **(DITUNDA)**.
- [ ] C3 (D) API key Gemini di `backend/.env` (tidak di-commit). (2.5)
- [ ] C4 (D) **(DITUNDA)** 15-20 calon tester. (2.6)
- [ ] C5 (D) 5 foto nota beragam, lalu ~50 untuk evaluasi, di `data/private/toko/`. (3.1, 5.1)
- [ ] C6 (D) Android Studio, Python 3.11+, Git, Claude Code/Antigravity terpasang. (1.3)
- [ ] C7 (D) Repo di GitHub, CI hijau. (1.2, 1.5)

## D. Backend ekstraksi
- [x] D1 (C) Prompt, skema keluaran, klien Gemini (timeout, retry SDK, satu ulang untuk JSON rusak),
  pemetaan ke `Nota`, biaya dari token. Diuji dengan klien palsu.
- [x] D2 (C) Endpoint `/v1/ekstrak`, App Check sekali pakai, kode galat, mode tiruan, Dockerfile.
- [ ] D3 (A) Uji panggilan Gemini sungguhan pada 5 nota. Selesai kalau: JSON tiap nota dibandingkan
  dengan fotonya oleh Dani. (RUNBOOK Fase 3)
- [ ] D4 (D+A) **(DITUNDA)** Deploy Cloud Run + Secret Manager + peran IAM. Selesai kalau: `/healthz` ok dan
  permintaan tanpa token ditolak 401. (Fase 4)

## E. Evaluasi
- [x] E1 (C) Rancangan format kebenaran, metrik, aturan split (`docs/EVALUASI.md`). Menunggu tinjauan A3.
- [ ] E2 (D) Label kebenaran nota toko (lewat pra-label, diperiksa manual). CORD dikonversi otomatis.
- [x] E3 (C) Harness: `bagi`, `pralabel`, `cord`, `jalankan` dengan tabel akurasi, tangkapan validator,
  alarm palsu, biaya, latensi, interval Wilson.
- [ ] E4 (A) Jalankan dev untuk menyetel, lalu uji dengan 2 model x 2 ukuran foto. Selesai kalau:
  `ringkasan.md` uji masuk README dan konfigurasi dipilih. (Fase 5)

## F. Aplikasi Android Lapisan 1
- [x] F1 (C) Gradle (wrapper asli 8.14.3), version catalog, Hilt, tema, navigasi bertipe, CI.
- [x] F2 (C) Ambil foto (kamera sistem, Photo Picker) dan kompres dengan koreksi EXIF.
- [x] F3 (C) Klien backend dengan App Check, layar memproses, layar gagal per jenis kegagalan, isi manual.
- [x] F4 (C) Layar periksa dan koreksi, validasi lokal seketika, aturan simpan, pemetaan produk + alias,
  deteksi nota ganda.
- [x] F5 (C) Room (stok dari SUM baris), riwayat, detail, hapus nota, tampilan selisih harga modal.
- [x] F6 (A) **Build pertama.** Kode Android belum pernah dikompilasi. Selesai kalau:
  `./gradlew assembleDebug testDebugUnitTest lintDebug` hijau. (`docs/HANDOFF-AGENT.md`)
- [x] F7 (A) Uji ujung ke ujung di emulator dengan backend tiruan, sesuai skenario di HANDOFF.
  Dijalankan di HP asli (Android 14, API 34) lewat `adb reverse tcp:8080 tcp:8080` dan
  `nota.api.url.debug=http://127.0.0.1:8080/` di `local.properties`, karena emulator tidak stabil di laptop.
  Semua poin lolos setelah perbaikan `FotoKompresor` (semua foto sebelumnya ditolak).
- [ ] F8 (D) Uji di HP asli dengan nota dari laci terhadap backend Cloud Run. **(DITUNDA)** bersama D4;
  sementara itu nota asli bisa diuji lewat backend lokal (`NOTA_KLIEN=gemini`) dengan cara di HANDOFF. Selesai kalau: daftar bug dikirim.

## G. Rilis
- [x] G1 (C) Signing dari `keystore.properties`, R8, target SDK 36, Crashlytics/Analytics bersyarat,
  draf kebijakan privasi, Data Safety, listing, pesan tester, panduan kuesioner.
- [ ] G2 (D) **(DITUNDA)** Kunci upload, kebijakan privasi di URL publik, formulir Play. (Fase 6)
- [ ] G3 (D) **(DITUNDA)** Internal testing, tautan Play Integrity, SHA-256 ke Firebase App Check. (6.5)
- [ ] G4 (D) **(DITUNDA)** Closed test 12+ tester selama 14 hari berturut-turut, isi `docs/LOG-CLOSED-TEST.md`.
- [ ] G5 (D+A) **(DITUNDA)** Perbaiki masukan tester, ajukan akses produksi.

## H. Studi kasus
- [ ] H1 (D) Ukur waktu dan kesalahan: 20 nota manual vs aplikasi.
- [ ] H2 (A+D) Isi semua [BELUM DIUKUR] di README dengan angka bersumber, video demo, tautan.
