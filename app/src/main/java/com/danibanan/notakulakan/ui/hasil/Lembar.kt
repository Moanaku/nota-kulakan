package com.danibanan.notakulakan.ui.hasil

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.danibanan.notakulakan.R
import com.danibanan.notakulakan.domain.PilihanProduk
import com.danibanan.notakulakan.domain.Produk
import com.danibanan.notakulakan.domain.Qty
import com.danibanan.notakulakan.domain.Rupiah
import com.danibanan.notakulakan.domain.Validator
import com.danibanan.notakulakan.domain.model.ItemBaris
import com.danibanan.notakulakan.domain.model.Nota
import com.danibanan.notakulakan.domain.model.Tingkat
import com.danibanan.notakulakan.ui.komponen.DataKosong
import com.danibanan.notakulakan.ui.komponen.JenisKosong
import com.danibanan.notakulakan.ui.komponen.JenisPesan
import com.danibanan.notakulakan.ui.komponen.KolomRupiah
import com.danibanan.notakulakan.ui.komponen.KolomTeks
import com.danibanan.notakulakan.ui.komponen.PemisahBaris
import com.danibanan.notakulakan.ui.komponen.PesanPeriksa
import com.danibanan.notakulakan.ui.komponen.SheetKoreksi
import com.danibanan.notakulakan.ui.komponen.SheetPilihan
import com.danibanan.notakulakan.ui.komponen.TombolTeks
import com.danibanan.notakulakan.ui.theme.Spasi
import com.danibanan.notakulakan.ui.theme.Ukuran

@Composable
fun LembarBaris(
    indeks: Int,
    item: ItemBaris,
    onSelesai: (ItemBaris) -> Unit,
    onHapus: () -> Unit,
    onTutup: () -> Unit,
) {
    var nama by remember { mutableStateOf(item.nama) }
    var qty by remember { mutableStateOf(item.qty?.let(Qty::format) ?: "") }
    var satuan by remember { mutableStateOf(item.satuan ?: "") }
    var harga by remember { mutableStateOf(item.hargaSatuan) }
    var jumlah by remember { mutableStateOf(item.jumlah) }
    var diskon by remember { mutableStateOf(item.diskonBaris.takeIf { it != 0L }) }
    val hargaAwal = remember { item.hargaSatuan }

    val qtyNilai = qty.takeIf { it.isNotBlank() }?.let(Qty::parseAtauNull)
    val qtySalah = qty.isNotBlank() && qtyNilai == null

    val baru = ItemBaris(
        nama = nama.trim(),
        qty = qtyNilai,
        satuan = satuan.trim().ifEmpty { null },
        hargaSatuan = harga,
        jumlah = jumlah,
        diskonBaris = diskon ?: 0L,
        // Harga yang dihitung server tetap ditandai selama pengguna tidak mengubahnya.
        hargaDihitung = item.hargaDihitung && harga == hargaAwal,
    )
    val pratinjau = Validator.periksaBaris(indeks, baru).firstOrNull { it.tingkat == Tingkat.GALAT }

    SheetKoreksi(
        judul = stringResource(R.string.edit_baris_judul, indeks + 1),
        teksSimpan = stringResource(R.string.simpan_koreksi_baris),
        teksBatal = stringResource(R.string.batal),
        onSimpan = { onSelesai(baru) },
        onTutup = onTutup,
        simpanAktif = !qtySalah,
    ) {
        KolomTeks(
            nilai = nama,
            onNilaiBerubah = { nama = it },
            label = stringResource(R.string.nama_barang),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spasi.m)) {
            KolomTeks(
                nilai = qty,
                onNilaiBerubah = { qty = it },
                label = stringResource(R.string.qty),
                modifier = Modifier.weight(1f),
                galat = if (qtySalah) stringResource(R.string.bukan_angka) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            KolomTeks(
                nilai = satuan,
                onNilaiBerubah = { satuan = it },
                label = stringResource(R.string.satuan),
                modifier = Modifier.weight(1f),
            )
        }
        KolomRupiah(
            nilai = harga,
            onNilaiBerubah = { harga = it },
            label = stringResource(R.string.harga_satuan),
            bantuan = hargaAwal
                ?.takeIf { harga != hargaAwal }
                ?.let { stringResource(R.string.sebelumnya_terbaca, Rupiah.format(it)) },
        )
        KolomRupiah(nilai = jumlah, onNilaiBerubah = { jumlah = it }, label = stringResource(R.string.jumlah_rp))
        KolomRupiah(nilai = diskon, onNilaiBerubah = { diskon = it }, label = stringResource(R.string.diskon_baris))

        val qtyBaru = baru.qty
        val hargaBaru = baru.hargaSatuan
        val jumlahBaru = baru.jumlah
        if (pratinjau != null) {
            PesanPeriksa(pratinjau.pesan, JenisPesan.GALAT)
        } else if (qtyBaru != null && hargaBaru != null && jumlahBaru != null && !baru.hargaDihitung) {
            PesanPeriksa(
                stringResource(R.string.cocok_baris, Qty.format(qtyBaru), Rupiah.format(hargaBaru), Rupiah.format(jumlahBaru)),
                JenisPesan.COCOK,
            )
        }
        TombolTeks(
            teks = stringResource(R.string.hapus_baris),
            onClick = onHapus,
            modifier = Modifier.fillMaxWidth(),
            ikon = Icons.Outlined.Delete,
        )
    }
}

