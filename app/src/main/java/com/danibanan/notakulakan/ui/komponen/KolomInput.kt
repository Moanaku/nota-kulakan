package com.danibanan.notakulakan.ui.komponen

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import com.danibanan.notakulakan.R
import com.danibanan.notakulakan.ui.theme.Radius
import com.danibanan.notakulakan.ui.theme.Spasi
import com.danibanan.notakulakan.ui.theme.Ukuran

@Composable
private fun kolomWarna() = MaterialTheme.colorScheme.let { w ->
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = w.onSurface,
        unfocusedTextColor = w.onSurface,
        errorTextColor = w.onSurface,
        focusedContainerColor = w.surface,
        unfocusedContainerColor = w.surface,
        errorContainerColor = w.surface,
        cursorColor = w.primary,
        errorCursorColor = w.error,
        focusedBorderColor = w.primary,
        unfocusedBorderColor = w.outline,
        errorBorderColor = w.error,
        focusedLabelColor = w.primary,
        unfocusedLabelColor = w.onSurfaceVariant,
        errorLabelColor = w.error,
        focusedPlaceholderColor = w.onSurfaceVariant,
        unfocusedPlaceholderColor = w.onSurfaceVariant,
        focusedPrefixColor = w.onSurfaceVariant,
        unfocusedPrefixColor = w.onSurfaceVariant,
        errorPrefixColor = w.onSurfaceVariant,
        focusedSupportingTextColor = w.onSurfaceVariant,
        unfocusedSupportingTextColor = w.onSurfaceVariant,
        errorSupportingTextColor = w.error,
    )
}

/** Pesan galat selalu ikon + kalimat, tidak hanya warna merah. */
@Composable
private fun PesanGalat(pesan: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Warning, contentDescription = null, modifier = Modifier.size(Ukuran.ikon))
        Text(pesan, modifier = Modifier.padding(start = Spasi.s), style = MaterialTheme.typography.bodySmall)
    }
}

/** Kolom teks biasa (nama barang, nama toko, satuan). */
@Composable
fun KolomTeks(
    nilai: String,
    onNilaiBerubah: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    galat: String? = null,
    bantuan: String? = null,
    satuBaris: Boolean = true,
) {
    OutlinedTextField(
        value = nilai,
        onValueChange = onNilaiBerubah,
        modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = Ukuran.sentuhMin),
        enabled = enabled,
        label = { Text(label) },
        textStyle = MaterialTheme.typography.bodyLarge,
        isError = galat != null,
        supportingText = (galat ?: bantuan)?.let { pesan ->
            { if (galat != null) PesanGalat(pesan) else Text(pesan) }
        },
        singleLine = satuBaris,
        shape = RoundedCornerShape(Radius.kecil),
        colors = kolomWarna(),
    )
}

/**
 * Kolom rupiah: pemilik state menyimpan [nilai] sebagai `Long?` (null = kosong), tidak pernah float.
 * Titik ribuan hanya tampilan; yang diketik hanya digit.
 */
@Composable
fun KolomRupiah(
    nilai: Long?,
    onNilaiBerubah: (Long?) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    galat: String? = null,
    bantuan: String? = null,
) {
    OutlinedTextField(
        value = nilai?.toString().orEmpty(),
        onValueChange = { masuk ->
            val digit = masuk.filter { it.isDigit() }.take(MAKS_DIGIT_RUPIAH)
            onNilaiBerubah(digit.toLongOrNull())
        },
        modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = Ukuran.sentuhMin),
        enabled = enabled,
        label = { Text(label) },
        prefix = { Text(stringResource(R.string.input_rupiah_awalan)) },
        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End),
        isError = galat != null,
        supportingText = (galat ?: bantuan)?.let { pesan ->
            { if (galat != null) PesanGalat(pesan) else Text(pesan) }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        visualTransformation = TitikRibuan,
        shape = RoundedCornerShape(Radius.kecil),
        colors = kolomWarna(),
    )
}

// Long maksimum 19 digit; 15 digit (di atas satu kuadriliun rupiah) sudah jauh melampaui nota mana pun.
private const val MAKS_DIGIT_RUPIAH = 15

/** "1234567" -> "1.234.567" tanpa mengubah teks yang disimpan. */
internal object TitikRibuan : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val asli = text.text
        val n = asli.length
        val hasil = StringBuilder()
        // keTampil[i] = posisi kursor di teks tampilan untuk posisi i di teks asli (0..n).
        val keTampil = IntArray(n + 1)
        asli.forEachIndexed { i, c ->
            if (i > 0 && (n - i) % 3 == 0) hasil.append('.')
            keTampil[i] = hasil.length
            hasil.append(c)
        }
        keTampil[n] = hasil.length
        val pemetaan = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = keTampil[offset.coerceIn(0, n)]
            override fun transformedToOriginal(offset: Int): Int {
                val o = offset.coerceIn(0, hasil.length)
                // Kursor di sebelah titik jatuh ke posisi asli terdekat sebelumnya.
                return keTampil.indexOfLast { it <= o }.coerceAtLeast(0)
            }
        }
        return TransformedText(AnnotatedString(hasil.toString()), pemetaan)
    }
}
