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
| toko (utama) | 17 nota kulakan cetak milik sendiri, dibagi dev/uji | difoto Dani | data pribadi; foto tidak di-commit, tidak dipublikasikan; ditutup kotak hitam (nama, alamat, HP, NPWP, rekening pembeli) dan tanpa EXIF sebelum dikirim ke Gemini |
| toko (internet) | 5 foto nota dari internet: 2 tulis, 2 campuran, 1 cetak; dilaporkan terpisah | internet | hak cipta pemilik asli; tidak didistribusikan; ditutup kotak hitam sebelum dikirim ke Gemini |
| toko (ketahanan) | 5 kasus khusus (dua nota satu foto, kertas terpotong, retur, salinan); kualitatif | difoto Dani / internet | sama seperti di atas |
| cord | ~50 struk dari CORD v2 split validation (dev) dan test (uji) | `python -m nota_eval cord` | CC BY 4.0, atribusi di README |

Catatan jujur:
- CORD berisi struk toko dan restoran Indonesia, **bukan nota grosir**. CORD menguji
  ekstraksi teks cetak dan aritmetika; tidak menguji singkatan grosir atau tulisan tangan.
- Pemetaan field CORD ke skema kita ditulis tanpa sempat membuka data aslinya. Jalankan
  konverter, buka 5 file hasil, dan cocokkan dengan gambarnya sebelum angka dipakai.
- Tulisan tangan hanya ada di set internet (004, 005), jadi **tidak dinilai pada nota milik sendiri**.
  Hipotesis bahwa tulisan tangan terbaca (KEPUTUSAN 1 dan 18) belum diuji.
- Foto evaluasi dari toko diambil dengan kamera HP, tetapi tidak lewat aplikasi. Kompresi
  aplikasi ditiru dengan parameter yang sama (`--max-sisi`, `--kualitas`).

### Set data

Setiap set punya manifest sendiri di `data/private/toko/` dan dilaporkan terpisah
(`jalankan --manifest <file> --split semua` untuk set tanpa split).

- **Set utama** (`manifest.jsonl`): nota milik sendiri, dibagi dev/uji dengan `bagi`. Satu-satunya
  sumber angka utama README.
- **Set internet** (`manifest-internet.jsonl`, 001-005): foto dari internet, bukan milik sendiri; hak
  cipta pada pemilik asli, tidak didistribusikan, dan hanya angka agregat beserta sumbernya yang
  dilaporkan. Tidak dipakai menyetel prompt. Dilaporkan terpisah dan tidak digabung ke set utama
  karena distribusinya berbeda (toko ritel kecil, struk termal).
- **Set ketahanan** (`manifest-ketahanan.jsonl`, 006, 007, 011, 016, 026): kualitatif saja.
- **Dataset publik** (CORD, bila dikerjakan): dilaporkan terpisah; cek lisensinya.
- Nota yang ditambahkan setelah `bagi` tidak dipakai menyetel prompt dan dilaporkan sebagai
  gelombang terpisah.

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
# Set utama 17 nota: dev 5 untuk mencari pola gagal, 12 untuk uji.
python -m nota_eval bagi --manifest ../data/private/toko/manifest.jsonl --dev 5 --seed 42

# 3. CORD (unduh parquet split validation dan test dari Hugging Face naver-clova-ix/cord-v2)
python -m nota_eval cord --parquet validation.parquet --keluar ../data/eval --split dev --n 20
python -m nota_eval cord --parquet test.parquet --keluar ../data/eval --split uji --n 50

# 4. Setel prompt HANYA dengan dev
python -m nota_eval jalankan --manifest ../data/private/toko/manifest.jsonl --split dev

# 5. Angka README: uji, beberapa konfigurasi sekaligus
python -m nota_eval jalankan --manifest ../data/private/toko/manifest.jsonl --split uji \
  --model gemini-3.1-flash-lite --model gemini-3.5-flash-lite --model gemini-3.8-flash \
  --max-sisi 1600 --max-sisi 1024 --kurs-idr <KURS>
