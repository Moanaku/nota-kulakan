package com.danibanan.notakulakan.ui.hasil

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danibanan.notakulakan.domain.PilihanProduk
import com.danibanan.notakulakan.domain.Produk
import com.danibanan.notakulakan.domain.RingkasNota
import com.danibanan.notakulakan.domain.StatusSimpan
import com.danibanan.notakulakan.domain.Validator
import com.danibanan.notakulakan.domain.model.HasilValidasi
import com.danibanan.notakulakan.domain.model.Isu
import com.danibanan.notakulakan.domain.model.ItemBaris
import com.danibanan.notakulakan.domain.model.KodeIsu
import com.danibanan.notakulakan.domain.model.Nota
import com.danibanan.notakulakan.domain.repo.Analitik
import com.danibanan.notakulakan.domain.repo.ContohRepository
import com.danibanan.notakulakan.domain.repo.DrafSimpan
import com.danibanan.notakulakan.domain.repo.EkstraksiRepository
import com.danibanan.notakulakan.domain.repo.HasilEkstraksi
import com.danibanan.notakulakan.domain.repo.HasilSimpan
import com.danibanan.notakulakan.domain.repo.IdContoh
import com.danibanan.notakulakan.domain.repo.Jam
import com.danibanan.notakulakan.domain.repo.JenisGagal
import com.danibanan.notakulakan.domain.repo.MetaEkstraksi
import com.danibanan.notakulakan.domain.repo.NotaRepository
import com.danibanan.notakulakan.domain.repo.PREFIKS_CONTOH
import com.danibanan.notakulakan.domain.repo.PenyiapFoto
import com.danibanan.notakulakan.domain.repo.barisKosong
import com.danibanan.notakulakan.domain.statusSimpan
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface HasilUi {
    data object Memproses : HasilUi
    data class Gagal(val jenis: JenisGagal, val bisaUlang: Boolean) : HasilUi
    data class Draf(val draf: DrafUi) : HasilUi
    data class Tersimpan(val hasil: HasilSimpan, val jumlahBaris: Int, val total: Long) : HasilUi
}

data class DrafUi(
    val nota: Nota,
    /** Isu format dari server untuk baris yang belum diubah pengguna (angka yang tak terbaca). */
    val isuFormatServer: List<Isu>,
    val pilihan: List<PilihanProduk>,
    val fotoPath: String?,
    val meta: MetaEkstraksi?,
    val catatanModel: String?,
    val manual: Boolean,
    val barisDiubah: Set<Int> = emptySet(),
    val konfirmasi: Boolean = false,
    val menyimpan: Boolean = false,
    val duplikat: RingkasNota? = null,
    val galatSimpan: Boolean = false,
    /** Nota contoh dari APK: hasil baca disiapkan, bukan hasil AI. Disimpan dengan supplier berawalan [PREFIKS_CONTOH]. */
    val contoh: Boolean = false,
) {
    /** Validasi lokal (cermin backend) digabung dengan isu format server yang masih berlaku. */
    val validasi: HasilValidasi = HasilValidasi(Validator.validasi(nota).isu + isuFormatServer)
    val status: StatusSimpan = statusSimpan(validasi)
    val namaKosong: Boolean = nota.items.any { it.nama.isBlank() }

    val bolehSimpan: Boolean
        get() = !menyimpan && !namaKosong && when (status) {
            StatusSimpan.BISA -> true
            StatusSimpan.PERLU_KONFIRMASI -> konfirmasi
            StatusSimpan.TERHALANG -> false
        }

    fun isuBaris(i: Int): List<Isu> = validasi.isu.filter { it.baris == i }
    val isuNota: List<Isu> get() = validasi.isu.filter { it.baris == null }
}

