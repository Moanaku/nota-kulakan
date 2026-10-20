# Hasil evaluasi

- Waktu: 2026-10-20T04:33:57+00:00
- Versi prompt: v2
- Split: uji; kasus: 12
- Harga model: https://ai.google.dev/gemini-api/docs/pricing (dibaca 2026-10-20)
- Definisi metrik: lihat docs/EVALUASI.md

| Konfigurasi | Jenis | n | Nota sempurna (95% CI) | Total benar | Qty | Harga satuan | Jumlah | Diskon baris | Pajak | Validator menangkap | Alarm palsu | Gagal | Biaya/nota | Latensi p50/p90 | Ukuran foto |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| gemini-3.1-flash-lite@1024px | semua | 12 | 42% (5/12; 19-68%) | 92% | 82% (40/49) | 92% (45/49) | 90% (44/49) | 78% (38/49) | 100% (12/12) | 100% (4/4) | 0% (0/5) | 0 | $0.00113 | 2563/16484 ms | 88 KB |
| gemini-3.1-flash-lite@1024px | cetak | 12 | 42% (5/12; 19-68%) | 92% | 82% (40/49) | 92% (45/49) | 90% (44/49) | 78% (38/49) | 100% (12/12) | 100% (4/4) | 0% (0/5) | 0 | $0.00113 | 2563/16484 ms | 88 KB |
| gemini-3.1-flash-lite@1600px | semua | 12 | 50% (6/12; 25-75%) | 83% | 82% (40/49) | 84% (41/49) | 84% (41/49) | 88% (43/49) | 100% (12/12) | 100% (3/3) | 0% (0/6) | 0 | $0.00115 | 2593/25438 ms | 193 KB |
| gemini-3.1-flash-lite@1600px | cetak | 12 | 50% (6/12; 25-75%) | 83% | 82% (40/49) | 84% (41/49) | 84% (41/49) | 88% (43/49) | 100% (12/12) | 100% (3/3) | 0% (0/6) | 0 | $0.00115 | 2593/25438 ms | 193 KB |
| gemini-3.5-flash-lite@1024px | semua | 12 | 58% (7/12; 32-81%) | 92% | 82% (40/49) | 88% (43/49) | 100% (49/49) | 90% (44/49) | 100% (12/12) | 100% (2/2) | 0% (0/7) | 0 | $0.00170 | 2140/4109 ms | 88 KB |
| gemini-3.5-flash-lite@1024px | cetak | 12 | 58% (7/12; 32-81%) | 92% | 82% (40/49) | 88% (43/49) | 100% (49/49) | 90% (44/49) | 100% (12/12) | 100% (2/2) | 0% (0/7) | 0 | $0.00170 | 2140/4109 ms | 88 KB |
| gemini-3.5-flash-lite@1600px | semua | 12 | 58% (7/12; 32-81%) | 100% | 82% (40/49) | 88% (43/49) | 100% (49/49) | 90% (44/49) | 92% (11/12) | 100% (2/2) | 0% (0/7) | 0 | $0.00170 | 2140/2625 ms | 193 KB |
| gemini-3.5-flash-lite@1600px | cetak | 12 | 58% (7/12; 32-81%) | 100% | 82% (40/49) | 88% (43/49) | 100% (49/49) | 90% (44/49) | 92% (11/12) | 100% (2/2) | 0% (0/7) | 0 | $0.00170 | 2140/2625 ms | 193 KB |

## Tidak lengkap

Tidak masuk tabel di atas.

- gemini-3.8-flash@1600px: 0/12 nota selesai; kuota harian habis (429)
- gemini-3.8-flash@1024px: 0/12 nota selesai; tidak dijalankan: kuota harian habis (429)
