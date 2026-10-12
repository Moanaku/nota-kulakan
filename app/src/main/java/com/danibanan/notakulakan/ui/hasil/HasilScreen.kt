package com.danibanan.notakulakan.ui.hasil

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danibanan.notakulakan.R
import com.danibanan.notakulakan.domain.PilihanProduk
import com.danibanan.notakulakan.domain.Qty
import com.danibanan.notakulakan.domain.Rupiah
import com.danibanan.notakulakan.domain.StatusSimpan
import com.danibanan.notakulakan.domain.model.Isu
import com.danibanan.notakulakan.domain.model.ItemBaris
import com.danibanan.notakulakan.domain.model.Tingkat
import com.danibanan.notakulakan.domain.repo.JenisGagal
import com.danibanan.notakulakan.ui.komponen.FotoNota
import com.danibanan.notakulakan.ui.komponen.Ikon
import com.danibanan.notakulakan.ui.komponen.LencanaSelisih
import com.danibanan.notakulakan.ui.komponen.Pemberitahuan
import com.danibanan.notakulakan.ui.komponen.formatTanggal
import com.danibanan.notakulakan.ui.tema.WarnaNota

private sealed interface Lembar {
    data class Baris(val indeks: Int) : Lembar
    data object Total : Lembar
    data class Produk(val indeks: Int) : Lembar
}

@Composable
fun HasilScreen(
    onKembali: () -> Unit,
    onGantiFoto: () -> Unit,
    onLihatStok: () -> Unit,
    onCatatLagi: () -> Unit,
    vm: HasilViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    when (val s = ui) {
        HasilUi.Memproses -> LayarMemproses(onKembali)
        is HasilUi.Gagal -> LayarGagal(s, onKembali, onUlang = vm::proses, onGantiFoto = onGantiFoto, onManual = vm::mulaiManual)
        is HasilUi.Draf -> LayarDraf(s.draf, vm, onKembali)
        is HasilUi.Tersimpan -> LayarTersimpan(s, onLihatStok, onCatatLagi)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KerangkaAtas(judul: String, onKembali: () -> Unit, bawah: @Composable () -> Unit = {}, isi: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(judul) },
                navigationIcon = {
                    IconButton(onClick = onKembali) { Ikon(R.drawable.ic_kembali, stringResource(R.string.kembali)) }
                },
            )
        },
        bottomBar = bawah,
        content = isi,
    )
}

@Composable
private fun LayarMemproses(onKembali: () -> Unit) {
    KerangkaAtas(stringResource(R.string.periksa_judul), onKembali) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.hasil_memproses), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.hasil_memproses_isi),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun pesanGagal(jenis: JenisGagal): Int = when (jenis) {
    JenisGagal.TANPA_JARINGAN -> R.string.gagal_tanpa_jaringan
    JenisGagal.WAKTU_HABIS -> R.string.gagal_waktu_habis
    JenisGagal.LAYANAN_SIBUK -> R.string.gagal_layanan_sibuk
    JenisGagal.BUKAN_NOTA -> R.string.gagal_bukan_nota
    JenisGagal.FOTO_TIDAK_VALID -> R.string.gagal_foto_tidak_valid
    JenisGagal.FOTO_TERLALU_BESAR -> R.string.gagal_foto_terlalu_besar
    JenisGagal.APLIKASI_DITOLAK -> R.string.gagal_aplikasi_ditolak
    JenisGagal.GANGGUAN_SERVER -> R.string.gagal_server
}

@Composable
private fun LayarGagal(
    s: HasilUi.Gagal,
    onKembali: () -> Unit,
    onUlang: () -> Unit,
    onGantiFoto: () -> Unit,
    onManual: () -> Unit,
) {
    val w = WarnaNota.status
    KerangkaAtas(stringResource(R.string.periksa_judul), onKembali) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Pemberitahuan(
                R.drawable.ic_peringatan,
                stringResource(R.string.gagal_judul),
                w.cekLatar,
                w.cekTeks,
                isi = stringResource(pesanGagal(s.jenis)),
            )
            Spacer(Modifier.weight(1f))
            if (s.bisaUlang) {
                Button(onClick = onUlang, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text(stringResource(R.string.coba_lagi))
                }
            }
            OutlinedButton(onClick = onGantiFoto, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(stringResource(R.string.ganti_foto))
            }
            TextButton(onClick = onManual, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.isi_manual))
            }
        }
    }
}