```

Bias jangkar pada pra-label: model terkuat yang menulis draf bisa membuat salahnya ikut
"dibenarkan" saat diperiksa sekilas. Periksa angka per angka. Kalau waktu cukup, label 10 nota
tanpa pra-label dan bandingkan hasilnya; tulis selisihnya di README.

## 6. Definisi metrik

- **Penyejajaran baris**: pasangan prediksi dan kebenaran berdasarkan kemiripan nama
  (SequenceMatcher, greedy, ambang 0,5). Baris kebenaran tanpa pasangan = terlewat.
- **Akurasi field per baris** qty, harga satuan, diskon baris, jumlah: nilai sama persis / baris
  kebenaran yang punya nilai itu. Baris terlewat dihitung salah. **Pajak** dinilai per nota
  (nota gagal dihitung salah).
- **Nota sempurna**: tidak gagal, jumlah baris sama, dan setiap baris cocok persis di qty,
  harga_satuan, diskon_baris, dan jumlah; serta diskon, pajak, biaya_lain, pembulatan, dan total
  tingkat nota sama persis (tanpa toleransi). Subtotal **tidak** dibandingkan: kunci sering null
  karena nota tidak mencetaknya, dan model yang mengisinya tidak salah. Dilaporkan dengan interval
  kepercayaan 95% Wilson, karena n kecil.
  - Definisi diperketat pada 2026-10-19 setelah putaran dev baseline, sebelum split uji dijalankan;
    hasil uji belum pernah dilihat. Alasan: definisi lama tidak menilai diskon_baris dan pajak
    sehingga nota dengan diskon bergeser tercatat sempurna.
- **Validator** (hanya nota yang kebenarannya konsisten secara aritmetika; "salah" = tidak sempurna
  menurut definisi di atas):
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

## 8. Riwayat prompt

### v1 -> v2 (2026-10-20)

Alasan, dari pola gagal di set dev:
- dev 020: nominal PPN yang sudah termasuk dalam harga terbaca sebagai pajak tambahan.
- dev 015: kolom potongan ("Disc", "Discount Product") bergeser ke baris lain.

Perubahan: aturan 3 (potongan dibaca sejajar barisnya, tanpa potongan = null) dan aturan 5
(pajak null bila PPN sudah termasuk).

Hasil dev (n=5, gemini-3.1-flash-lite, 1600 px, JPEG 85, definisi metrik 2026-10-19):

| Versi | Nota sempurna (Wilson 95%) | Qty | Diskon baris | Pajak | Validator menangkap | Alarm palsu |
|---|---|---|---|---|---|---|
| v1 | 60% (3/5; 23-88%) | 23/25 | 22/25 | 4/5 | 2/2 | 0/3 |
| v2 run1 | 80% (4/5; 38-96%) | 23/25 | 25/25 | 5/5 | 1/1 | 0/4 |
| v2 run2 | 80% (4/5; 38-96%) | 23/25 | 25/25 | 5/5 | 1/1 | 0/4 |

Nota 014, 017, 025 tetap sempurna di kedua run. Nota 015 dan pajak 020 membaik. Satu-satunya nota
yang masih salah adalah 020: qty dua baris (0,5 per kunci 0,125) dan validator menangkapnya.
Metrik run1 dan run2 identik.

Catatan:
- (a) Hanya dev yang dipakai menyetel. Set uji belum dijalankan.
- (b) Qty dua kolom (mis. 020) belum ditangani. Butuh field isi per kemasan, yang mengubah format
  API dan perlu persetujuan (aturan 12).
- (c) Contoh "Discount Product" di aturan 3 berasal dari nota dev, tetapi judul kolom yang sama
  ada di nota uji pemasok yang sama (013, 021). Perbaikan kemungkinan ikut membantu nota uji itu,
  dan dev dan uji berbagi template pemasok, jadi angka uji tidak sepenuhnya independen dari dev.
- (d) Pada 2026-10-19 server Gemini membalas 503 dua kali berturut-turut selama sekitar 15 menit
  tanpa detail kuota. Dipakai tier gratis; biaya di laporan adalah perkiraan harga daftar, bukan
  tagihan.

## 9. Protokol uji

Ditetapkan 2026-10-20, sebelum split uji dijalankan.

- **Konfigurasi utama** (angka utama README): `gemini-3.1-flash-lite` pada 1600 px, yang disetel di
  dev dengan prompt v2.
- **Konfigurasi uji**: 3 model (`gemini-3.1-flash-lite`, `gemini-3.5-flash-lite`,
  `gemini-3.8-flash`) x 2 ukuran foto (1024 dan 1600 px) = 6. Semuanya dilaporkan di tabel; tidak
  ada yang disembunyikan.
- **Pindah model**: dari konfigurasi utama hanya bila model lain unggul >= 2 nota sempurna di uji
  DAN biaya per nota <= 3x konfigurasi utama. Selain itu tetap.
- **Ukuran foto**: 1024 px dipakai bila nota sempurnanya tidak lebih rendah dari 1600 px lebih dari
  1 nota (alasan: pemakaian data pengguna). Selain itu 1600 px.
- **Pengulangan**: konfigurasi yang gagal di tengah jalan karena 503/429 diulang penuh dari awal.
  Konfigurasi tidak pernah diulang karena akurasinya rendah.
- **Batas inferensi**: selisih <= 2 nota dari 12 tidak bermakna secara statistik (Wilson 95%
  lebar). Dilaporkan sebagai batasan, bukan sebagai pemenang.
- **Biaya**: harga daftar dari https://ai.google.dev/gemini-api/docs/pricing, dibaca 2026-10-20,
  termasuk token berpikir sebagai token keluaran. Tier gratis tidak ditagih; angka ini perkiraan.
  Harga `gemini-3.8-flash` naik pada 2027-01-01 ($1,50 masuk / $7,50 keluar per 1 juta token dari
  $0,75 / $3,75), jadi angka biayanya hanya berlaku sampai 2026-12-31.
- **Set lain**: set internet dan set ketahanan dijalankan sesudah konfigurasi ditetapkan, dengan
  konfigurasi yang terpilih, dan dilaporkan terpisah.

### Putaran uji pertama terhenti (2026-10-20) dan putaran kedua

Putaran pertama tidak menghasilkan ringkasan: proses keluar tanpa menulis `hasil.csv` dan
`ringkasan.md`, dan latensi per nota tidak tersimpan. Empat konfigurasi selesai (12 nota), lalu
`gemini-3.8-flash` @1600 px terhenti di nota keenam karena waktu habis (3 kali retry); 3.8-flash
@1024 px belum dijalankan. Keputusan, ditetapkan sebelum putaran kedua dijalankan:

- Harness diperbaiki (latensi per nota disimpan; konfigurasi yang terhenti ditandai tidak lengkap
  tanpa membuang ringkasan konfigurasi lain; kode keluar bukan 0 bila ada yang tidak lengkap).
  Definisi metrik, prompt, dan skema tidak berubah.
- **Putaran kedua adalah satu-satunya putaran resmi** untuk keenam konfigurasi, apa pun hasilnya.
  Putaran pertama diarsipkan, tidak dipakai sebagai angka, dan hanya disebut di sini. Hitungan kasar
  yang sempat terlihat di layar pada putaran pertama tidak dipakai.
- Waktu habis (batas 30 detik per panggilan, sama dengan produksi) adalah kegagalan terukur: nota
  dihitung gagal di kolom "Gagal" dan konfigurasi tidak diulang. Penyebabnya (model lambat atau
  server sedang padat) tidak bisa dibedakan dan dicatat sebagai batasan.
- Layanan sibuk (503) atau kuota (429) setelah retry membuat konfigurasi "tidak lengkap": tidak
  masuk tabel metrik, dicantumkan terpisah, dan diulang penuh di lain waktu.

## 10. Hasil uji, putaran 2 (2026-10-20)

Putaran resmi sesuai bagian 9: split uji (12 nota milik sendiri, cetak), prompt v2, foto tertutup.
Ringkasan lengkap: `docs/hasil/uji-2026-10-20-putaran2.md`. Biaya = perkiraan harga daftar
(tier gratis, tanpa tagihan), harga dibaca 2026-10-20.

| Konfigurasi | Nota sempurna (Wilson 95%) | Total benar | Diskon baris | Validator menangkap | Biaya/nota | Latensi p50/p90 | Foto |
|---|---|---|---|---|---|---|---|
| 3.1-flash-lite @1024 | 5/12 = 42% (19-68%) | 92% | 78% | 4/4 | $0,00113 | 2,6 / 16,5 s | 88 KB |
| 3.1-flash-lite @1600 | 6/12 = 50% (25-75%) | 83% | 88% | 3/3 | $0,00115 | 2,6 / 25,4 s | 193 KB |
| 3.5-flash-lite @1024 | 7/12 = 58% (32-81%) | 92% | 90% | 2/2 | $0,00170 | 2,1 / 4,1 s | 88 KB |
| 3.5-flash-lite @1600 | 7/12 = 58% (32-81%) | 100% | 90% | 2/2 | $0,00170 | 2,1 / 2,6 s | 193 KB |
| 3.8-flash @1600 dan @1024 | tidak lengkap: 0/12, kuota harian habis (429) | | | | | | |

Gagal (waktu habis, layanan) = 0 di keempat konfigurasi lengkap. Alarm palsu 0 di semuanya.

### Penerapan aturan bagian 9 (hitungan, bukan penilaian)

- **Model**: 3.5-flash-lite unggul 1 nota sempurna (7 vs 6) dari konfigurasi utama; syaratnya >= 2.
  Model tetap `gemini-3.1-flash-lite`. `gemini-3.8-flash` belum dievaluasi (kuota harian habis), jadi
  keputusan model baru final setelah 3.8-flash dijalankan penuh di hari lain (protokol: konfigurasi
  tidak lengkap diulang penuh).
- **Ukuran foto**: 1024 px (5/12) tidak lebih rendah dari 1600 px (6/12) lebih dari 1 nota, jadi
  1024 px dipakai (foto 88 KB vs 193 KB).
- **Konfigurasi sementara**: `gemini-3.1-flash-lite` @1024 px, 5/12 nota sempurna (42%, Wilson
  19-68%). Interval antar konfigurasi saling tumpang tindih; selisih 1 nota tidak bermakna.

### Temuan (dari hasil per nota; penyebab tertulis sebagai dugaan bila belum diselidiki)

- Nota 009, 018, 023, 024, 027 salah di keempat konfigurasi. Tiga di antaranya (023, 024, 027)
  kuncinya tidak konsisten secara aritmetika (pecahan qty yang dibulatkan, selisih pembulatan 1
  rupiah), sehingga praktis tidak bisa sempurna persis; dilaporkan terpisah dari matriks validator.
- Pada nota dengan kunci konsisten (n=9), semua nota yang salah ditandai validator di keempat
  konfigurasi (terlewat 0 dari 2-4). Pembaginya kecil; belum bisa disimpulkan lebih jauh.
- 009 dan 018 (diskon persen per baris): `diskon_baris` tidak terbaca (model mengisi 0). Skema tidak
  punya field persen dan kunci berupa rupiah turunan.
- 022: 3.1-flash-lite memakai total tulisan tangan (4.505.000) bukan total cetak (4.504.997); 3.5
  benar.
- 024: pada 3.1-flash-lite @1600 hasilnya rusak total (nama bergeser, qty/harga/jumlah kosong).
  Dugaan: foto terbalik 180 derajat; belum diselidiki.
- 027: diskon nota 1001 (menghitung dua kali potongan baris); pada 3.5 juga pajak 11745 (PPN sudah
  termasuk).
- Latensi p90 3.1-flash-lite 16,5-25,4 detik mendekati batas 30 detik per panggilan, sedangkan 3.5
  2,6-4,1 detik. Satu putaran, beban server tidak terkontrol; belum bisa dipisahkan dari beban
  server. Aturan bagian 9 tidak memuat latensi; aturan tidak diubah setelah hasil terlihat. Ini
  dicatat sebagai pertimbangan untuk pemilihan model sesudah Lapisan 1.
- 3.5-flash-lite 1,5x lebih mahal per nota dari 3.1-flash-lite ($0,00170 vs $0,00113-0,00115).

## 11. Set internet dan set ketahanan (2026-10-20, konfigurasi sementara)

Konfigurasi: `gemini-3.1-flash-lite` @1024 px, prompt v2, foto tertutup. Sementara karena
`gemini-3.8-flash` belum selesai dievaluasi (bagian 10); bila model berganti, dua set ini diulang.
Dijalankan satu kali, tidak diulang demi akurasi. Tidak dipakai menyetel apa pun.

### Set internet (001-005, n=5, dilaporkan terpisah)

Nota sempurna 5/5 (Wilson 95%: 57-100%); tulis tangan (004, 005) 2/2. Biaya rata-rata $0,00119 per
nota (perkiraan harga daftar), latensi p50/p90 3,4 / 5,8 detik, foto rata-rata 28 KB. Validator
menandai 002 dan 004, yang kuncinya sendiri tidak konsisten (bukan kesalahan model); alarm palsu 0/3.

Cara membaca: n=5, jadi 100% tidak boleh dibaca sebagai akurasi umum. Set ini berbeda dari set utama
(foto dari internet, bukan nota grosir yang saya kumpulkan, dan jauh lebih sedikit baris diskon),
dan tidak boleh dibandingkan langsung dengan 42% di set uji. Penyebab selisihnya belum diselidiki.

### Set ketahanan (006, 007, 011, 016, 026, kualitatif)

Harness menolak manifest ini karena kunci 007 masih berstatus pra-label. Kunci tidak dikarang.
Kelima foto dijalankan lewat skrip sementara yang memakai fungsi harness yang sama; keluaran mentah
dan isu validator disimpan, "nota sempurna" tidak dihitung.

| id | kasus | perilaku model | validator |
|---|---|---|---|
| 006 | kertas terpotong | qty 10 dan 2 diisi (kunci null), tanpa tanda ragu | menandai 1 baris |
| 007 | dua nota dalam satu foto | digabung jadi satu nota, tanpa peringatan | menandai 3 baris + 1 kosong |
| 011 | retur | tidak ada sinyal retur; baris 40 x 812 ditulis sesuai cetakan 32.466 | menandai baris 4 |
| 016 | salinan 014 | hasil sama dengan 014 | lolos |
| 026 | salinan 025 plus baris bonus tanpa harga | 7 baris identik dengan 025 | menandai baris tanpa harga |

Pelajaran produk (hipotesis perilaku, bukan angka akurasi): pada foto yang tidak lengkap atau
berisi dua nota, model tidak memberi tanda keraguan, jadi satu-satunya penjaga adalah validator dan
konfirmasi pengguna; lolos validator pada nota yang salah tidak bisa dibedakan dari nota benar.
016 lolos dan akan masuk otomatis. Keluaran tidak menyimpan `terbaca` dan `catatan`; periksa apakah
itu memang keputusan desain.

## 12. gemini-3.8-flash: evaluasi tidak lengkap (2026-10-20)

Hanya `gemini-3.8-flash` @1600 px yang dicoba pada putaran ini (folder `uji-20261020-3.8flash-1600`,
split uji, prompt v2). Hasil terukur, tanpa angka akurasi:

- 9 dari 12 nota terproses, dan **kesembilannya gagal `waktu_habis`** pada batas 30 detik per panggilan
  (token 0, tidak ada keluaran model). Nota ke-10 kena 429 (kuota harian habis) setelah 3 kali coba ulang,
  sisanya tidak dijalankan. @1024 px tidak dijalankan.
- Batas tier gratis yang tercatat dari galat 429 tadi pagi: 20 permintaan per hari per proyek per model
  (`GenerateRequestsPerDayPerProjectPerModel-FreeTier`). Dugaan, belum diverifikasi: coba ulang setelah
  waktu habis ikut dihitung ke kuota, sehingga 9 nota yang gagal sudah menghabiskannya.
- Penyebab waktu habis belum diketahui: bisa latensi model lebih dari 30 detik atau layanan tidak merespons;
  keduanya tidak dibedakan. Satu putaran sebelumnya (putaran 1) juga menunjukkan waktu habis pada model ini.

### Keputusan (ditulis sebelum ada pengulangan; tidak ada pengulangan)

- Pada @1600 maksimal 3 nota sempurna mungkin (3 nota sisa), sedangkan syarat mengganti model adalah
  minimal 8 dari 12 dan biaya paling banyak 3x. Jadi 3.8-flash tidak bisa mengalahkan konfigurasi utama
  pada ukuran itu. @1024 tidak terukur; tidak ada klaim apa pun tentang akurasinya.
- Konfigurasi akhir tidak berubah: `gemini-3.1-flash-lite` @1024 px, 5/12 nota sempurna (42%, Wilson 19-68%).
  Set internet dan set ketahanan (bagian 11) sudah memakai konfigurasi ini, jadi tidak perlu diulang.
- 3.8-flash tidak dievaluasi lebih lanjut. Alasannya praktis dan tidak bergantung pada akurasi: batas 20
  permintaan per hari di tier gratis tidak cukup untuk aplikasi, harga naik dua kali lipat pada 2027-01-01,
  dan dua putaran berturut-turut menunjukkan waktu habis pada batas produksi 30 detik.

Keterbatasan untuk README: model ini diuji sebagian dan hanya pada satu ukuran foto.

## 13. Protokol gelombang lapangan: waktu per nota dan nota pemasok lain (ditetapkan 2026-10-20, sebelum data dikumpulkan)

Tujuan: mengukur (1) waktu per nota, cara lama lawan aplikasi, dan (2) seberapa baik aplikasi membaca nota
pemasok yang tidak ada di set utama. Dilaporkan terpisah dari angka uji (bagian 10); tidak dipakai menyetel
prompt, model, atau ukuran foto. Kunci di sini bukan kunci evaluasi resmi, hanya hasil pemeriksaan manual.

**Persiapan.** Konfigurasi akhir: `gemini-3.1-flash-lite`, foto 1024 px (bagian 12). Laptop menjalankan
backend dengan Gemini asli; HP tersambung kabel (`adb reverse`); aplikasi debug. Izin penjual didapat
sebelum memotret. Nomor telepon, alamat pribadi, dan nomor rekening ditutup sebelum difoto. Foto asli
dihapus dari HP setelah selesai dan tidak masuk repo. Toko dicatat dengan kode (T1, T2, ...), bukan nama.

**Satuan.** Nota (n sebanyak yang terkumpul; target 20, dilaporkan apa adanya berapa pun hasilnya).
Tiap nota dikerjakan dua kali (berpasangan): cara lama dan aplikasi, urutan diselang-seling (nota ganjil
cara lama dulu, genap aplikasi dulu). Nota disimpan di tempat yang sama dan dipegang kedua kali.
Cara lama (asumsi, bisa diganti bila Dani menyebutkan yang lain): mengetik nama barang, qty, harga, jumlah
tiap baris dan total ke Catatan/Excel di HP.

**Waktu.** Stopwatch. Cara lama: dari nota di tangan sampai semua baris dan total terketik. Aplikasi: dari
mengetuk "Catat nota" sampai "Simpan nota ke stok" selesai (termasuk foto, menunggu AI, memeriksa, mengoreksi).
Ditulis dalam detik per nota, dan dilaporkan juga per baris (detik / jumlah baris).

**Kesalahan.** Setelah selesai, bandingkan hasil dengan nota fisik angka per angka. `salah_manual` =
jumlah angka salah pada hasil ketikan cara lama. `baris_dikoreksi_di_app` = baris yang perlu diperbaiki
pengguna sebelum disimpan. `salah_lolos_di_app` = angka salah pada data yang tersimpan. `validator_menandai`
= ya/tidak. Nota dinilai "benar tanpa koreksi" bila `baris_dikoreksi_di_app` = 0 dan hasil benar.

**Pelaporan.** Rata-rata dan median detik per nota dan per baris untuk kedua cara, selisihnya, jumlah nota,
jumlah toko, serta proporsi nota "benar tanpa koreksi" (Wilson 95%). Keterbatasan wajib: satu pemakai (Dani),
efek belajar karena nota yang sama dikerjakan dua kali (diredam dengan urutan diselang-seling), jumlah nota
kecil, nota dikumpulkan dari toko yang dikenal Dani (bukan sampel acak), dan cara lama yang diasumsikan.
Tidak ada klaim tentang pengguna lain, pendapatan, atau dampak bisnis.
