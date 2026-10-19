# Kebijakan Privasi Nota Kulakan

> DRAF. Bukan nasihat hukum. Sebelum dipublikasikan: isi bagian dalam kurung siku, cocokkan setiap
> pernyataan dengan konfigurasi yang benar-benar dipakai, dan baca ketentuan Gemini API serta Firebase
> yang berlaku saat itu.

Terakhir diperbarui: [TANGGAL]

Nota Kulakan ("aplikasi") dibuat oleh [NAMA PENGEMBANG] sebagai proyek portofolio. Kebijakan ini
menjelaskan data apa yang diproses aplikasi dan untuk apa.

## 1. Foto nota

- Foto nota yang Anda ambil atau pilih dikecilkan di HP, lalu dikirim lewat koneksi terenkripsi (HTTPS)
  ke server aplikasi di Google Cloud Run (region Jakarta).
- Server meneruskan foto ke layanan Google Gemini API untuk dibaca, lalu mengembalikan hasilnya ke HP.
- Server aplikasi **tidak menyimpan** foto dan tidak mencatat isinya di log. Log server hanya berisi
  angka teknis (jumlah baris, waktu proses, pemakaian token).
- [CATATAN 12 Okt 2026: pengembangan memakai tier gratis; kalimat berikut baru benar setelah pindah ke
  tier berbayar sebelum rilis. Lihat `docs/KEPUTUSAN.md` 16.]
- Pemrosesan oleh Gemini API tunduk pada ketentuan Google untuk layanan berbayar: [CEK DAN RINGKAS
  KETENTUAN PENGGUNAAN DATA DAN RETENSI GEMINI API YANG BERLAKU].
- Anda bisa memakai aplikasi tanpa mengirim foto dengan memilih "Isi manual".

## 2. Data yang disimpan di HP

- Isi nota, daftar produk, harga modal, dan foto nota disimpan di penyimpanan aplikasi di HP Anda.
- Database (tanpa foto) ikut dicadangkan ke akun Google Anda melalui fitur cadangan Android, bila fitur
  itu aktif di HP Anda. Foto tidak ikut cadangan cloud.
- Menghapus nota di aplikasi menghapus nota dan fotonya dari HP. Menghapus aplikasi menghapus semua data lokal.

## 3. Analitik dan laporan crash

Aplikasi memakai Firebase dari Google:
- **Firebase Analytics** mencatat peristiwa pemakaian berupa angka dan status saja, misalnya jumlah baris
  nota, berapa baris yang dikoreksi, dan lama pencatatan. Nama barang, harga, supplier, dan foto tidak
  pernah dikirim. Firebase memakai pengenal instalasi aplikasi.
- **Firebase Crashlytics** mengirim laporan saat aplikasi berhenti mendadak, berisi informasi teknis
  perangkat dan jejak galat.
- **Firebase App Check** dengan Play Integrity memeriksa bahwa permintaan berasal dari aplikasi asli,
  untuk mencegah penyalahgunaan server.

## 4. Berbagi data

Data tidak dijual dan tidak dibagikan untuk iklan. Data hanya diproses oleh penyedia layanan yang
disebut di atas (Google Cloud, Gemini API, Firebase) untuk menjalankan aplikasi.

## 5. Anak-anak

Aplikasi ditujukan untuk pemilik usaha dewasa dan tidak ditujukan untuk anak-anak.

## 6. Kontak

Pertanyaan atau permintaan terkait data: [EMAIL KONTAK].
