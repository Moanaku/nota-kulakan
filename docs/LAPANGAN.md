# Hari lapangan: langkah demi langkah

Protokol pengukuran ada di `docs/EVALUASI.md` bagian 13. Dokumen ini hanya langkah operasional.
Konfigurasi: `gemini-3.1-flash-lite`, foto 1024 px (`docs/KEPUTUSAN.md` 19). Semua perintah dari akar repo, PowerShell.

## 1. Sebelum berangkat (di rumah)
1. `backend/.env` berisi `GEMINI_API_KEY` (tidak di-commit; jangan ditampilkan atau difoto layar).
2. HP: opsi pengembang + debugging USB aktif, kabel tersambung, `adb devices` menampilkan status `device`.
3. `.\scripts\lapangan.ps1 pasang`: membangun aplikasi debug yang menunjuk ke `http://localhost:8080/` dan memasangnya.
   (Bawaan debug `10.0.2.2` hanya untuk emulator; di HP asli alamat itu tidak terjangkau.)
   Hari lapangan memakai build debug ini (server ada), BUKAN mode contoh: nota contoh diberi supplier
   berawalan "(Contoh) " dan tidak boleh ikut terhitung. Bila sempat mencoba mode contoh, hapus lewat Riwayat >
   "Hapus data contoh" sebelum mencatat nota sungguhan.
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

## 4. Mengisi lembar
Satu baris per nota di lembar CSV (Excel atau editor teks, simpan sebagai CSV). Kolom sama dengan
[`hasil/lapangan-2026-10-29.csv`](hasil/lapangan-2026-10-29.csv):

| Kolom | Isi |
|---|---|
| `kode_nota` | kode nota (N01, N02, ...), bukan nama toko |
| `jenis` | cetak / tulis tangan |
| `jumlah_baris` | jumlah baris barang di nota |
| `detik_input` | stopwatch: ketuk "Catat nota" sampai "Simpan nota ke stok" selesai (termasuk foto, menunggu AI, memeriksa, mengoreksi) |
| `baris_dikoreksi` | baris yang diperbaiki sebelum disimpan |
| `validator_menandai` | JUMLAH baris yang ditandai validator (bukan ya/tidak) |
| `salah_lolos` | angka salah pada data yang tersimpan |
| `catatan` | hal janggal (galat, server lambat, foto buram) |

Isi segera setelah tiap nota, bukan di akhir hari. Pembanding "menulis ulang di buku" tidak dipakai lagi
(`EVALUASI.md` bagian 13, catatan revisi), jadi tidak ada kolom waktu cara lama.

## 5. Selesai: menghentikan dan membersihkan
1. Ctrl+C di jendela backend.
2. `.\scripts\lapangan.ps1 bersih`: mencabut `adb reverse` dan menghapus data aplikasi (nota, foto di dalam aplikasi).
3. Hapus foto asli dari HP: galeri, album Kamera, lalu "Sampah" galeri/Google Foto. Foto tidak boleh masuk repo.
4. `adb shell ls /sdcard/DCIM/Camera` untuk memastikan tidak ada yang tersisa; hapus yang perlu dengan
   `adb shell rm /sdcard/DCIM/Camera/<nama>`.
5. Nonaktifkan debugging USB bila tidak dipakai lagi.

## 6. Waktu menemukan kembali (belum diukur)

Belum ada angka. Pengukuran ini menguji fitur cari nota di Riwayat, dan baru dilakukan setelah protokol ini ditulis:

1. Nota fisik di tumpukan dan nota di aplikasi harus memuat nota yang **SAMA** (nota yang sama difoto dan dicatat).
2. 10 pertanyaan ditulis **SEBELUM** mengukur, mis. "kapan terakhir beli susu dari toko X dan berapa harganya".
   Daftar pertanyaan tidak diubah sesudah pengukuran dimulai.
3. Urutan bergantian per pertanyaan: pertanyaan ganjil aplikasi dulu, genap tumpukan dulu (atau sebaliknya,
   tetapi dicatat dan konsisten). Tiap pertanyaan dikerjakan dengan kedua cara.
4. Stopwatch Dani sendiri: dari pertanyaan dibacakan sampai jawaban benar ditemukan (dibandingkan dengan nota fisik).
5. Dilaporkan: jumlah pertanyaan, median dan rata-rata detik tiap cara, pertanyaan yang tidak ketemu, dan
   keterbatasannya (satu pengguna, nota yang itu-itu saja, belajar dari urutan). Tanpa klaim hemat waktu
   di luar angka yang terukur.
