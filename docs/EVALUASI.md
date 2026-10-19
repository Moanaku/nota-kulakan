# Evaluasi ekstraksi

Status: rancangan dari Claude, **menunggu tinjauan Dani**. Dani wajib bisa menjelaskan setiap
definisi di bawah saat interview; ubah yang tidak disetujui sebelum angka pertama diambil.

## 1. Pertanyaan yang dijawab

1. Seberapa sering ekstraksi menghasilkan nota yang benar seluruhnya, per jenis nota?
2. Kalau salah, seberapa sering validator menandainya (dan seberapa sering alarm palsu)?
3. Berapa biaya, latensi, dan ukuran data per nota untuk tiap konfigurasi?
4. Apakah model lebih murah atau foto lebih kecil masih cukup akurat?

## 2. Data

| Set | Isi | Sumber | Lisensi / izin |
|---|---|---|---|
| toko | ~50 nota kulakan dari toko pengguna, cetak dan tulis | difoto Dani | data pribadi; foto tidak di-commit, tidak dipublikasikan; telepon, alamat pribadi, dan rekening ditutup sebelum dikirim ke Gemini tier gratis (KEPUTUSAN 16) |
| cord | ~50 struk dari CORD v2 split validation (dev) dan test (uji) | `python -m nota_eval cord` | CC BY 4.0, atribusi di README |

Catatan jujur:
- CORD berisi struk toko dan restoran Indonesia, **bukan nota grosir**. CORD menguji
  ekstraksi teks cetak dan aritmetika; tidak menguji singkatan grosir atau tulisan tangan.
- Pemetaan field CORD ke skema kita ditulis tanpa sempat membuka data aslinya. Jalankan
  konverter, buka 5 file hasil, dan cocokkan dengan gambarnya sebelum angka dipakai.
- Foto evaluasi dari toko diambil dengan kamera HP, tetapi tidak lewat aplikasi. Kompresi
  aplikasi ditiru dengan parameter yang sama (`--max-sisi`, `--kualitas`).

## 3. Struktur folder

```
data/private/toko/manifest.jsonl     # tidak di-commit
data/private/toko/001.jpg
data/private/toko/001.json           # jawaban benar
data/eval/manifest.jsonl             # CORD, bisa dibuat ulang
data/eval/cord/cord-uji-0000.png
```

Satu baris manifest:

```json
{"id": "toko-001", "gambar": "001.jpg", "kebenaran": "001.json", "jenis": "tulis", "split": null}
```

`jenis`: `cetak`, `tulis`, `campuran`, `cord`. `split`: `dev`, `uji`, atau `null` sebelum dibagi.

## 4. Format jawaban benar

Sama dengan `nota` di respons API (`nota.serialize.nota_ke_dict`):

```json
{
  "items": [
    {"nama": "Indomie Goreng", "qty": "2", "satuan": "dus", "harga_satuan": 110000, "jumlah": 220000, "diskon_baris": 0}
  ],
  "subtotal": null, "diskon": 0, "pajak": 0, "biaya_lain": 0, "pembulatan": 0, "total": 220000,
  "supplier": null, "tanggal_tertulis": null
}
```

Aturan pelabelan:
- Tulis **apa yang tertulis di nota**, bukan apa yang seharusnya. Nota yang hitungannya salah
  dilabel apa adanya; metrik memisahkannya sendiri.
- `harga_satuan` = null kalau nota memang tidak menulis harga satuan.
- Uang = bilangan bulat rupiah. Qty = teks desimal dengan titik (`"1.5"`).
- Urutan baris mengikuti nota dari atas ke bawah. Baris yang dicoret tidak dilabel.
- Nota yang kertas fisiknya tidak lengkap sehingga ada field yang tidak bisa dilabel (terpotong,
  sobek, luntur) **tidak masuk set utama**. Letakkan di manifest ketahanan
  (`manifest-ketahanan.jsonl`) dan laporkan terpisah dari angka akurasi. Alasannya: field null di
  kunci jawaban membuat validator selalu melapor `field_kosong`, yang merusak matriks kebingungan
  validator (bagian 6).
- `pajak` = pajak yang **ditambahkan di atas** jumlah baris ("+PPN", "PPN Exclude"). PPN yang sudah
  termasuk harga ("Harga sudah termasuk PPN", walau nominal PPN-nya dicetak) -> `pajak` 0.
- Diskon persen per baris -> `diskon_baris` = qty x harga - netto, dalam rupiah. Nilainya diturunkan
  dari selisih yang dijelaskan oleh persen tertulis di baris itu, bukan ditebak.
- Kuantitas dua kolom (besar + kecil) ditulis dalam satuan harga. Contoh: 0 karton + 5 buah dengan
  isi 40 per karton, harga per karton -> qty `"0.125"`.