@HiltViewModel
class HasilViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val ekstraksi: EkstraksiRepository,
    private val contoh: ContohRepository,
    private val repo: NotaRepository,
    private val penyiapFoto: PenyiapFoto,
    private val analitik: Analitik,
    private val jam: Jam,
) : ViewModel() {

    private val uri: String? = savedStateHandle["uri"]
    private val idContoh: String? = savedStateHandle["contoh"]
    private val mulai = jam.sekarang()
    private var fotoTerkompres: File? = null
    private var tersimpan = false
    private var jobProses: Job? = null

    private val _ui = MutableStateFlow<HasilUi>(HasilUi.Memproses)
    val ui: StateFlow<HasilUi> = _ui.asStateFlow()

    private val _produk = MutableStateFlow<List<Produk>>(emptyList())
    val produk: StateFlow<List<Produk>> = _produk.asStateFlow()

    init {
        when {
            idContoh != null -> muatContoh(idContoh)
            uri == null -> mulaiManual()
            else -> proses()
        }
    }

    /** Nota contoh: dibaca dari assets APK lalu mengikuti jalur yang sama dengan hasil server (tanpa jaringan). */
    private fun muatContoh(kunci: String) {
        val id = IdContoh.entries.firstOrNull { it.berkas == kunci } ?: return mulaiManual()
        jobProses = viewModelScope.launch {
            try {
                val nota = contoh.muat(id)
                fotoTerkompres = nota.foto
                val items = nota.hasil.nota.items
                _ui.value = HasilUi.Draf(
                    DrafUi(
                        nota = nota.hasil.nota,
                        isuFormatServer = emptyList(),
                        pilihan = pilihanAwal(items),
                        fotoPath = nota.foto.absolutePath,
                        meta = null,
                        catatanModel = null,
                        manual = false,
                        contoh = true,
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("NotaContoh", "nota contoh gagal dibaca: ${e.javaClass.simpleName}")
                _ui.value = HasilUi.Gagal(JenisGagal.GANGGUAN_SERVER, bisaUlang = false)
            }
        }
    }

    /** Kompres foto (sekali) lalu kirim ke backend. Dipanggil lagi oleh tombol "Coba lagi". */
    fun proses() {
        val sumber = uri ?: return
        jobProses?.cancel()
        _ui.value = HasilUi.Memproses
        jobProses = viewModelScope.launch {
            val foto = fotoTerkompres ?: try {
                penyiapFoto.siapkan(sumber).also { fotoTerkompres = it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Tanpa log, penyebab galat foto tidak terlihat sama sekali. Hanya jenisnya yang dicatat:
                // pesan exception dari ContentResolver/berkas bisa memuat URI atau path foto.
                Log.w("FotoNota", "foto gagal disiapkan: ${e.javaClass.simpleName}")
                analitik.ekstraksiGagal(JenisGagal.FOTO_TIDAK_VALID)
                _ui.value = HasilUi.Gagal(JenisGagal.FOTO_TIDAK_VALID, bisaUlang = false)
                return@launch
            }
            when (val hasil = ekstraksi.ekstrak(foto)) {
                is HasilEkstraksi.Berhasil -> {
                    analitik.ekstraksiSelesai(
                        lolos = Validator.validasi(hasil.nota).lolos,
                        jumlahBaris = hasil.nota.items.size,
                        latensiMs = hasil.meta.latensiMs,
                        model = hasil.meta.model,
                    )
                    _ui.value = HasilUi.Draf(
                        DrafUi(
                            nota = hasil.nota,
                            isuFormatServer = hasil.isuServer.filter { it.kode == KodeIsu.FORMAT_TIDAK_DIKENALI },
                            pilihan = pilihanAwal(hasil.nota.items),
                            fotoPath = foto.absolutePath,
                            meta = hasil.meta,
                            catatanModel = hasil.catatan,
                            manual = false,
                        ),
                    )
                }
                is HasilEkstraksi.Gagal -> {
                    analitik.ekstraksiGagal(hasil.jenis)
                    val bisaUlang = hasil.jenis !in setOf(JenisGagal.BUKAN_NOTA, JenisGagal.FOTO_TIDAK_VALID, JenisGagal.FOTO_TERLALU_BESAR)
                    _ui.value = HasilUi.Gagal(hasil.jenis, bisaUlang)
                }
            }
        }
    }

    /** Jalan keluar bila foto tidak terbaca atau tidak ada internet: isi baris sendiri. */
    fun mulaiManual() {
        jobProses?.cancel()
        val nota = Nota(items = listOf(barisKosong()), total = null)
        _ui.value = HasilUi.Draf(
            DrafUi(
                nota = nota,
                isuFormatServer = emptyList(),
                pilihan = listOf(PilihanProduk.Baru("")),
                fotoPath = fotoTerkompres?.absolutePath,
                meta = null,
                catatanModel = null,
                manual = true,
            ),
        )
    }

    fun muatProduk() {
        viewModelScope.launch { _produk.value = repo.daftarProduk() }
    }

    fun ubahBaris(i: Int, baru: ItemBaris) {
        val lama = drafSekarang()?.nota?.items?.getOrNull(i) ?: return
        ubahDraf { d ->
            val items = d.nota.items.toMutableList().also { it[i] = baru }
            val pilihan = d.pilihan.toMutableList()
            // Nama berubah dan produk belum dipilih manual: ikuti nama baru.
            if (baru.nama != lama.nama && pilihan[i] is PilihanProduk.Baru) pilihan[i] = PilihanProduk.Baru(baru.nama.trim())
            d.copy(
                nota = d.nota.copy(items = items),
                pilihan = pilihan,
                barisDiubah = d.barisDiubah + i,
                isuFormatServer = d.isuFormatServer.filterNot { it.baris == i },
                konfirmasi = false,
            )
        }
        if (baru.nama != lama.nama) cariAlias(i, baru.nama)
    }

    fun tambahBaris() = ubahDraf { d ->
        d.copy(
            nota = d.nota.copy(items = d.nota.items + barisKosong()),
            pilihan = d.pilihan + PilihanProduk.Baru(""),
            barisDiubah = d.barisDiubah + d.nota.items.size,
            konfirmasi = false,
        )
    }

    fun hapusBaris(i: Int) = ubahDraf { d ->
        d.copy(
            nota = d.nota.copy(items = d.nota.items.filterIndexed { j, _ -> j != i }),
            pilihan = d.pilihan.filterIndexed { j, _ -> j != i },
            // Indeks bergeser; tandai semua yang tersisa setelah i sebagai berubah.
            barisDiubah = d.barisDiubah.filter { it < i }.toSet() + (i until d.nota.items.size - 1),
            isuFormatServer = emptyList(),
            konfirmasi = false,
        )
    }

    fun ubahTingkatNota(total: Long?, subtotal: Long?, diskon: Long, pajak: Long, biayaLain: Long, supplier: String?) =
        ubahDraf { d ->
            d.copy(
                nota = d.nota.copy(
                    total = total,
                    subtotal = subtotal,
                    diskon = diskon,
                    pajak = pajak,
                    biayaLain = biayaLain,
                    supplier = supplier?.trim()?.ifEmpty { null },
                ),
                isuFormatServer = d.isuFormatServer.filterNot { it.baris == null },
                konfirmasi = false,
            )
        }

    fun pilihProduk(i: Int, pilihan: PilihanProduk) = ubahDraf { d ->
        d.copy(pilihan = d.pilihan.toMutableList().also { it[i] = pilihan })
    }

    fun setKonfirmasi(ya: Boolean) = ubahDraf { it.copy(konfirmasi = ya) }

    fun tutupDuplikat() = ubahDraf { it.copy(duplikat = null) }

    /** Simpan ke stok. Tanpa `paksa`, cek dulu kemungkinan nota yang sama sudah tercatat. */
    fun simpan(paksa: Boolean = false) {
        val d = drafSekarang() ?: return
        if (!d.bolehSimpan) return
        ubahDraf { it.copy(menyimpan = true, galatSimpan = false, duplikat = null) }
        viewModelScope.launch {
            try {
                if (!paksa) {
                    val dup = repo.cariDuplikat(d.nota.total, d.nota.items.size, jam.sekarang())
                    if (dup != null) {
                        ubahDraf { it.copy(menyimpan = false, duplikat = dup) }
                        return@launch
                    }
                }
                val hasil = repo.simpan(
                    DrafSimpan(
                        nota = if (d.contoh) d.nota.copy(supplier = bertanda(d.nota.supplier)) else d.nota,
                        pilihan = d.pilihan,
                        fotoSementara = fotoTerkompres,
                        dikonfirmasiMeskiTidakCocok = d.status == StatusSimpan.PERLU_KONFIRMASI,
                        meta = d.meta,
                    ),
                )
                tersimpan = true
                // Nota contoh tidak dihitung di analitik: bukan pemakaian nyata.
                if (!d.contoh) {
                    analitik.notaDisimpan(
                        jumlahBaris = d.nota.items.size,
                        barisDikoreksi = d.barisDiubah.size,
                        dikonfirmasi = d.status == StatusSimpan.PERLU_KONFIRMASI,
                        durasiDetik = (jam.sekarang() - mulai) / 1000,
                        manual = d.manual,
                    )
                }
                _ui.value = HasilUi.Tersimpan(hasil, d.nota.items.size, d.nota.total ?: 0L)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ubahDraf { it.copy(menyimpan = false, galatSimpan = true) }
            }
        }
    }

    override fun onCleared() {
        if (!tersimpan) fotoTerkompres?.delete()
    }

    /** Supplier selalu berawalan [PREFIKS_CONTOH] (dengan sisa nama yang tidak kosong), walau pengguna mengubahnya. */
    private fun bertanda(supplier: String?): String =
        PREFIKS_CONTOH + supplier.orEmpty().removePrefix(PREFIKS_CONTOH).trim().ifEmpty { "Nota contoh" }

    private suspend fun pilihanAwal(items: List<ItemBaris>): List<PilihanProduk> {
        val saran = try {
            repo.saranProduk(items.map { it.nama })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            items.map { null }
        }
        return items.mapIndexed { i, item -> saran.getOrNull(i)?.let { PilihanProduk.Ada(it) } ?: PilihanProduk.Baru(item.nama.trim()) }
    }

    private fun cariAlias(i: Int, nama: String) {
        viewModelScope.launch {
            val produk = runCatching { repo.saranProduk(listOf(nama)).firstOrNull() }.getOrNull() ?: return@launch
            ubahDraf { d ->
                if (i < d.pilihan.size && d.pilihan[i] is PilihanProduk.Baru && d.nota.items[i].nama == nama) {
                    d.copy(pilihan = d.pilihan.toMutableList().also { it[i] = PilihanProduk.Ada(produk) })
                } else {
                    d
                }
            }
        }
    }

    private fun drafSekarang(): DrafUi? = (_ui.value as? HasilUi.Draf)?.draf

    private fun ubahDraf(f: (DrafUi) -> DrafUi) {
        _ui.update { s -> if (s is HasilUi.Draf) HasilUi.Draf(f(s.draf)) else s }
    }
}
