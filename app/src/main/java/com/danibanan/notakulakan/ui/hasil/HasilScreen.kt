package com.danibanan.notakulakan.ui.hasil

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
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
import com.danibanan.notakulakan.ui.komponen.BarAksiBawah
import com.danibanan.notakulakan.ui.komponen.BarAtas
import com.danibanan.notakulakan.ui.komponen.BarisBarang
import com.danibanan.notakulakan.ui.komponen.BarisRingkasanTotal
import com.danibanan.notakulakan.ui.komponen.ChipSelisihHarga
import com.danibanan.notakulakan.ui.komponen.ChipStatus
import com.danibanan.notakulakan.ui.komponen.DataKosong
import com.danibanan.notakulakan.ui.komponen.FotoNota
import com.danibanan.notakulakan.ui.komponen.JenisKosong
import com.danibanan.notakulakan.ui.komponen.JenisPesan
import com.danibanan.notakulakan.ui.komponen.KondisiGalat
import com.danibanan.notakulakan.ui.komponen.PemisahBaris
import com.danibanan.notakulakan.ui.komponen.PesanPeriksa
import com.danibanan.notakulakan.ui.komponen.SkeletonBarisBarang
import com.danibanan.notakulakan.ui.komponen.SpandukOffline
import com.danibanan.notakulakan.ui.komponen.StatusBaris
import com.danibanan.notakulakan.ui.komponen.TombolSekunder
import com.danibanan.notakulakan.ui.komponen.TombolTeks
import com.danibanan.notakulakan.ui.komponen.TombolUtama
import com.danibanan.notakulakan.ui.komponen.formatTanggal
import com.danibanan.notakulakan.ui.komponen.rememberTersambung
import com.danibanan.notakulakan.ui.theme.Elevasi
import com.danibanan.notakulakan.ui.theme.Radius
import com.danibanan.notakulakan.ui.theme.Spasi
import com.danibanan.notakulakan.ui.theme.Ukuran

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

/**
 * Memuat: kalimat yang menyebut objeknya, tombol batal, dan skeleton berbentuk baris barang (bukan spinner tengah
 * layar). Tahap yang lebih rinci tidak dilacak ViewModel, jadi hanya satu teks tahap.
 */
@Composable
internal fun LayarMemproses(onKembali: () -> Unit) {
    Scaffold(topBar = { BarAtas(stringResource(R.string.periksa_judul), onKembali = onKembali) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = Spasi.l),
            verticalArrangement = Arrangement.spacedBy(Spasi.s),
        ) {
            Text(
                stringResource(R.string.hasil_memproses),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                stringResource(R.string.hasil_memproses_isi),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TombolTeks(stringResource(R.string.batal_membaca), onClick = onKembali, sejajarMargin = true)
            SkeletonBarisBarang(Modifier.padding(top = Spasi.s), jumlahBaris = 4)
        }
    }
}

/** Jenis gagal (termasuk Gemini 503/429 = layanan sibuk, 504/timeout = waktu habis) menjadi kalimat ramah. */
private fun pesanGagal(jenis: JenisGagal, tersambung: Boolean): Int = when (jenis) {
    // HP online tetapi server tak terjangkau: bukan masalah koneksi pengguna.
    JenisGagal.TANPA_JARINGAN -> if (tersambung) R.string.gagal_layanan_tak_terjangkau else R.string.gagal_tanpa_jaringan
    JenisGagal.WAKTU_HABIS -> R.string.gagal_waktu_habis
    JenisGagal.LAYANAN_SIBUK -> R.string.gagal_layanan_sibuk
    JenisGagal.BUKAN_NOTA -> R.string.gagal_bukan_nota
    JenisGagal.FOTO_TIDAK_VALID -> R.string.gagal_foto_tidak_valid
    JenisGagal.FOTO_TERLALU_BESAR -> R.string.gagal_foto_terlalu_besar
    JenisGagal.APLIKASI_DITOLAK -> R.string.gagal_aplikasi_ditolak
    JenisGagal.GANGGUAN_SERVER -> R.string.gagal_server
}

/**
 * Gagal: sebab spesifik per jenis di tengah, aksi utama di [BarAksiBawah] ("Coba lagi", atau "Foto ulang" bila foto
 * yang salah), "Isi manual" sebagai aksi sekunder. "Ganti foto nota" tetap ada sebagai tombol teks bila aksi
 * utamanya mengulang panggilan.
 */
