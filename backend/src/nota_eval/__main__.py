"""CLI evaluasi. Jalankan dari folder backend/: python -m nota_eval <perintah> --help"""

from __future__ import annotations

import argparse
import os
import sys
from collections import Counter
from datetime import datetime
from pathlib import Path

from .cord import konversi_parquet
from .dataset import baca_manifest, bagi_split, tulis_manifest
from .jalankan import Konfigurasi, jalankan_evaluasi, muat_env, pralabel

FOLDER_BACKEND = Path(__file__).resolve().parents[2]


def _parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(prog="python -m nota_eval")
    sub = p.add_subparsers(dest="perintah", required=True)

    j = sub.add_parser("jalankan", help="ukur akurasi, biaya, dan latensi")
    j.add_argument("--manifest", type=Path, required=True)
    j.add_argument(
        "--split",
        choices=["dev", "uji", "semua"],
        default="dev",
        help="bawaan dev; angka README hanya dari --split uji",
    )
    j.add_argument("--model", action="append", help="boleh berulang untuk membandingkan model")
    j.add_argument("--max-sisi", type=int, action="append", help="sisi terpanjang foto (px), 0 = asli")
    j.add_argument("--kualitas", type=int, default=85, help="kualitas JPEG setelah dikecilkan")
    j.add_argument("--thinking", help="MINIMAL | LOW | MEDIUM | HIGH")
    j.add_argument("--media-resolution", help="contoh MEDIA_RESOLUTION_MEDIUM")
    j.add_argument("--jenis", action="append", help="batasi jenis: cetak, tulis, campuran, cord")
    j.add_argument("--batas", type=int, help="hanya N kasus pertama (uji coba)")
    j.add_argument("--jeda", type=float, default=0.0, help="detik antar panggilan (hindari batas kuota)")
    j.add_argument("--kurs-idr", type=float, help="kurs USD->IDR untuk kolom biaya; tulis sumber kursnya")
    j.add_argument("--keluar", type=Path)

    pl = sub.add_parser("pralabel", help="tulis draf jawaban benar dari model (wajib diperiksa manual)")
    pl.add_argument("--manifest", type=Path, required=True)
    pl.add_argument("--model", default="gemini-3.8-flash")
    pl.add_argument("--timpa", action="store_true")

    b = sub.add_parser("bagi", help="bagi kasus tanpa split menjadi dev/uji")
    b.add_argument("--manifest", type=Path, required=True)
    b.add_argument("--dev", type=int, required=True, help="jumlah kasus dev")
    b.add_argument("--seed", type=int, default=42)

    c = sub.add_parser("cord", help="konversi parquet CORD v2 ke format evaluasi")
    c.add_argument("--parquet", type=Path, required=True)
    c.add_argument("--keluar", type=Path, required=True)
    c.add_argument("--split", choices=["dev", "uji"], required=True)
    c.add_argument("--n", type=int, default=50)
    return p


def main(argv: list[str] | None = None) -> int:
    args = _parser().parse_args(argv)
    muat_env(FOLDER_BACKEND / ".env")

    if args.perintah == "cord":
        laporan = konversi_parquet(args.parquet, args.keluar, args.split, args.n)
        print(f"Ditulis {laporan['ditulis']} kasus ke {laporan['manifest']}")
        for alasan, n in sorted(laporan["dilewati"].items()):
            print(f"  dilewati karena {alasan}: {n}")
        return 0

    kasus = baca_manifest(args.manifest)

    if args.perintah == "bagi":
        baru = bagi_split(kasus, args.dev, args.seed)
        tulis_manifest(args.manifest, baru)
        print(Counter((k.jenis, k.split) for k in baru))
        return 0

    if args.perintah == "pralabel":
        n = pralabel(kasus, Konfigurasi(model=args.model, max_sisi=0))
        print(f"{n} draf ditulis. Periksa setiap angka terhadap foto sebelum dipakai.")
        return 0

    if args.split != "semua":
        kasus = [k for k in kasus if k.split == args.split]
    if args.jenis:
        kasus = [k for k in kasus if k.jenis in args.jenis]
    if args.batas:
        kasus = kasus[: args.batas]
    if not kasus:
        print("Tidak ada kasus yang cocok dengan filter.", file=sys.stderr)
        return 1

    model = args.model or [os.environ.get("NOTA_MODEL", "gemini-3.1-flash-lite")]
    max_sisi = args.max_sisi or [1600]
    konfigurasi = [
        Konfigurasi(m, s, args.kualitas, args.thinking, args.media_resolution)
        for m in model
        for s in max_sisi
    ]
    keluar = args.keluar or FOLDER_BACKEND / "eval-hasil" / datetime.now().strftime("%Y%m%d-%H%M%S")
    jalankan_evaluasi(kasus, konfigurasi, keluar, jeda_detik=args.jeda, kurs_idr=args.kurs_idr)
    print(f"\nHasil lengkap: {keluar}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