- Dokumen di luar cakupan (bukan nota pembelian, mis. retur) -> set ketahanan, tidak masuk set utama.
- Transaksi yang sama (salinan Surat Jalan/Faktur dengan nomor, tanggal, barang, dan total sama)
  hanya satu di set utama: simpan yang bernomor file lebih kecil; salinannya ke set ketahanan.
- Kuantitas multi-satuan yang tidak habis dibagi (mis. 6 buah dari isi 72) dibulatkan 3 desimal
  (`"0.083"`). Selisih qty x harga dengan jumlah tertulis dicatat, dan nota itu dilaporkan terpisah
  dari matriks validator.
- Bila nota mencetak harga daftar dan harga netto, `harga_satuan` = harga daftar dan `diskon_baris` =
  selisihnya. Diskon nota = 0 bila total diskon yang tercetak hanya jumlah potongan baris.
- Angka tulisan tangan di atas nota (penjumlahan manual, pembulatan) tidak dipakai; kunci memakai
  angka cetak.

## 5. Alur kerja

```bash
cd backend
pip install -r requirements-dev.txt && pip install -e . --no-deps

# 1. Draf jawaban benar dengan model terkuat, lalu PERIKSA SETIAP ANGKA terhadap foto.
#    File draf bertanda "_status"; hapus baris itu setelah diperiksa. Evaluasi menolak draf.
python -m nota_eval pralabel --manifest ../data/private/toko/manifest.jsonl --model gemini-3.8-flash

# 2. Bagi dev/uji SEKALI. Sisa tetap uji selamanya.
python -m nota_eval bagi --manifest ../data/private/toko/manifest.jsonl --dev 15 --seed 42

# 3. CORD (unduh parquet split validation dan test dari Hugging Face naver-clova-ix/cord-v2)
python -m nota_eval cord --parquet validation.parquet --keluar ../data/eval --split dev --n 20
python -m nota_eval cord --parquet test.parquet --keluar ../data/eval --split uji --n 50

# 4. Setel prompt HANYA dengan dev
python -m nota_eval jalankan --manifest ../data/private/toko/manifest.jsonl --split dev

# 5. Angka README: uji, beberapa konfigurasi sekaligus
python -m nota_eval jalankan --manifest ../data/private/toko/manifest.jsonl --split uji \
  --model gemini-3.1-flash-lite --model gemini-3.8-flash --max-sisi 1600 --max-sisi 1024 --kurs-idr <KURS>
```

Bias jangkar pada pra-label: model terkuat yang menulis draf bisa membuat salahnya ikut
"dibenarkan" saat diperiksa sekilas. Periksa angka per angka. Kalau waktu cukup, label 10 nota
tanpa pra-label dan bandingkan hasilnya; tulis selisihnya di README.

## 6. Definisi metrik

- **Penyejajaran baris**: pasangan prediksi dan kebenaran berdasarkan kemiripan nama
  (SequenceMatcher, greedy, ambang 0,5). Baris kebenaran tanpa pasangan = terlewat.
- **Akurasi field** qty, harga satuan, jumlah: nilai sama persis / baris kebenaran yang punya
  nilai itu. Baris terlewat dihitung salah.
- **Nota sempurna**: tidak gagal, jumlah baris sama, semua baris cocok persis di semua field
  angka, total sama. Dilaporkan dengan interval kepercayaan 95% Wilson, karena n kecil.
- **Validator** (hanya nota yang kebenarannya konsisten secara aritmetika):
  - menangkap = tidak sempurna DAN ditandai / semua yang tidak sempurna
  - alarm palsu = sempurna TAPI ditandai / semua yang sempurna
  - "terlewat" (salah tapi lolos) adalah angka paling penting: data salah masuk ke stok tanpa
    peringatan.
- Nota asli yang hitungannya salah dilaporkan sebagai jumlah terpisah.
- **Biaya** dari token yang dilaporkan API x harga di `nota/pricing.py` (catat tanggalnya).
  Panggilan yang gagal tetap dihitung biayanya.
- **Latensi**: p50 dan p90 waktu panggilan model dari mesin evaluasi, bukan dari HP.
- **Nama barang tidak dinilai** selain untuk penyejajaran. Batas ini ditulis di README.

## 7. Aturan

- Prompt hanya disetel memakai `--split dev`. Setiap perubahan prompt menaikkan `VERSI_PROMPT`.
- Angka di README hanya dari `--split uji`, disertai versi prompt, tanggal, model, dan n.
- Kasus uji tidak pernah dipindah ke dev setelah dipakai.
- Simpan folder `eval-hasil/<waktu>/` yang dipakai README (ringkasan.md di-commit, prediksi
  dan foto tidak).
