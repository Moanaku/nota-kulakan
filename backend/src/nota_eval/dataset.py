"""Manifest evaluasi: satu baris JSON per kasus.

Contoh baris:
  {"id": "toko-001", "gambar": "toko/001.jpg", "kebenaran": "toko/001.json",
   "jenis": "tulis", "split": "dev"}

- Path relatif terhadap folder file manifest.
- `jenis`: "cetak" | "tulis" | "campuran" | "cord".
- `split`: "dev" (boleh dipakai menyetel prompt) | "uji" (tidak boleh) | null (belum dibagi).
- File `kebenaran` memakai format `nota.serialize.nota_ke_dict`.
"""

from __future__ import annotations

import json
import random
from collections import defaultdict
from dataclasses import dataclass, replace
from pathlib import Path

from nota.schema import Nota
from nota.serialize import dict_ke_nota

JENIS_SAH = ("cetak", "tulis", "campuran", "cord")
SPLIT_SAH = ("dev", "uji", None)


@dataclass(frozen=True)
class Kasus:
    id: str
    gambar: Path
    kebenaran: Path
    jenis: str
    split: str | None

    def baca_kebenaran(self) -> Nota:
        return dict_ke_nota(json.loads(self.kebenaran.read_text(encoding="utf-8")))


def baca_manifest(path: Path) -> list[Kasus]:
    akar = path.parent
    kasus: list[Kasus] = []
    id_terpakai: set[str] = set()
    for nomor, baris in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        if not baris.strip():
            continue
        d = json.loads(baris)
        jenis, split = d.get("jenis"), d.get("split")
        if jenis not in JENIS_SAH:
            raise ValueError(f"baris {nomor}: jenis {jenis!r} tidak dikenal")
        if split not in SPLIT_SAH:
            raise ValueError(f"baris {nomor}: split {split!r} tidak dikenal")
        if d["id"] in id_terpakai:
            raise ValueError(f"baris {nomor}: id {d['id']!r} ganda")
        id_terpakai.add(d["id"])
        kasus.append(
            Kasus(
                id=d["id"],
                gambar=akar / d["gambar"],
                kebenaran=akar / d["kebenaran"],
                jenis=jenis,
                split=split,
            )
        )
    return kasus


def tulis_manifest(path: Path, kasus: list[Kasus]) -> None:
    akar = path.parent
    baris = [
        json.dumps(
            {
                "id": k.id,
                "gambar": k.gambar.relative_to(akar).as_posix(),
                "kebenaran": k.kebenaran.relative_to(akar).as_posix(),
                "jenis": k.jenis,
                "split": k.split,
            },
            ensure_ascii=False,
        )
        for k in kasus
    ]
    path.write_text("\n".join(baris) + "\n", encoding="utf-8")


def bagi_split(kasus: list[Kasus], jumlah_dev: int, seed: int) -> list[Kasus]:
    """Bagi dev/uji secara acak tapi bisa diulang, seimbang per jenis.

    Kasus yang SUDAH punya split tidak diubah: begitu set uji dipakai melapor angka,
    isinya tidak boleh berpindah ke dev.
    """
    acak = random.Random(seed)
    belum = [k for k in kasus if k.split is None]
    per_jenis: dict[str, list[Kasus]] = defaultdict(list)
    for k in belum:
        per_jenis[k.jenis].append(k)

    total_belum = len(belum)
    hasil_split: dict[str, str] = {}
    if total_belum == 0:
        return list(kasus)

    # Kuota dev per jenis sebanding jumlahnya (metode sisa terbesar), dibatasi isi tiap jenis.
    target = min(jumlah_dev, total_belum)
    jenis_urut = sorted(per_jenis)
    kuota = {j: target * len(per_jenis[j]) / total_belum for j in jenis_urut}
    n_dev = {j: min(int(kuota[j]), len(per_jenis[j])) for j in jenis_urut}
    sisa = target - sum(n_dev.values())
    while sisa > 0:
        kandidat = [j for j in jenis_urut if n_dev[j] < len(per_jenis[j])]
        kandidat.sort(key=lambda j: (-(kuota[j] - n_dev[j]), j))
        n_dev[kandidat[0]] += 1
        sisa -= 1

    for jenis in jenis_urut:
        daftar = sorted(per_jenis[jenis], key=lambda k: k.id)
        acak.shuffle(daftar)
        for i, k in enumerate(daftar):
            hasil_split[k.id] = "dev" if i < n_dev[jenis] else "uji"

    return [replace(k, split=hasil_split.get(k.id, k.split)) for k in kasus]
