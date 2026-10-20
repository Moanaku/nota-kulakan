"""Membuat tiga gambar nota SINTETIS untuk mode contoh (app/src/main/assets/contoh/*.jpg).

Semua isi buatan sendiri: tidak ada nama, alamat, atau nomor orang asli. Setiap gambar memuat teks
"CONTOH SINTETIS". Gambar menampilkan angka yang BENAR; kesalahan baca pada nota "selisih" ada di
berkas JSON (harga satuan terbaca 60), bukan di gambar. Tanpa rahasia dan tanpa jaringan.

Pakai: backend/.venv/Scripts/python.exe tools/buat_gambar_contoh.py
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

KELUARAN = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "assets" / "contoh"
LEBAR = 560
KUALITAS = 72
HURUF = ["consola.ttf", "cour.ttf", "DejaVuSansMono.ttf"]

# (nama, qty, satuan, harga, diskon_baris, jumlah) sebagai teks seperti tercetak di nota.
NOTA = {
    "bersih": {
        "toko": "GROSIR CONTOH SEJAHTERA",
        "tanggal": "12/10/2026",
        "baris": [
            ("Mie Goreng", "2", "dus", "110.000", "", "220.000"),
            ("Minyak Goreng 2L", "1", "karton", "215.000", "", "215.000"),
            ("Teh Celup", "3", "dus", "45.000", "", "135.000"),
        ],
        "akhir": [("Subtotal", "570.000"), ("TOTAL", "570.000")],
    },
    "selisih": {
        "toko": "GROSIR CONTOH MAKMUR",
        "tanggal": "14/10/2026",
        "baris": [
            ("Gula Pasir", "2", "bal", "60.000", "", "120.000"),
            ("Kopi Sachet", "4", "renceng", "28.000", "", "112.000"),
            ("Susu Kental Manis", "2", "dus", "95.000", "", "190.000"),
        ],
        "akhir": [("Subtotal", "422.000"), ("TOTAL", "422.000")],
    },
    "diskon": {
        "toko": "GROSIR CONTOH ABADI",
        "tanggal": "16/10/2026",
        "baris": [
            ("Sabun Cuci", "3", "dus", "80.000", "Disc 10.000", "230.000"),
            ("Shampo Sachet", "5", "renceng", "24.000", "", "120.000"),
            ("Pasta Gigi", "2", "dus", "150.000", "", "300.000"),
        ],
        "akhir": [
            ("Subtotal", "650.000"),
            ("Diskon", "-15.000"),
            ("Ongkos kirim", "25.000"),
            ("TOTAL", "660.000"),
        ],
    },
}


def huruf(ukuran: int) -> ImageFont.ImageFont | ImageFont.FreeTypeFont:
    for nama in HURUF:
        try:
            return ImageFont.truetype(nama, ukuran)
        except OSError:
            continue
    return ImageFont.load_default(ukuran)


def gambar(data: dict) -> Image.Image:
    besar, sedang, kecil = huruf(30), huruf(20), huruf(17)
    tinggi = 260 + 62 * len(data["baris"]) + 34 * len(data["akhir"]) + 70
    img = Image.new("RGB", (LEBAR, tinggi), "white")
    d = ImageDraw.Draw(img)
    d.rectangle([8, 8, LEBAR - 9, tinggi - 9], outline="black", width=2)

    y = 24
    d.text((LEBAR // 2, y), "CONTOH SINTETIS", font=besar, fill="black", anchor="ma")
    y += 44
    d.text((LEBAR // 2, y), "bukan nota nyata", font=kecil, fill="black", anchor="ma")
    y += 40
    d.text((LEBAR // 2, y), data["toko"], font=sedang, fill="black", anchor="ma")
    y += 30
    d.text((24, y), f"Tgl {data['tanggal']}", font=kecil, fill="black")
    y += 32
    d.line([24, y, LEBAR - 24, y], fill="black", width=1)
    y += 12

    for nama, qty, satuan, harga, disc, jumlah in data["baris"]:
        d.text((24, y), nama, font=sedang, fill="black")
        d.text((LEBAR - 24, y), jumlah, font=sedang, fill="black", anchor="ra")
        y += 28
        rincian = f"{qty} {satuan} x {harga}" + (f"  {disc}" if disc else "")
        d.text((40, y), rincian, font=kecil, fill="black")
        y += 34
    d.line([24, y, LEBAR - 24, y], fill="black", width=1)
    y += 12

    for label, nilai in data["akhir"]:
        f = sedang if label != "TOTAL" else besar
        d.text((24, y), label, font=f, fill="black")
        d.text((LEBAR - 24, y), nilai, font=f, fill="black", anchor="ra")
        y += 34 if label != "TOTAL" else 44
    return img


def main() -> None:
    KELUARAN.mkdir(parents=True, exist_ok=True)
    for kunci, data in NOTA.items():
        jalur = KELUARAN / f"{kunci}.jpg"
        gambar(data).save(jalur, "JPEG", quality=KUALITAS, optimize=True)
        print(jalur.name, jalur.stat().st_size // 1024, "KB")


if __name__ == "__main__":
    main()
