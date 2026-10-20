# Protokol agen

Aturan tetap untuk setiap tugas agen. Dibaca sebelum mengerjakan berkas tugas
(`data/private/eval-hasil/_agen/TUGAS.md`). Aturan keras proyek ada di `CLAUDE.md`.

1. **Commit tanpa atribusi Claude/AI** (CLAUDE.md aturan 13). Sebelum push jalankan
   `git log origin/main..main --format="%h | %an | %s%n%b"`. Kalau ada Co-Authored-By, "Claude",
   atau "Generated with", BERHENTI dan laporkan. Push tanpa `--force`.
2. **Rahasia**: jangan mencetak isi `.env`, API key, atau header. Kunci tidak pernah masuk chat atau repo.
3. **Gemini tier gratis**, tanpa billing. Biaya di laporan hanya perkiraan harga daftar.
4. **Python**: pakai `backend/.venv/Scripts/python.exe`. Jangan `pip install` kecuali tugas menyuruh.
5. **Foto**: hanya versi tertutup dari `data/private/toko/` yang dikirim ke Gemini. Jangan menyentuh
   `data/private/asli/`.
6. **Split**: prompt hanya disetel dengan dev. Uji hanya dijalankan bila tugas menyebutnya. Hasil uji
   tidak mengubah prompt atau metrik. Konfigurasi tidak pernah diulang karena akurasinya rendah;
   hanya karena kegagalan layanan, dan diulang penuh dari awal.
7. **Skema, format API, applicationId, migrasi database**: BERHENTI dan tanya dulu (aturan 12).
8. **503 atau 429** setelah retry bawaan harness: BERHENTI dan laporkan (kode, pesan, nota yang
   sempat selesai). Tidak ada putaran tunggu tambahan.
9. **Tes dan lint** harus lulus sebelum commit kode:
   `cd backend && ruff check src tests && ruff format --check src tests && python -m pytest -q`.
   Jangan melemahkan atau menonaktifkan tes atau lint.
10. **Laporan**: tulis ke `data/private/eval-hasil/_agen/LAPORAN.md`, maksimal 30 baris selain tabel:
    yang dikerjakan, angka kunci, hal janggal, hash commit dan status CI. Tulis apa adanya;
    jangan merapikan hasil yang buruk dan jangan menyimpulkan lebih dari datanya.