@Composable
private fun LayarDraf(d: DrafUi, vm: HasilViewModel, onKembali: () -> Unit) {
    var lembar by remember { mutableStateOf<Lembar?>(null) }
    var tampilFoto by remember { mutableStateOf(false) }
    val produk by vm.produk.collectAsStateWithLifecycle()
    val w = WarnaNota.status

    LaunchedEffect(Unit) { vm.muatProduk() }

    KerangkaAtas(
        stringResource(R.string.periksa_judul),
        onKembali,
        bawah = { PanelSimpan(d, onKonfirmasi = vm::setKonfirmasi, onSimpan = { vm.simpan() }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                val jumlahGalat = d.validasi.isu.count { it.tingkat == Tingkat.GALAT }
                when {
                    d.manual && d.status == StatusSimpan.TERHALANG ->
                        Pemberitahuan(R.drawable.ic_ubah, stringResource(R.string.status_isi_manual), w.netralLatar, w.netralTeks)
                    jumlahGalat == 0 ->
                        Pemberitahuan(R.drawable.ic_cek, stringResource(R.string.status_cocok), w.cocokLatar, w.cocokTeks)
                    else ->
                        Pemberitahuan(R.drawable.ic_peringatan, stringResource(R.string.status_perlu_cek, jumlahGalat), w.cekLatar, w.cekTeks)
                }
            }
            d.catatanModel?.let { catatan ->
                item {
                    Text(
                        stringResource(R.string.catatan_ai, catatan),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (d.fotoPath != null) {
                item {
                    Column {
                        TextButton(onClick = { tampilFoto = !tampilFoto }) {
                            Ikon(R.drawable.ic_galeri, null)
                            Spacer(Modifier.size(6.dp))
                            Text(stringResource(if (tampilFoto) R.string.sembunyikan_foto else R.string.lihat_foto))
                        }
                        if (tampilFoto) FotoNota(d.fotoPath)
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().clickable { lembar = Lembar.Total }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        d.nota.supplier?.let { stringResource(R.string.supplier) + ": " + it } ?: stringResource(R.string.supplier_kosong),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Ikon(R.drawable.ic_ubah, stringResource(R.string.ubah_total), Modifier.size(18.dp))
                }
            }
            itemsIndexed(d.nota.items) { i, item ->
                KartuBaris(
                    indeks = i,
                    item = item,
                    isu = d.isuBaris(i),
                    pilihan = d.pilihan.getOrNull(i),
                    onUbah = { lembar = Lembar.Baris(i) },
                    onGantiProduk = { lembar = Lembar.Produk(i) },
                )
            }
            item {
                OutlinedButton(onClick = vm::tambahBaris, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Ikon(R.drawable.ic_tambah, null)
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.tambah_baris))
                }
            }
            item { KartuTotal(d, onUbah = { lembar = Lembar.Total }) }
        }
    }

    when (val l = lembar) {
        is Lembar.Baris -> d.nota.items.getOrNull(l.indeks)?.let { item ->
            LembarBaris(
                indeks = l.indeks,
                item = item,
                onSelesai = { baru -> vm.ubahBaris(l.indeks, baru); lembar = null },
                onHapus = { vm.hapusBaris(l.indeks); lembar = null },
                onTutup = { lembar = null },
            )
        }
        Lembar.Total -> LembarTotal(
            nota = d.nota,
            onSelesai = { total, subtotal, diskon, pajak, biaya, supplier ->
                vm.ubahTingkatNota(total, subtotal, diskon, pajak, biaya, supplier)
                lembar = null
            },
            onTutup = { lembar = null },
        )
        is Lembar.Produk -> LembarProduk(
            namaDiNota = d.nota.items.getOrNull(l.indeks)?.nama.orEmpty(),
            daftar = produk,
            onPilih = { pilihan -> vm.pilihProduk(l.indeks, pilihan); lembar = null },
            onTutup = { lembar = null },
        )
        null -> Unit
    }

    d.duplikat?.let { dup ->
        AlertDialog(
            onDismissRequest = vm::tutupDuplikat,
            title = { Text(stringResource(R.string.duplikat_judul)) },
            text = {
                Text(
                    stringResource(
                        R.string.duplikat_isi,
                        dup.total?.let(Rupiah::format) ?: "-",
                        dup.jumlahBaris,
                        formatTanggal(dup.dibuatPada),
                    ),
                )
            },
            confirmButton = { TextButton(onClick = { vm.simpan(paksa = true) }) { Text(stringResource(R.string.tetap_simpan)) } },
            dismissButton = { TextButton(onClick = vm::tutupDuplikat) { Text(stringResource(R.string.batal)) } },
        )
    }
}

@Composable
private fun KartuBaris(
    indeks: Int,
    item: ItemBaris,
    isu: List<Isu>,
    pilihan: PilihanProduk?,
    onUbah: () -> Unit,
    onGantiProduk: () -> Unit,
) {
    val w = WarnaNota.status
    val galat = isu.any { it.tingkat == Tingkat.GALAT }
    Card(
        onClick = onUbah,
        modifier = Modifier.fillMaxWidth(),
        colors = if (galat) CardDefaults.cardColors(containerColor = w.galatLatar) else CardDefaults.cardColors(),
        border = if (galat) BorderStroke(1.5.dp, MaterialTheme.colorScheme.error) else null,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        item.nama.ifBlank { stringResource(R.string.tanpa_nama) },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        buildString {
                            append(item.qty?.let(Qty::format) ?: "?")
                            item.satuan?.let { append(" ").append(it) }
                            append(" × ")
                            append(item.hargaSatuan?.let(Rupiah::format) ?: "?")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (galat) w.galatTeks else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    item.jumlah?.let(Rupiah::format) ?: "?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            for (i in isu) {
                Text(
                    i.pesan,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (i.tingkat == Tingkat.GALAT) w.galatTeks else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (pilihan != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when (pilihan) {
                            is PilihanProduk.Ada -> stringResource(R.string.produk_ada, pilihan.produk.nama)
                            is PilihanProduk.Baru -> stringResource(R.string.produk_baru, pilihan.nama.ifBlank { "-" })
                        },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onGantiProduk) { Text(stringResource(R.string.ganti_produk)) }
                }
            }
        }
    }
}

@Composable
private fun KartuTotal(d: DrafUi, onUbah: () -> Unit) {
    val n = d.nota
    val w = WarnaNota.status
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.ringkasan_total), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            val jumlahBaris = if (n.items.all { it.jumlah != null }) n.items.sumOf { it.jumlah ?: 0L } else null
            BarisAngka(stringResource(R.string.jumlah_semua_baris), jumlahBaris)
            n.subtotal?.let { BarisAngka(stringResource(R.string.subtotal_tertulis), it) }
            if (n.diskon != 0L) BarisAngka(stringResource(R.string.diskon), -n.diskon)
            if (n.pajak != 0L) BarisAngka(stringResource(R.string.pajak), n.pajak)
            if (n.biayaLain != 0L) BarisAngka(stringResource(R.string.biaya_lain), n.biayaLain)
            BarisAngka(stringResource(R.string.total_tertulis), n.total, tebal = true)
            for (i in d.isuNota) {
                Text(
                    i.pesan,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (i.tingkat == Tingkat.GALAT) w.galatTeks else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onUbah) { Text(stringResource(R.string.ubah_total)) }
        }
    }
}

