"""Prompt dan skema keluaran untuk ekstraksi nota.

Prinsip: model MENYALIN, kode MENAFSIRKAN.
- Angka disalin persis seperti tertulis, termasuk titik dan koma. Kode (`parse_rupiah`,
  `parse_qty`) yang menafsirkan pemisah, karena perilakunya bisa diuji.
- Model dilarang menghitung atau membetulkan angka. Kalau model "membetulkan", validator
  kehilangan kesempatan menangkap ketidakcocokan yang sebenarnya ada di nota.
- Bagian yang tidak terbaca diisi null, bukan ditebak.

Setiap perubahan prompt WAJIB menaikkan VERSI_PROMPT, supaya hasil evaluasi bisa dilacak
ke versi prompt yang menghasilkannya. Prompt hanya boleh disetel memakai set dev.
"""

from __future__ import annotations

VERSI_PROMPT = "v1"

PROMPT = """\
Kamu menyalin isi nota pembelian barang dari grosir atau sales ke toko sembako di Indonesia.
Nota bisa tercetak, ditulis tangan, atau campuran.

Aturan:
1. SALIN angka persis seperti tertulis, termasuk titik dan koma. Contoh: tulis "60.000",
   jangan "60000" dan jangan 60. Jangan menghitung, menjumlahkan, atau membetulkan angka,
   walaupun menurutmu hitungan di nota salah.
2. Satu baris barang di nota = satu entri di "items", urut dari atas ke bawah.
   Abaikan baris yang dicoret.
3. Untuk tiap barang:
   - "nama": nama barang apa adanya, termasuk singkatan (contoh "Indomie Grg").
   - "qty": banyaknya barang, apa adanya (contoh "2", "1,5").
   - "satuan": dus, pcs, renceng, slop, bal, karton, kg, dan sebagainya, bila tertulis.
   - "harga_satuan": harga per satuan, bila tertulis.
   - "jumlah": nilai rupiah baris itu (biasanya kolom paling kanan), bila tertulis.
   - "diskon": potongan khusus baris itu, bila tertulis.
4. Isi null untuk bagian yang tidak tertulis atau tidak terbaca. JANGAN menebak.
5. "subtotal", "diskon", "pajak", "biaya_lain" (ongkos kirim, service, biaya lain yang
   menambah total), dan "total" untuk tingkat nota. Null bila tidak ada.
6. "supplier" dan "tanggal" apa adanya bila tertulis.
7. "jenis": "cetak", "tulis", atau "campuran".
8. "terbaca": false bila gambar bukan nota atau nota sama sekali tidak terbaca.
9. "catatan": singkat, hanya bila ada bagian penting yang tidak terbaca atau meragukan.
"""

_TEKS_ATAU_NULL = {"type": ["string", "null"]}

SKEMA_KELUARAN: dict = {
    "type": "object",
    "properties": {
        "terbaca": {"type": "boolean"},
        "jenis": {"type": "string", "enum": ["cetak", "tulis", "campuran"]},
        "supplier": _TEKS_ATAU_NULL,
        "tanggal": _TEKS_ATAU_NULL,
        "items": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "nama": {"type": "string"},
                    "qty": _TEKS_ATAU_NULL,
                    "satuan": _TEKS_ATAU_NULL,
                    "harga_satuan": _TEKS_ATAU_NULL,
                    "jumlah": _TEKS_ATAU_NULL,
                    "diskon": _TEKS_ATAU_NULL,
                },
                "required": ["nama", "qty", "satuan", "harga_satuan", "jumlah", "diskon"],
            },
        },
        "subtotal": _TEKS_ATAU_NULL,
        "diskon": _TEKS_ATAU_NULL,
        "pajak": _TEKS_ATAU_NULL,
        "biaya_lain": _TEKS_ATAU_NULL,
        "total": _TEKS_ATAU_NULL,
        "catatan": _TEKS_ATAU_NULL,
    },
    "required": [
        "terbaca",
        "jenis",
        "supplier",
        "tanggal",
        "items",
        "subtotal",
        "diskon",
        "pajak",
        "biaya_lain",
        "total",
        "catatan",
    ],
}