@Composable
internal fun LayarGagal(
    s: HasilUi.Gagal,
    onKembali: () -> Unit,
    onUlang: () -> Unit,
    onGantiFoto: () -> Unit,
    onManual: () -> Unit,
    tersambung: Boolean = rememberTersambung(),
) {
    Scaffold(
        topBar = { BarAtas(stringResource(R.string.periksa_judul), onKembali = onKembali) },
        bottomBar = {
            BarAksiBawah(
                // Foto yang salah tidak membaik dengan diulang: aksi utamanya memilih foto lain.
                teksUtama = stringResource(if (s.bisaUlang) R.string.galat_coba_lagi else R.string.galat_foto_ulang),
                onUtama = if (s.bisaUlang) onUlang else onGantiFoto,
                teksSekunder = stringResource(R.string.isi_manual),
                onSekunder = onManual,
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SpandukOffline(tampil = s.jenis == JenisGagal.TANPA_JARINGAN && !tersambung)
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(Modifier.verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                    KondisiGalat(
                        sebab = stringResource(pesanGagal(s.jenis, tersambung)),
                        onCobaLagi = null,
                        judul = stringResource(R.string.gagal_judul),
                    )
                    if (s.bisaUlang) TombolTeks(stringResource(R.string.ganti_foto), onClick = onGantiFoto)
                }
            }
        }
    }
}

private fun statusBaris(item: ItemBaris, isu: List<Isu>): StatusBaris = when {
    isu.any { it.tingkat == Tingkat.GALAT } -> StatusBaris.SELISIH
    item.qty == null || item.hargaSatuan == null || item.jumlah == null -> StatusBaris.BELUM_TERBACA
    else -> StatusBaris.COCOK
}

private fun qtyHarga(item: ItemBaris): String = buildString {
    append(item.qty?.let(Qty::format) ?: "?")
    item.satuan?.let { append(" ").append(it) }
    append(" × ")
    append(item.hargaSatuan?.let(Rupiah::format) ?: "?")
}

private fun jenisPesan(isu: Isu): JenisPesan = if (isu.tingkat == Tingkat.GALAT) JenisPesan.GALAT else JenisPesan.INFO

