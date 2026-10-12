package com.danibanan.notakulakan.ui.hasil

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.danibanan.notakulakan.R
import com.danibanan.notakulakan.domain.PilihanProduk
import com.danibanan.notakulakan.domain.Produk
import com.danibanan.notakulakan.domain.Qty
import com.danibanan.notakulakan.domain.Rupiah
import com.danibanan.notakulakan.domain.Validator
import com.danibanan.notakulakan.domain.model.ItemBaris
import com.danibanan.notakulakan.domain.model.Nota
import com.danibanan.notakulakan.domain.model.Tingkat
import com.danibanan.notakulakan.ui.komponen.Ikon
import com.danibanan.notakulakan.ui.tema.WarnaNota

/** Teks masukan uang: kosong = null, tak terbaca = galat. */
private class MasukanUang(awal: Long?) {
    var teks by mutableStateOf(awal?.let(Rupiah::formatTanpaRp) ?: "")
    val nilai: Long? get() = teks.takeIf { it.isNotBlank() }?.let(Rupiah::parseAtauNull)
    val salah: Boolean get() = teks.isNotBlank() && nilai == null
}

@Composable
private fun KolomAngka(
    label: String,
    teks: String,
    onUbah: (String) -> Unit,
    salah: Boolean,
    keyboard: KeyboardType = KeyboardType.Number,
    bantuan: String? = null,
    modifier: Modifier = Modifier,
) {
    val pesan: String? = if (salah) stringResource(R.string.bukan_angka) else bantuan
    OutlinedTextField(
        value = teks,
        onValueChange = onUbah,
        label = { Text(label) },
        isError = salah,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        supportingText = if (pesan != null) {
            { Text(pesan) }
        } else {
            null
        },
        modifier = modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LembarBaris(
    indeks: Int,
    item: ItemBaris,
    onSelesai: (ItemBaris) -> Unit,
    onHapus: () -> Unit,
    onTutup: () -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var nama by remember { mutableStateOf(item.nama) }
    var qty by remember { mutableStateOf(item.qty?.let(Qty::format) ?: "") }
    var satuan by remember { mutableStateOf(item.satuan ?: "") }
    val harga = remember { MasukanUang(item.hargaSatuan) }
    val jumlah = remember { MasukanUang(item.jumlah) }
    val diskon = remember { MasukanUang(item.diskonBaris.takeIf { it != 0L }) }
    val hargaAwal = remember { harga.teks }

    val qtyNilai = qty.takeIf { it.isNotBlank() }?.let(Qty::parseAtauNull)
    val qtySalah = qty.isNotBlank() && qtyNilai == null
    val adaSalah = qtySalah || harga.salah || jumlah.salah || diskon.salah

    val baru = ItemBaris(
        nama = nama.trim(),
        qty = qtyNilai,
        satuan = satuan.trim().ifEmpty { null },
        hargaSatuan = harga.nilai,
        jumlah = jumlah.nilai,
        diskonBaris = diskon.nilai ?: 0L,
        // Harga yang dihitung server tetap ditandai selama pengguna tidak mengubahnya.
        hargaDihitung = item.hargaDihitung && harga.teks == hargaAwal,
    )
    val pratinjau = Validator.periksaBaris(indeks, baru).firstOrNull { it.tingkat == Tingkat.GALAT }

    ModalBottomSheet(onDismissRequest = onTutup, sheetState = state) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.edit_baris_judul, indeks + 1),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                value = nama,
                onValueChange = { nama = it },
                label = { Text(stringResource(R.string.nama_barang)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KolomAngka(
                    stringResource(R.string.qty), qty, { qty = it }, qtySalah,
                    keyboard = KeyboardType.Decimal, modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = satuan,
                    onValueChange = { satuan = it },
                    label = { Text(stringResource(R.string.satuan)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            KolomAngka(
                stringResource(R.string.harga_satuan), harga.teks, { harga.teks = it }, harga.salah,
                bantuan = item.hargaSatuan
                    ?.takeIf { harga.teks != hargaAwal }
                    ?.let { stringResource(R.string.sebelumnya_terbaca, Rupiah.format(it)) },
            )
            KolomAngka(stringResource(R.string.jumlah_rp), jumlah.teks, { jumlah.teks = it }, jumlah.salah)
            KolomAngka(stringResource(R.string.diskon_baris), diskon.teks, { diskon.teks = it }, diskon.salah)

            val s = WarnaNota.status
            val qtyBaru = baru.qty
            val hargaBaru = baru.hargaSatuan
            val jumlahBaru = baru.jumlah
            if (pratinjau != null) {
                Text(
                    pratinjau.pesan,
                    color = s.galatTeks,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().background(s.galatLatar, RoundedCornerShape(12.dp)).padding(12.dp),
                )
            } else if (qtyBaru != null && hargaBaru != null && jumlahBaru != null && !baru.hargaDihitung) {
                Text(
                    stringResource(R.string.cocok_baris, Qty.format(qtyBaru), Rupiah.format(hargaBaru), Rupiah.format(jumlahBaru)),
                    color = s.cocokTeks,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().background(s.cocokLatar, RoundedCornerShape(12.dp)).padding(12.dp),
                )
            }

            Button(
                onClick = { onSelesai(baru) },
                enabled = !adaSalah,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text(stringResource(R.string.selesai)) }
            TextButton(onClick = onHapus, modifier = Modifier.fillMaxWidth()) {
                Ikon(R.drawable.ic_hapus, null, warna = MaterialTheme.colorScheme.error)
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.hapus_baris), color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.size(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LembarTotal(
    nota: Nota,
    onSelesai: (total: Long?, subtotal: Long?, diskon: Long, pajak: Long, biayaLain: Long, supplier: String?) -> Unit,
    onTutup: () -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var supplier by remember { mutableStateOf(nota.supplier ?: "") }
    val total = remember { MasukanUang(nota.total) }
    val subtotal = remember { MasukanUang(nota.subtotal) }
    val diskon = remember { MasukanUang(nota.diskon.takeIf { it != 0L }) }
    val pajak = remember { MasukanUang(nota.pajak.takeIf { it != 0L }) }
    val biaya = remember { MasukanUang(nota.biayaLain.takeIf { it != 0L }) }
    val adaSalah = listOf(total, subtotal, diskon, pajak, biaya).any { it.salah }

    ModalBottomSheet(onDismissRequest = onTutup, sheetState = state) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.edit_total_judul), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = supplier,
                onValueChange = { supplier = it },
                label = { Text(stringResource(R.string.supplier)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            KolomAngka(stringResource(R.string.total_rp), total.teks, { total.teks = it }, total.salah)
            KolomAngka(stringResource(R.string.subtotal_opsional), subtotal.teks, { subtotal.teks = it }, subtotal.salah)
            KolomAngka(stringResource(R.string.diskon), diskon.teks, { diskon.teks = it }, diskon.salah)
            KolomAngka(stringResource(R.string.pajak), pajak.teks, { pajak.teks = it }, pajak.salah)
            KolomAngka(stringResource(R.string.biaya_lain), biaya.teks, { biaya.teks = it }, biaya.salah)
            Button(
                onClick = {
                    onSelesai(
                        total.nilai,
                        subtotal.nilai,
                        kotlin.math.abs(diskon.nilai ?: 0L),
                        pajak.nilai ?: 0L,
                        biaya.nilai ?: 0L,
                        supplier,
                    )
                },
                enabled = !adaSalah,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text(stringResource(R.string.selesai)) }
            Spacer(Modifier.size(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LembarProduk(
    namaDiNota: String,
    daftar: List<Produk>,
    onPilih: (PilihanProduk) -> Unit,
    onTutup: () -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var cari by remember { mutableStateOf("") }
    val tersaring = daftar.filter { cari.isBlank() || it.nama.contains(cari.trim(), ignoreCase = true) }

    ModalBottomSheet(onDismissRequest = onTutup, sheetState = state) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).imePadding()) {
            Text(stringResource(R.string.pilih_produk_judul), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.pilih_produk_isi, namaDiNota.ifBlank { "-" }),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(10.dp))
            OutlinedTextField(
                value = cari,
                onValueChange = { cari = it },
                label = { Text(stringResource(R.string.cari_produk)) },
                leadingIcon = { Ikon(R.drawable.ic_cari, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp).navigationBarsPadding()) {
                val namaBaru = cari.trim().ifEmpty { namaDiNota.trim() }
                if (namaBaru.isNotEmpty()) {
                    item {
                        BarisPilihan(stringResource(R.string.buat_produk_baru, namaBaru), tebal = true) {
                            onPilih(PilihanProduk.Baru(namaBaru))
                        }
                        HorizontalDivider()
                    }
                }
                items(tersaring, key = { it.id }) { p ->
                    BarisPilihan(p.nama) { onPilih(PilihanProduk.Ada(p)) }
                }
            }
        }
    }
}

@Composable
private fun BarisPilihan(teks: String, tebal: Boolean = false, onKlik: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(onClick = onKlik)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            teks,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (tebal) FontWeight.Bold else FontWeight.Normal,
            color = if (tebal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