@Composable
fun LembarTotal(
    nota: Nota,
    onSelesai: (total: Long?, subtotal: Long?, diskon: Long, pajak: Long, biayaLain: Long, supplier: String?) -> Unit,
    onTutup: () -> Unit,
) {
    var supplier by remember { mutableStateOf(nota.supplier ?: "") }
    var total by remember { mutableStateOf(nota.total) }
    var subtotal by remember { mutableStateOf(nota.subtotal) }
    var diskon by remember { mutableStateOf(nota.diskon.takeIf { it != 0L }) }
    var pajak by remember { mutableStateOf(nota.pajak.takeIf { it != 0L }) }
    var biaya by remember { mutableStateOf(nota.biayaLain.takeIf { it != 0L }) }

    SheetKoreksi(
        judul = stringResource(R.string.edit_total_judul),
        teksSimpan = stringResource(R.string.simpan_total_nota),
        teksBatal = stringResource(R.string.batal),
        onSimpan = { onSelesai(total, subtotal, diskon ?: 0L, pajak ?: 0L, biaya ?: 0L, supplier) },
        onTutup = onTutup,
    ) {
        KolomTeks(
            nilai = supplier,
            onNilaiBerubah = { supplier = it },
            label = stringResource(R.string.supplier),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        )
        KolomRupiah(nilai = total, onNilaiBerubah = { total = it }, label = stringResource(R.string.total_rp))
        KolomRupiah(nilai = subtotal, onNilaiBerubah = { subtotal = it }, label = stringResource(R.string.subtotal_opsional))
        KolomRupiah(nilai = diskon, onNilaiBerubah = { diskon = it }, label = stringResource(R.string.diskon))
        KolomRupiah(nilai = pajak, onNilaiBerubah = { pajak = it }, label = stringResource(R.string.pajak))
        KolomRupiah(nilai = biaya, onNilaiBerubah = { biaya = it }, label = stringResource(R.string.biaya_lain))
    }
}

/** Pemilih produk di atas [SheetPilihan]: memilih satu baris langsung menutup sheet (lewat [onPilih]). */
@Composable
fun LembarProduk(
    namaDiNota: String,
    daftar: List<Produk>,
    onPilih: (PilihanProduk) -> Unit,
    onTutup: () -> Unit,
) {
    var cari by remember { mutableStateOf("") }
    val tersaring = daftar.filter { cari.isBlank() || it.nama.contains(cari.trim(), ignoreCase = true) }
    val namaBaru = cari.trim().ifEmpty { namaDiNota.trim() }

    SheetPilihan(judul = stringResource(R.string.pilih_produk_judul), onTutup = onTutup) {
        Text(
            stringResource(R.string.pilih_produk_isi, namaDiNota.ifBlank { "-" }),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        KolomTeks(nilai = cari, onNilaiBerubah = { cari = it }, label = stringResource(R.string.cari_produk))
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = Spasi.xxxl * 8).navigationBarsPadding()) {
            if (namaBaru.isNotEmpty()) {
                item {
                    BarisPilihan(stringResource(R.string.buat_produk_baru, namaBaru), utama = true) {
                        onPilih(PilihanProduk.Baru(namaBaru))
                    }
                    PemisahBaris()
                }
            }
            if (tersaring.isEmpty() && cari.isNotBlank()) {
                item {
                    DataKosong(
                        jenis = JenisKosong.HASIL_FILTER_KOSONG,
                        objek = stringResource(R.string.objek_produk),
                        onAksi = { cari = "" },
                    )
                }
            }
            items(tersaring, key = { it.id }) { p ->
                BarisPilihan(p.nama) { onPilih(PilihanProduk.Ada(p)) }
                PemisahBaris()
            }
        }
    }
}

@Composable
private fun BarisPilihan(teks: String, utama: Boolean = false, onKlik: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Ukuran.sentuhMin)
            .clickable(onClick = onKlik)
            .padding(vertical = Spasi.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            teks,
            style = if (utama) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            color = if (utama) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