@Composable
private fun LayarDraf(d: DrafUi, vm: HasilViewModel, onKembali: () -> Unit) {
    var lembar by remember { mutableStateOf<Lembar?>(null) }
    var tampilFoto by remember { mutableStateOf(false) }
    val produk by vm.produk.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.muatProduk() }

    Scaffold(
        topBar = { BarAtas(stringResource(R.string.periksa_judul), onKembali = onKembali) },
        bottomBar = { BarSimpan(d, onSimpan = { vm.simpan() }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = Spasi.l, end = Spasi.l, top = Spasi.s, bottom = Spasi.l),
        ) {
            // Satu elemen dominan: total nota. Status keseluruhan (PesanPeriksa) tepat di bawahnya.
            item {
                val jumlahGalat = d.validasi.isu.count { it.tingkat == Tingkat.GALAT }
                Column(Modifier.padding(bottom = Spasi.l), verticalArrangement = Arrangement.spacedBy(Spasi.s)) {
                    BarisRingkasanTotal(
                        label = stringResource(R.string.total_tertulis),
                        nilai = d.nota.total?.let(Rupiah::format) ?: stringResource(R.string.belum_diisi),
                        dominan = true,
                        modifier = Modifier
                            .clip(RoundedCornerShape(Radius.sedang))
                            .clickable { lembar = Lembar.Total }
                            .defaultMinSize(minHeight = Ukuran.sentuhMin),
                    )
                    when {
                        d.manual && d.status == StatusSimpan.TERHALANG ->
                            PesanPeriksa(stringResource(R.string.status_isi_manual), JenisPesan.INFO)
                        jumlahGalat == 0 -> PesanPeriksa(stringResource(R.string.status_cocok), JenisPesan.COCOK)
                        else -> PesanPeriksa(stringResource(R.string.status_perlu_cek, jumlahGalat), JenisPesan.GALAT)
                    }
                    // Konfirmasi tepat di bawah masalahnya; tombol simpan di bilah bawah baru aktif setelah dicentang.
                    if (d.status == StatusSimpan.PERLU_KONFIRMASI) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Radius.sedang))
                                .clickable { vm.setKonfirmasi(!d.konfirmasi) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = d.konfirmasi, onCheckedChange = vm::setKonfirmasi)
                            Text(
                                stringResource(R.string.konfirmasi_tidak_cocok),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
            if (d.contoh) {
                item { PesanPeriksa(stringResource(R.string.periksa_contoh), JenisPesan.INFO, Modifier.padding(bottom = Spasi.m)) }
            }
            d.catatanModel?.let { catatan ->
                item {
                    Text(
                        stringResource(R.string.catatan_ai, catatan),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Spasi.m),
                    )
                }
            }
            if (d.fotoPath != null) {
                item {
                    Column(Modifier.padding(bottom = Spasi.s)) {
                        TombolTeks(
                            teks = stringResource(if (tampilFoto) R.string.sembunyikan_foto else R.string.lihat_foto),
                            onClick = { tampilFoto = !tampilFoto },
                            sejajarMargin = true,
                        )
                        if (tampilFoto) FotoNota(d.fotoPath)
                    }
                }
            }
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.sedang))
                        .clickable { lembar = Lembar.Total }
                        .defaultMinSize(minHeight = Ukuran.sentuhMin),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spasi.m),
                ) {
                    Text(
                        d.nota.supplier?.let { stringResource(R.string.supplier) + ": " + it } ?: stringResource(R.string.supplier_kosong),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.ubah_total_supplier),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                Text(
                    stringResource(R.string.hasil_barang_judul),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = Spasi.l, bottom = Spasi.s),
                )
            }
            if (d.nota.items.isEmpty()) {
                item {
                    DataKosong(
                        jenis = JenisKosong.BELUM_ADA_DATA,
                        objek = stringResource(R.string.objek_barang),
                        teksAksi = stringResource(R.string.tambah_baris),
                        onAksi = vm::tambahBaris,
                    )
                }
            }
            itemsIndexed(d.nota.items) { i, item ->
                BarisDraf(
                    item = item,
                    isu = d.isuBaris(i),
                    pilihan = d.pilihan.getOrNull(i),
                    onUbah = { lembar = Lembar.Baris(i) },
                    onGantiProduk = { lembar = Lembar.Produk(i) },
                )
                PemisahBaris()
            }
            if (d.nota.items.isNotEmpty()) {
                item {
                    TombolTeks(
                        teks = stringResource(R.string.tambah_baris),
                        onClick = vm::tambahBaris,
                        modifier = Modifier.padding(top = Spasi.s),
                        ikon = Icons.Outlined.Add,
                        sejajarMargin = true,
                    )
                }
            }
            item { RingkasanTotal(d, onUbah = { lembar = Lembar.Total }) }
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
            shape = RoundedCornerShape(Radius.besar),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = Elevasi.tinggi,
            title = { Text(stringResource(R.string.duplikat_judul), style = MaterialTheme.typography.headlineSmall) },
            text = {
                Text(
                    stringResource(
                        R.string.duplikat_isi,
                        dup.total?.let(Rupiah::format) ?: "-",
                        dup.jumlahBaris,
                        formatTanggal(dup.dibuatPada),
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            confirmButton = { TombolTeks(stringResource(R.string.tetap_simpan), onClick = { vm.simpan(paksa = true) }) },
            dismissButton = { TombolTeks(stringResource(R.string.batal), onClick = vm::tutupDuplikat) },
        )
    }
}

/** Satu baris draf: [BarisBarang] + pesan pemeriksaan + produk tujuan. Daftar = baris + divider. */
@Composable
private fun BarisDraf(
    item: ItemBaris,
    isu: List<Isu>,
    pilihan: PilihanProduk?,
    onUbah: () -> Unit,
    onGantiProduk: () -> Unit,
) {
    Column(Modifier.padding(vertical = Spasi.xs), verticalArrangement = Arrangement.spacedBy(Spasi.xs)) {
        BarisBarang(
            nama = item.nama.ifBlank { stringResource(R.string.tanpa_nama) },
            qtyHarga = qtyHarga(item),
            jumlah = item.jumlah?.let(Rupiah::format) ?: "?",
            status = statusBaris(item, isu),
            onClick = onUbah,
        )
        for (i in isu) PesanPeriksa(i.pesan, jenisPesan(i))
        if (pilihan != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spasi.s)) {
                Text(
                    when (pilihan) {
                        is PilihanProduk.Ada -> stringResource(R.string.produk_ada, pilihan.produk.nama)
                        is PilihanProduk.Baru -> stringResource(R.string.produk_baru, pilihan.nama.ifBlank { "-" })
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TombolTeks(stringResource(R.string.ganti_produk), onClick = onGantiProduk)
            }
        }
    }
}

