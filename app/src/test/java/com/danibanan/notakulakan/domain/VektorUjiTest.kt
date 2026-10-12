package com.danibanan.notakulakan.domain

import com.danibanan.notakulakan.domain.model.ItemBaris
import com.danibanan.notakulakan.domain.model.Nota
import java.io.File
import java.math.BigDecimal
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Menjalankan shared/test-vectors.json, file yang sama dengan backend/tests/test_vectors.py.
 * Kalau tes ini gagal sementara versi Python lolos, aturan kedua bahasa sudah menyimpang.
 */
class VektorUjiTest {

    private val vektor: JsonObject by lazy {
        val path = System.getProperty("nota.vektor")
            ?: error("System property nota.vektor tidak diisi (lihat app/build.gradle.kts)")
        Json.parseToJsonElement(File(path).readText()).jsonObject
    }

    private fun JsonObject.teks(kunci: String): String? = this[kunci]?.takeUnless { it is JsonNull }?.jsonPrimitive?.contentOrNull
    private fun JsonObject.long(kunci: String): Long? = this[kunci]?.takeUnless { it is JsonNull }?.jsonPrimitive?.longOrNull
    private fun JsonObject.galat(): Boolean = this["galat"]?.jsonPrimitive?.booleanOrNull == true

    @Test
    fun rupiah() {
        val kasus = vektor["rupiah"]!!.jsonArray
        assertTrue(kasus.size >= 20)
        for (k in kasus.map { it.jsonObject }) {
            val teks = k.teks("teks")!!
            if (k.galat()) {
                try {
                    val hasil = Rupiah.parse(teks)
                    fail("'$teks' seharusnya ditolak, malah terbaca $hasil")
                } catch (_: AngkaTidakValid) {
                }
            } else {
                assertEquals("'$teks'", k.long("hasil"), Rupiah.parse(teks))
            }
        }
    }

    @Test
    fun qty() {
        val kasus = vektor["qty"]!!.jsonArray
        assertTrue(kasus.size >= 10)
        for (k in kasus.map { it.jsonObject }) {
            val teks = k.teks("teks")!!
            if (k.galat()) {
                try {
                    val hasil = Qty.parse(teks)
                    fail("'$teks' seharusnya ditolak, malah terbaca $hasil")
                } catch (_: AngkaTidakValid) {
                }
            } else {
                assertEquals("'$teks'", 0, BigDecimal(k.teks("hasil")!!).compareTo(Qty.parse(teks)))
            }
        }
    }

    @Test
    fun validasi() {
        val kasus = vektor["validasi"]!!.jsonArray
        assertTrue(kasus.size >= 10)
        for (k in kasus.map { it.jsonObject }) {
            val nama = k.teks("nama")!!
            val hasil = Validator.validasi(notaDari(k["nota"]!!.jsonObject))
            val harapan = k["harapan"]!!.jsonObject
            val aktual = hasil.isu.map {
                listOf(it.kode.nilai, it.baris, it.diharapkan, it.ditemukan, it.tingkat.nilai)
            }
            val diharapkan = harapan["isu"]!!.jsonArray.map { e ->
                val o = e.jsonObject
                listOf(o.teks("kode"), o["baris"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.intOrNull, o.long("diharapkan"), o.long("ditemukan"), o.teks("tingkat"))
            }
            assertEquals(nama, diharapkan, aktual)
            assertEquals(nama, harapan["lolos"]!!.jsonPrimitive.booleanOrNull, hasil.lolos)
        }
    }

    private fun notaDari(o: JsonObject): Nota {
        val items = o["items"]!!.jsonArray.map { e: JsonElement ->
            val it = e.jsonObject
            ItemBaris(
                nama = it.teks("nama")!!,
                qty = it.teks("qty")?.let(::BigDecimal),
                satuan = it.teks("satuan"),
                hargaSatuan = it.long("harga_satuan"),
                jumlah = it.long("jumlah"),
                diskonBaris = it.long("diskon_baris") ?: 0,
                hargaDihitung = it["harga_dihitung"]?.jsonPrimitive?.booleanOrNull == true,
            )
        }
        return Nota(
            items = items,
            total = o.long("total"),
            subtotal = o.long("subtotal"),
            diskon = o.long("diskon") ?: 0,
            pajak = o.long("pajak") ?: 0,
            biayaLain = o.long("biaya_lain") ?: 0,
            pembulatan = o.long("pembulatan") ?: 0,
        )
    }
}