@Composable
private fun BarisAngka(label: String, nilai: Long?, tebal: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(
            nilai?.let(Rupiah::format) ?: stringResource(R.string.belum_diisi),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (tebal) FontWeight.ExtraBold else FontWeight.Bold,
        )
    }
}

@Composable
private fun PanelSimpan(d: DrafUi, onKonfirmasi: (Boolean) -> Unit, onSimpan: () -> Unit) {
    Surface(tonalElevation = 3.dp, shadowElevation = 6.dp) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            when {
                d.namaKosong -> Text(stringResource(R.string.terhalang_nama), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                d.status == StatusSimpan.TERHALANG ->
                    Text(stringResource(R.string.terhalang_lengkapi), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                d.status == StatusSimpan.PERLU_KONFIRMASI -> Row(
                    Modifier.fillMaxWidth().clickable { onKonfirmasi(!d.konfirmasi) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = d.konfirmasi, onCheckedChange = onKonfirmasi)
                    Text(stringResource(R.string.konfirmasi_tidak_cocok), style = MaterialTheme.typography.bodySmall)
                }
            }
            if (d.galatSimpan) {
                Text(stringResource(R.string.galat_simpan), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = onSimpan,
                enabled = d.bolehSimpan,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                if (d.menyimpan) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.simpan_ke_stok))
                }
            }
        }
    }
}

@Composable
private fun LayarTersimpan(s: HasilUi.Tersimpan, onLihatStok: () -> Unit, onCatatLagi: () -> Unit) {
    val w = WarnaNota.status
    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))
            Box(
                Modifier.size(72.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(shape = RoundedCornerShape(36.dp), color = w.cocokLatar, modifier = Modifier.fillMaxSize()) {}
                Ikon(R.drawable.ic_cek, null, Modifier.size(36.dp), warna = w.cocokTeks)
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.tersimpan_judul), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text(
                stringResource(R.string.tersimpan_isi, s.jumlahBaris, Rupiah.format(s.total)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (s.hasil.perubahan.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(R.string.tersimpan_perubahan),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                for (p in s.hasil.perubahan) {
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(p.namaProduk, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    Rupiah.format(p.hargaSekarang) + (p.satuan?.let { " / $it" } ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            LencanaSelisih(
                                selisih = p.hargaSebelumnya?.let { p.hargaSekarang - it },
                                adaRiwayat = p.hargaSebelumnya != null,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
            Button(onClick = onLihatStok, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(stringResource(R.string.lihat_stok))
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onCatatLagi, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(stringResource(R.string.catat_nota_lain))
            }
        }
    }
}
