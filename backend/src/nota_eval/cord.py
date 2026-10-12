"""Konversi CORD v2 (NAVER Clova AI, lisensi CC BY 4.0) ke format kebenaran Nota.

Sumber   : https://github.com/clovaai/cord (Hugging Face: naver-clova-ix/cord-v2)
Atribusi : Park et al., "CORD: A Consolidated Receipt Dataset for Post-OCR Parsing",
           Document Intelligence Workshop @ NeurIPS 2019. Wajib dicantumkan di README.

Pemetaan field (dibuat dari pemahaman format CORD; WAJIB dicek pada data asli sebelum
angka dilaporkan, karena penulis belum bisa mengunduh dataset di lingkungannya):
  menu[].nm            -> items[].nama
  menu[].cnt           -> items[].qty
  menu[].unitprice     -> items[].harga_satuan (sering tidak ada -> None)
  menu[].price         -> items[].jumlah
  menu[].discountprice -> items[].diskon_baris
  sub_total.subtotal_price -> subtotal
  sub_total.discount_price -> diskon
  sub_total.tax_price      -> pajak
  sub_total.service_price  -> biaya_lain
  total.total_price        -> total

Sampel yang tidak bisa dipetakan dengan yakin DILEWATI dan alasannya dicatat (bukan ditebak):
menu dengan sub-item berharga, angka yang tidak terbaca parser, atau total ganda.
"""

from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Any

from nota.api import deteksi_mime
from nota.money import QtyParseError, RupiahParseError, parse_qty, parse_rupiah
from nota.schema import ItemBaris, Nota
from nota.serialize import nota_ke_dict


class CordTidakBisa(ValueError):
    """Sampel CORD tidak bisa dipetakan dengan yakin."""


def _teks(nilai: Any) -> str | None:
    if nilai is None:
        return None
    if isinstance(nilai, list):
        bagian = [str(x).strip() for x in nilai if str(x).strip()]
        return " ".join(bagian) or None
    teks = str(nilai).strip()
    return teks or None


def _rupiah(nilai: Any, label: str) -> int | None:
    if isinstance(nilai, list):
        if len(nilai) == 1:
            nilai = nilai[0]
        else:
            raise CordTidakBisa(f"{label} ganda")
    teks = _teks(nilai)
    if teks is None:
        return None
    bersih = teks.lstrip("@").rstrip("xX").replace(" ", "")
    try:
        return parse_rupiah(bersih)
    except RupiahParseError as e:
        raise CordTidakBisa(f"{label} tidak terbaca: {teks!r}") from e


def _qty(nilai: Any) -> Any:
    teks = _teks(nilai)
    if teks is None:
        return None
    bersih = re.sub(r"[^0-9.,]", "", teks)
    if not bersih:
        raise CordTidakBisa(f"qty tidak terbaca: {teks!r}")
    try:
        return parse_qty(bersih)
    except QtyParseError as e:
        raise CordTidakBisa(f"qty tidak terbaca: {teks!r}") from e


def petakan_cord(gt_parse: dict[str, Any]) -> Nota:
    menu = gt_parse.get("menu", [])
    if isinstance(menu, dict):
        menu = [menu]
    if not isinstance(menu, list) or not menu:
        raise CordTidakBisa("menu kosong")

    items: list[ItemBaris] = []
    for m in menu:
        if not isinstance(m, dict):
            raise CordTidakBisa("baris menu bukan objek")
        sub = m.get("sub")
        sub_daftar = sub if isinstance(sub, list) else [sub] if isinstance(sub, dict) else []
        if any(isinstance(s, dict) and s.get("price") for s in sub_daftar):
            raise CordTidakBisa("sub-item berharga")
        nama = _teks(m.get("nm"))
        if nama is None:
            raise CordTidakBisa("nama barang kosong")
        items.append(
            ItemBaris(
                nama=nama,
                qty=_qty(m.get("cnt")),
                satuan=None,
                harga_satuan=_rupiah(m.get("unitprice"), "harga satuan"),
                jumlah=_rupiah(m.get("price"), "harga baris"),
                diskon_baris=abs(_rupiah(m.get("discountprice"), "diskon baris") or 0),
            )
        )

    sub_total = gt_parse.get("sub_total") or {}
    total = gt_parse.get("total") or {}
    if not isinstance(sub_total, dict) or not isinstance(total, dict):
        raise CordTidakBisa("sub_total/total bukan objek")

    return Nota(
        items=tuple(items),
        total=_rupiah(total.get("total_price"), "total"),
        subtotal=_rupiah(sub_total.get("subtotal_price"), "subtotal"),
        diskon=abs(_rupiah(sub_total.get("discount_price"), "diskon") or 0),
        pajak=_rupiah(sub_total.get("tax_price"), "pajak") or 0,
        biaya_lain=_rupiah(sub_total.get("service_price"), "service") or 0,
    )


def konversi_parquet(parquet: Path, keluar: Path, split: str, n: int, awalan: str = "cord") -> dict[str, Any]:
    """Tulis gambar, file kebenaran, dan baris manifest. Mengembalikan laporan."""
    import pyarrow.parquet as pq

    tabel = pq.read_table(parquet).to_pylist()
    folder = keluar / awalan
    folder.mkdir(parents=True, exist_ok=True)
    manifest = keluar / "manifest.jsonl"
    id_ada: set[str] = set()
    if manifest.exists():
        for baris in manifest.read_text(encoding="utf-8").splitlines():
            if baris.strip():
                id_ada.add(json.loads(baris)["id"])

    dilewati: dict[str, int] = {}
    baris_baru: list[str] = []
    for indeks, baris in enumerate(tabel):
        if len(baris_baru) >= n:
            break
        id_kasus = f"{awalan}-{split}-{indeks:04d}"
        if id_kasus in id_ada:
            continue
        try:
            gt = json.loads(baris["ground_truth"])["gt_parse"]
            nota = petakan_cord(gt)
        except CordTidakBisa as e:
            alasan = str(e).split(":")[0]
            dilewati[alasan] = dilewati.get(alasan, 0) + 1
            continue
        gambar = baris["image"]["bytes"] if isinstance(baris["image"], dict) else baris["image"]
        ekstensi = {"image/jpeg": "jpg", "image/png": "png", "image/webp": "webp"}.get(
            deteksi_mime(gambar) or ""
        )
        if ekstensi is None:
            dilewati["gambar tidak dikenal"] = dilewati.get("gambar tidak dikenal", 0) + 1
            continue
        (folder / f"{id_kasus}.{ekstensi}").write_bytes(gambar)
        (folder / f"{id_kasus}.json").write_text(
            json.dumps(nota_ke_dict(nota), ensure_ascii=False, indent=2), encoding="utf-8"
        )
        baris_baru.append(
            json.dumps(
                {
                    "id": id_kasus,
                    "gambar": f"{awalan}/{id_kasus}.{ekstensi}",
                    "kebenaran": f"{awalan}/{id_kasus}.json",
                    "jenis": "cord",
                    "split": split,
                }
            )
        )

    with manifest.open("a", encoding="utf-8") as f:
        for b in baris_baru:
            f.write(b + "\n")
    return {"ditulis": len(baris_baru), "dilewati": dilewati, "manifest": str(manifest)}
