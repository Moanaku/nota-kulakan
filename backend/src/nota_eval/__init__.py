"""Harness evaluasi ekstraksi nota.

Memakai fungsi `nota.extraction.ekstrak` yang SAMA dengan API, sehingga angka evaluasi
mengukur kode yang benar-benar melayani aplikasi.

Perintah (jalankan dari folder backend/):
  python -m nota_eval bagi      --manifest M --dev 15 --seed 42
  python -m nota_eval pralabel  --manifest M --model gemini-3.8-flash
  python -m nota_eval cord      --parquet test.parquet --keluar ../data/eval/cord --n 50
  python -m nota_eval jalankan  --manifest M --split uji --model A --model B --max-sisi 1600

Lihat docs/EVALUASI.md untuk aturan lengkap.
"""
