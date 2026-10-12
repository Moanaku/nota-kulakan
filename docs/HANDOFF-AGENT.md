# Prompt serah-terima untuk agen lokal

Kode di repo ini ditulis di lingkungan yang **tidak bisa** mengakses Google Maven, Maven Central,
Docker Hub, atau Gemini API. Akibatnya:

| Bagian | Sudah diverifikasi? |
|---|---|
| Backend Python (`backend/`) | Ya. 78 tes dan lint lolos. |
| Domain Kotlin murni (`app/.../domain/`) | Ya. Dikompilasi Kotlin 2.0.21, 19 tes JUnit lolos, termasuk vektor uji bersama. |
| Kode Android lain (UI, Room, Retrofit, Hilt) dan Gradle | Ya, 12 Okt 2026. Build, 30 tes JUnit, dan lint hijau; alur lengkap lolos di HP asli dengan backend tiruan (TASKS F6, F7). |
| Dockerfile | Belum pernah dibuild. **Ditunda** bersama deploy. |
| Panggilan Gemini sungguhan | Belum pernah dijalankan (TASKS D3, berikutnya). |

Prompt di bawah sudah dijalankan sekali dan selesai. Simpan untuk sejarah, atau jalankan ulang bila
lingkungan build berganti. Tempel prompt di bawah ke **Claude Code**
(terminal VS Code atau aplikasi desktop) atau ke **Antigravity**, dari folder akar repo. Agen
yang tidak otomatis membaca `CLAUDE.md` tetap akan membacanya karena prompt memintanya.

---

## Prompt (salin semua di dalam blok)

```text
Kamu melanjutkan proyek Nota Kulakan di repo ini. Baca dulu CLAUDE.md, docs/PRD.md,
docs/TASKS.md, dan docs/HANDOFF-AGENT.md. Ikuti aturan keras di CLAUDE.md.

Konteks: kode Android ditulis tanpa pernah dikompilasi (lingkungan penulis tidak bisa
mengakses Google Maven). Domain murni Kotlin dan backend Python sudah lolos tes.
Tugasmu: membuat semuanya terbuild, teruji, dan berjalan di emulator, TANPA mengubah perilaku.

Langkah:
1. Cek alat: `java -version` (17+), `./gradlew --version`, `python --version` (3.11+).
2. Backend:
   cd backend
   python -m venv .venv && source .venv/bin/activate   (Windows: .venv\Scripts\activate)
   pip install -r requirements-dev.txt && pip install -e . --no-deps
   ruff check src tests && python -m pytest -q
   Semua harus lolos sebelum lanjut.
3. Android: jalankan `./gradlew assembleDebug testDebugUnitTest lintDebug`.
   - Bila sync gagal karena versi tidak ditemukan: perbarui HANYA versi yang disebut galat di
     gradle/libs.versions.toml ke versi stabil terdekat yang ada. Jaga kecocokan: plugin
     kotlin-compose = versi Kotlin; versi KSP harus berawalan versi Kotlin; Hilt plugin =
     library Hilt. Jangan naik ke AGP 9 atau Kotlin 2.4 di langkah ini.
   - Perbaiki galat kompilasi dengan perubahan sekecil mungkin. Jangan mengubah logika di
     app/src/main/java/.../domain/ maupun nilai harapan di shared/test-vectors.json; dua hal itu
     sudah teruji dan menjadi kontrak dengan backend.
   - Jangan menonaktifkan tes, menurunkan aturan lint, atau menambah @Suppress tanpa alasan
     yang ditulis di komentar.
   - Ulangi sampai ketiga task hijau. Room akan membuat app/schemas/; commit folder itu.
4. Uji ujung ke ujung tanpa API key:
   Terminal 1: cd backend && NOTA_KLIEN=tiruan APP_CHECK=off uvicorn nota.api:app --port 8080
     (Windows PowerShell: $env:NOTA_KLIEN="tiruan"; $env:APP_CHECK="off"; uvicorn nota.api:app --port 8080)
   Terminal 2: jalankan aplikasi debug di emulator (URL bawaan debug: http://10.0.2.2:8080/).
   Lakukan: Catat nota -> pilih foto apa saja dari galeri -> layar Periksa nota harus menampilkan
   4 baris contoh, baris "Gula Pasir 1kg" merah (2 x Rp 60 = Rp 120, tertulis Rp 120.000) dan
   baris "Air Mineral" bertanda harga dihitung -> perbaiki harga gula menjadi 60.000 -> status
   berubah hijau -> Simpan -> layar Tersimpan -> tab Stok menampilkan 4 produk -> simpan foto yang
   sama sekali lagi -> muncul dialog "Nota ini mungkin sudah dicatat".
   Juga uji: matikan backend lalu catat nota -> layar gagal "tidak ada koneksi"/"gangguan
   server" dengan tombol Coba lagi; dan "Isi manual tanpa foto".
5. Laporkan dalam format:
   - Perubahan versi dependensi (lama -> baru, alasan)
   - Perbaikan kompilasi (file, masalah, perbaikan)
   - Hasil langkah 4, per poin: lolos / gagal (+ tangkapan layar bila gagal)
   - Hal yang kamu ragukan dan BELUM kamu ubah
6. Commit per kelompok perubahan dengan pesan yang jelas. Jangan commit .env, google-services.json,
   keystore, local.properties, atau foto di data/private/.

Jangan mengerjakan Lapisan 2 (lihat docs/PRD.md bagian 3) dan jangan menambah fitur.
Tanyakan dulu sebelum mengubah applicationId, skema database, atau format API.
```

