# Hari lapangan: langkah demi langkah

Protokol pengukuran ada di `docs/EVALUASI.md` bagian 13. Dokumen ini hanya langkah operasional.
Konfigurasi: `gemini-3.1-flash-lite`, foto 1024 px (`docs/KEPUTUSAN.md` 19). Semua perintah dari akar repo, PowerShell.

## 1. Sebelum berangkat (di rumah)
1. `backend/.env` berisi `GEMINI_API_KEY` (tidak di-commit; jangan ditampilkan atau difoto layar).
2. HP: opsi pengembang + debugging USB aktif, kabel tersambung, `adb devices` menampilkan status `device`.
3. `.\scripts\lapangan.ps1 pasang`: membangun aplikasi debug yang menunjuk ke `http://localhost:8080/` dan memasangnya.
   (Bawaan debug `10.0.2.2` hanya untuk emulator; di HP asli alamat itu tidak terjangkau.)
4. Siapkan lembar: `data/private/eval-hasil/lapangan/lembar-lapangan.csv` sudah berisi header.

## 2. Menjalankan backend asli
```powershell
.\scripts\lapangan.ps1 mulai
```
Skrip membaca `backend/.env` (tanpa mencetak isinya), menyetel `NOTA_KLIEN=gemini` dan `APP_CHECK=off`,
menjalankan `adb reverse tcp:8080 tcp:8080`, lalu menjalankan uvicorn di port 8080. Biarkan jendela terbuka.
Tanpa skrip: setel variabel dari `.env` ke environment sendiri, `adb reverse tcp:8080 tcp:8080`, lalu
`cd backend; .\.venv\Scripts\python.exe -m uvicorn nota.api:app --port 8080`.
Periksa dari terminal lain: `curl.exe http://127.0.0.1:8080/healthz`.

## 3. Daftar periksa di toko
- [ ] Izin penjual didapat sebelum memotret.
- [ ] Nomor telepon, alamat pribadi, nomor rekening, dan NPWP ditutup (kertas atau jari) sebelum difoto.
- [ ] Toko dicatat dengan kode (T1, T2, ...), bukan nama.
- [ ] Foto lewat kamera aplikasi atau galeri; foto asli tidak dibagikan ke aplikasi lain.
- [ ] Urutan berpasangan: nota ganjil cara lama dulu, genap aplikasi dulu.

## 4. Mengisi lembar
Satu baris per nota di `lembar-lapangan.csv` (Excel atau editor teks, simpan sebagai CSV):

| Kolom | Isi |
|---|---|
| `id` | nomor urut nota (1, 2, ...) |
| `kode_toko` | T1, T2, ... |
| `jenis` | cetak / tulis / campuran |
| `jumlah_baris` | jumlah baris barang di nota |
| `urutan` | `manual-dulu` atau `app-dulu` |
| `detik_manual` | stopwatch: nota di tangan sampai semua baris dan total terketik |
| `detik_aplikasi` | stopwatch: ketuk "Catat nota" sampai "Simpan nota ke stok" selesai |
| `salah_manual` | jumlah angka salah pada ketikan cara lama (banding dengan nota fisik) |
| `baris_dikoreksi_di_app` | baris yang diperbaiki sebelum disimpan |
| `salah_lolos_di_app` | angka salah pada data yang tersimpan |
| `validator_menandai` | ya / tidak |
| `catatan` | hal janggal (galat, waktu tunggu lama, foto buram) |

Isi segera setelah tiap nota, bukan di akhir hari.

## 5. Selesai: menghentikan dan membersihkan
1. Ctrl+C di jendela backend.
2. `.\scripts\lapangan.ps1 bersih`: mencabut `adb reverse` dan menghapus data aplikasi (nota, foto di dalam aplikasi).
3. Hapus foto asli dari HP: galeri, album Kamera, lalu "Sampah" galeri/Google Foto. Foto tidak boleh masuk repo.
4. `adb shell ls /sdcard/DCIM/Camera` untuk memastikan tidak ada yang tersisa; hapus yang perlu dengan
   `adb shell rm /sdcard/DCIM/Camera/<nama>`.
5. Nonaktifkan debugging USB bila tidak dipakai lagi.
