# Nota Kulakan

Aplikasi Android: foto nota grosir -> item & total dibaca AI -> diperiksa aturan hitungan -> stok & harga modal tercatat.
Studi kasus produk dan rekayasa, bukan pesaing aplikasi kasir.

## UI
- UI: ikuti `docs/DESIGN.md`; tidak ada nilai warna/jarak/ukuran teks di luar token (`ui/theme`).

## Dokumen
- Spesifikasi `docs/PRD.md`, tugas dan pemilik `docs/TASKS.md`, langkah operasional `docs/RUNBOOK.md`,
  alasan keputusan `docs/KEPUTUSAN.md`, evaluasi `docs/EVALUASI.md`, aturan tetap agen `docs/PROTOKOL-AGEN.md`. Baca PRD dan TASKS sebelum mengerjakan tugas;
  centang tugas hanya bila kriteria selesainya terpenuhi.

## Stack
- Backend: Python 3.11+ (FastAPI, google-genai, firebase-admin) di `backend/src/nota`, evaluasi di `backend/src/nota_eval`. Deploy Cloud Run.
- Android: Kotlin, Jetpack Compose, MVVM + Clean Architecture (domain/data/ui), Hilt, Room, Retrofit + kotlinx.serialization, minSdk 26, targetSdk 36.
- Firebase (App Check Play Integrity, Crashlytics, Analytics) hanya aktif bila `app/google-services.json` ada.

## Perintah
- Backend: `cd backend && pip install -r requirements-dev.txt && pip install -e . --no-deps`
- Tes + lint backend: `cd backend && ruff check src tests && ruff format --check src tests && python -m pytest -q`
- Backend lokal tanpa API key: `cd backend && NOTA_KLIEN=tiruan APP_CHECK=off uvicorn nota.api:app --port 8080`
- Android: `./gradlew assembleDebug testDebugUnitTest lintDebug` (emulator memanggil `http://10.0.2.2:8080/`)
- Evaluasi: `cd backend && python -m nota_eval --help`

## Aturan keras
1. Tidak ada API key, `.env`, `google-services.json`, keystore, atau `local.properties` di repo.
2. Uang = `int`/`Long` rupiah, tidak pernah float. Qty = `Decimal`/`BigDecimal`. Stok dihitung dari baris nota, tidak disimpan.
3. `validator.py` dan `domain/` Kotlin murni: tanpa jaringan, AI, atau I/O.
4. Aturan hitungan: Python acuan, Kotlin cermin. Setiap perubahan di salah satu wajib menambah kasus di `shared/test-vectors.json` (hasil harapan ditulis tangan) dan lolos di Pytest DAN JUnit.
5. Hasil AI tidak masuk stok sebelum lolos validator atau dikonfirmasi pengguna (lihat `statusSimpan`). Data AI yang buruk = daftar isu, bukan exception.
6. Model menyalin, kode menafsirkan: angka dari model berupa teks, ditafsirkan `parse_rupiah`/`parse_qty`.
7. Setiap perubahan prompt menaikkan `VERSI_PROMPT`; prompt hanya disetel dengan `--split dev`; angka README hanya dari `--split uji`.
8. Test ditulis Claude, ditinjau Dani. Jangan menonaktifkan tes atau melemahkan lint agar build hijau.
9. README hanya berisi angka terukur dan bersumber. Data sintetis diberi label.
10. Foto nota asli tidak di-commit (`data/private/`). Foto evaluasi ditutup kotak hitam (nama, alamat, HP, NPWP,
    rekening pembeli) sebelum dikirim ke Gemini; foto asli ada di `data/private/asli/` dan tidak dikirim.
    Kode tidak menulis foto ke disk dan log tidak memuat isi nota; unggahan multipart sementara dapat ditampung
    framework di berkas sementara bila > 1 MB.
11. Jangan mulai Lapisan 2 (`docs/PRD.md` 7b) sebelum Lapisan 1 berjalan, terukur, dan terdokumentasi.
12. Tanya dulu sebelum mengubah `applicationId`, skema database (butuh migrasi), atau format API.
13. Pesan commit dan deskripsi PR tanpa atribusi Claude/AI (tanpa Co-Authored-By, tanpa tautan sesi).