---

## Menjalankan lagi (HP asli lewat USB, backend lokal)

Emulator di laptop Dani tidak stabil (mati sendiri setelah 1-2 menit), jadi uji dilakukan di HP lewat
kabel USB. HP memanggil `127.0.0.1:8080`, lalu `adb reverse` meneruskannya ke laptop.

1. Sekali saja: tambahkan ke `local.properties` di akar repo (tidak di-commit):
   ```
   nota.api.url.debug=http://127.0.0.1:8080/
   ```
   `127.0.0.1` sudah diizinkan cleartext di `app/src/debug/res/xml/network_security_config.xml`.
   Hapus baris ini bila kembali memakai emulator (bawaannya `http://10.0.2.2:8080/`).
2. Backend (PowerShell, dari `backend/`, venv aktif):
   - Tiruan, tanpa API key:
     `$env:NOTA_KLIEN="tiruan"; $env:APP_CHECK="off"; uvicorn nota.api:app --port 8080`
   - Gemini sungguhan (butuh `backend/.env` berisi `GEMINI_API_KEY`, lihat TASKS C3):
     `uvicorn nota.api:app --port 8080 --env-file .env`
     Server API tidak membaca `.env` sendiri; `--env-file` yang memuatnya. Pastikan `APP_CHECK=off`
     di `.env` untuk uji lokal.
3. HP: colokkan USB, izinkan USB debugging, lalu dari akar repo:
   ```
   adb devices                      # HP harus berstatus "device"
   adb reverse tcp:8080 tcp:8080    # ulangi setiap kabel dicabut atau HP "offline"
   ./gradlew installDebug
   ```
   Bila `adb devices` menampilkan HP dan emulator sekaligus, pakai `adb -s <serial>`.
4. Bila `testDebugUnitTest` gagal dengan `Could not find or load main class VS`: ada entri PATH
   Windows bertanda kutip (`"X:\...\Microsoft VS Code\bin`). Hapus tanda kutipnya di Environment
   Variables, lalu `./gradlew --stop` supaya daemon memakai PATH baru.

## Setelah prompt di atas selesai

**Rilis ditunda** (keputusan Dani, 12 Okt 2026). Jangan kerjakan Firebase (RUNBOOK 2.3-2.4), deploy
Cloud Run (Fase 4), maupun Play Store (Fase 6) sampai Dani membukanya lagi; di TASKS bertanda (DITUNDA).

Gemini memakai **tier gratis** AI Studio tanpa billing (RUNBOOK 2.2 dilewati). Aturan data nota:
  Nota yang boleh dikirim ke Gemini hanya yang memuat nama toko, nama toko penerima, daftar barang,
  dan harga. Nota yang memuat nomor telepon, alamat pribadi, atau nomor rekening harus dipotong atau
  ditutup dulu (crop, atau tutup dengan kertas saat memfoto).
Jangan mengirim foto yang belum memenuhi aturan ini, termasuk lewat aplikasi ke backend lokal.

Yang berikutnya adalah membaca nota asli secara lokal: RUNBOOK 2.5 (API key), lalu Fase 3 (D3), lalu
Fase 5 (evaluasi). Pakai `--jeda` di `pralabel` dan `jalankan` karena kuota tier gratis terbatas.
Minta agen mengikuti langkah demi langkah dan
berhenti di setiap langkah yang membutuhkan akun, pembayaran, atau persetujuanmu.
