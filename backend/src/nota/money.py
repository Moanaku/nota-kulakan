"""Parsing dan format angka rupiah dan qty.

Aturan keras: uang selalu `int` rupiah, tidak pernah float.
Parser ini berdiri di perbatasan antara teks dari AI dan sisa program.
Prinsipnya: lebih baik menolak daripada menebak angka yang ambigu, karena angka yang salah
diam-diam jauh lebih mahal daripada satu baris yang ditandai.

Keputusan desain penting: AI diminta MENYALIN angka persis seperti tertulis ("60.000"),
bukan mengubahnya menjadi bilangan. Pemisah ribuan ditafsirkan oleh kode yang bisa diuji,
bukan oleh model. Ini menutup kesalahan klasik "60.000" terbaca 60.

Aturan yang sama dicerminkan di Kotlin (app/.../domain/Angka.kt) dan dijaga oleh
shared/test-vectors.json.
"""

from __future__ import annotations

import re
from decimal import Decimal, InvalidOperation


class RupiahParseError(ValueError):
    """Teks tidak bisa dibaca sebagai bilangan bulat rupiah dengan yakin."""


class QtyParseError(ValueError):
    """Teks tidak bisa dibaca sebagai jumlah barang dengan yakin."""


_AWALAN = re.compile(r"^(?:rp\.?|idr)\s*", re.IGNORECASE)
_AKHIRAN_TANPA_SEN = re.compile(r"[,.]-+$")  # "60.000,-"
_ANGKA_DAN_PEMISAH = re.compile(r"[\d.,]+")
_SPASI = re.compile(r"\s+")


def _gabung_ribuan(teks: str, pemisah: str) -> str:
    """Gabungkan angka berkelompok ribuan ("1.250.000" -> "1250000"), tolak yang tidak wajar."""
    kelompok = teks.split(pemisah)
    if len(kelompok) == 1:
        return kelompok[0]
    if not 1 <= len(kelompok[0]) <= 3 or any(len(k) != 3 for k in kelompok[1:]):
        raise RupiahParseError(f"pengelompokan ribuan tidak wajar: {teks!r}")
    return "".join(kelompok)


def parse_rupiah(teks: str) -> int:
    """Ubah teks rupiah ("Rp 60.000,-", "1.250.000,00", "(5.000)") menjadi int.

    Aturan pemisah:
    - Titik dan koma sama-sama muncul: yang paling kanan adalah desimal, yang lain ribuan.
    - Satu jenis pemisah muncul berkali-kali: ribuan ("1.250.000").
    - Satu pemisah muncul sekali dan diikuti tepat 3 digit: ribuan ("60.000", "60,000").
    - Satu pemisah muncul sekali dan diikuti 1-2 digit: desimal, harus bernilai nol ("60,00").
    - Selain itu ditolak.
    Angka negatif ditulis "-5.000" atau "(5.000)" (umum untuk diskon).
    """
    if not isinstance(teks, str):
        raise RupiahParseError(f"bukan teks: {teks!r}")

    s = teks.strip()
    negatif = False
    if s.startswith("(") and s.endswith(")"):
        negatif, s = True, s[1:-1].strip()
    if s.startswith("-"):
        negatif, s = True, s[1:].strip()
    s = _AWALAN.sub("", s)
    if s.startswith("-"):
        negatif, s = True, s[1:].strip()
    s = _AKHIRAN_TANPA_SEN.sub("", s)

    if not _ANGKA_DAN_PEMISAH.fullmatch(s) or not any(c.isdigit() for c in s):
        raise RupiahParseError(f"bukan angka rupiah: {teks!r}")

    ada_titik, ada_koma = "." in s, "," in s
    pecahan = ""

    if ada_titik and ada_koma:
        desimal = "." if s.rfind(".") > s.rfind(",") else ","
        ribuan = "," if desimal == "." else "."
        kiri, _, pecahan = s.rpartition(desimal)
        if desimal in kiri:
            raise RupiahParseError(f"pemisah campur aduk: {teks!r}")
        digits = _gabung_ribuan(kiri, ribuan)
    elif ada_titik or ada_koma:
        pemisah = "." if ada_titik else ","
        bagian = s.split(pemisah)
        if len(bagian) > 2:
            digits = _gabung_ribuan(s, pemisah)
        else:
            kiri, kanan = bagian
            if len(kanan) == 3 and kiri not in ("", "0"):
                digits = kiri + kanan
            elif kiri != "" and len(kanan) in (1, 2):
                digits, pecahan = kiri, kanan
            else:
                raise RupiahParseError(f"format angka ambigu: {teks!r}")
    else:
        digits = s

    if not digits.isdigit():
        raise RupiahParseError(f"bukan angka rupiah: {teks!r}")
    if pecahan and (not pecahan.isdigit() or len(pecahan) > 2 or int(pecahan) != 0):
        raise RupiahParseError(f"bukan bilangan bulat rupiah: {teks!r}")

    nilai = int(digits)
    return -nilai if negatif else nilai