@Composable
private fun RingkasanTotal(d: DrafUi, onUbah: () -> Unit) {
    val n = d.nota
    Column(Modifier.padding(top = Spasi.xl), verticalArrangement = Arrangement.spacedBy(Spasi.xs)) {
        Text(
            stringResource(R.string.ringkasan_total),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = Spasi.s),
        )
        val jumlahBaris = if (n.items.all { it.jumlah != null }) n.items.sumOf { it.jumlah ?: 0L } else null
        BarisRingkasanTotal(stringResource(R.string.jumlah_semua_baris), jumlahBaris.rupiahAtauKosong())
        n.subtotal?.let { BarisRingkasanTotal(stringResource(R.string.subtotal_tertulis), it.rupiahAtauKosong()) }
        if (n.diskon != 0L) BarisRingkasanTotal(stringResource(R.string.diskon), (-n.diskon).rupiahAtauKosong())
        if (n.pajak != 0L) BarisRingkasanTotal(stringResource(R.string.pajak), n.pajak.rupiahAtauKosong())
        if (n.biayaLain != 0L) BarisRingkasanTotal(stringResource(R.string.biaya_lain), n.biayaLain.rupiahAtauKosong())
        for (i in d.isuNota) PesanPeriksa(i.pesan, jenisPesan(i))
        TombolTeks(stringResource(R.string.ubah_total), onClick = onUbah)
    }
}

@Composable
private fun Long?.rupiahAtauKosong(): String = this?.let(Rupiah::format) ?: stringResource(R.string.belum_diisi)

/**
 * Bilah aksi "Simpan nota". Bila tombol nonaktif, alasannya tertulis di atas tombol (bukan hanya tombol yang abu-abu);
 * galat simpan juga tampil di sini, dekat tombol yang baru ditekan.
 */
@Composable
private fun BarSimpan(d: DrafUi, onSimpan: () -> Unit) {
    BarAksiBawah(
        teksUtama = stringResource(if (d.menyimpan) R.string.menyimpan_nota else R.string.simpan_ke_stok),
        onUtama = onSimpan,
        utamaAktif = d.bolehSimpan,
        catatan = {
            when {
                d.namaKosong -> PesanPeriksa(stringResource(R.string.terhalang_nama), JenisPesan.GALAT)
                d.status == StatusSimpan.TERHALANG -> PesanPeriksa(stringResource(R.string.terhalang_lengkapi), JenisPesan.GALAT)
                d.status == StatusSimpan.PERLU_KONFIRMASI && !d.konfirmasi ->
                    PesanPeriksa(stringResource(R.string.terhalang_centang), JenisPesan.INFO)
            }
            if (d.galatSimpan) PesanPeriksa(stringResource(R.string.galat_simpan), JenisPesan.GALAT)
        },
    )
}

@Composable
private fun LayarTersimpan(s: HasilUi.Tersimpan, onLihatStok: () -> Unit, onCatatLagi: () -> Unit) {
    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spasi.l, vertical = Spasi.xl),
            verticalArrangement = Arrangement.spacedBy(Spasi.s),
        ) {
            ChipStatus(StatusBaris.COCOK, teks = stringResource(R.string.tersimpan_judul))
            BarisRingkasanTotal(
                label = stringResource(R.string.total_nota),
                nilai = Rupiah.format(s.total),
                dominan = true,
                modifier = Modifier.padding(top = Spasi.s),
            )
            Text(
                stringResource(R.string.tersimpan_isi, s.jumlahBaris),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (s.hasil.perubahan.isNotEmpty()) {
                Text(
                    stringResource(R.string.tersimpan_perubahan),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = Spasi.xl, bottom = Spasi.s),
                )
                for (p in s.hasil.perubahan) {
                    Column(Modifier.fillMaxWidth().padding(vertical = Spasi.s), verticalArrangement = Arrangement.spacedBy(Spasi.xs)) {
                        Text(p.namaProduk, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            Rupiah.format(p.hargaSekarang) + (p.satuan?.let { " / $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ChipSelisihHarga(
                            selisih = p.hargaSebelumnya?.let { p.hargaSekarang - it },
                            adaRiwayat = p.hargaSebelumnya != null,
                        )
                    }
                    PemisahBaris()
                }
            }
            TombolUtama(
                stringResource(R.string.lihat_stok),
                onClick = onLihatStok,
                modifier = Modifier.fillMaxWidth().padding(top = Spasi.xl),
            )
            TombolSekunder(stringResource(R.string.catat_nota_lain), onClick = onCatatLagi, modifier = Modifier.fillMaxWidth())
        }
    }
}
