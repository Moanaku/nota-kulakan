# R8 untuk build rilis.
# Retrofit 3, OkHttp 4, Room, Hilt, dan Firebase membawa aturan consumer sendiri.

# kotlinx.serialization: pertahankan serializer kelas @Serializable (DTO dan rute navigasi).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class com.danibanan.notakulakan.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.danibanan.notakulakan.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Crashlytics: nama file dan nomor baris di stack trace (mapping diunggah plugin Crashlytics).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