def parse_qty(teks: str) -> Decimal:
    """Ubah teks qty ("2", "1,5", "0.25", "3x", "1.000") menjadi Decimal positif.

    Aturan:
    - Koma adalah desimal ("1,5" = 1.5), kebiasaan Indonesia.
    - Titik diikuti tepat 3 digit adalah ribuan ("1.000" = 1000); diikuti 1-2 digit adalah
      desimal ("1.5" = 1.5), karena model kadang menulis desimal gaya Inggris.
    - Akhiran "x" diabaikan ("3x" = 3).
    - Maksimal 3 angka di belakang koma, karena stok disimpan dalam satuan per seribu.
    - Nol, negatif, pecahan "1/2", dan teks lain ditolak.
    """
    if not isinstance(teks, str):
        raise QtyParseError(f"bukan teks: {teks!r}")
    s = _SPASI.sub("", teks).rstrip("xX")
    if not s or not re.fullmatch(r"\d+(?:[.,]\d+)*", s):
        raise QtyParseError(f"bukan angka qty: {teks!r}")

    if "," in s:
        if s.count(",") > 1:
            raise QtyParseError(f"format qty ambigu: {teks!r}")
        kiri, kanan = s.split(",")
        if not kanan.isdigit():
            raise QtyParseError(f"format qty ambigu: {teks!r}")
        kiri = _gabung_ribuan_qty(kiri, teks)
        normal = f"{kiri}.{kanan}"
    elif "." in s:
        bagian = s.split(".")
        if len(bagian) == 2 and len(bagian[1]) in (1, 2):
            normal = s
        else:
            normal = _gabung_ribuan_qty(s, teks)
    else:
        normal = s

    try:
        nilai = Decimal(normal)
    except InvalidOperation as e:  # pragma: no cover - regex di atas sudah menyaring
        raise QtyParseError(f"bukan angka qty: {teks!r}") from e
    if nilai <= 0:
        raise QtyParseError(f"qty harus lebih dari 0: {teks!r}")
    if -nilai.as_tuple().exponent > 3:  # type: ignore[operator]
        raise QtyParseError(f"qty lebih dari 3 angka desimal: {teks!r}")
    return nilai


def _gabung_ribuan_qty(teks: str, asli: str) -> str:
    if "." not in teks:
        return teks
    try:
        return _gabung_ribuan(teks, ".")
    except RupiahParseError as e:
        raise QtyParseError(f"format qty ambigu: {asli!r}") from e


def format_rupiah(n: int) -> str:
    """60000 -> "Rp 60.000"."""
    tanda = "-" if n < 0 else ""
    return f"{tanda}Rp {abs(n):,}".replace(",", ".")


def format_qty(qty: Decimal) -> str:
    """Decimal("1.500") -> "1,5"; Decimal("2") -> "2"."""
    teks = f"{qty.normalize():f}"
    return teks.replace(".", ",")
