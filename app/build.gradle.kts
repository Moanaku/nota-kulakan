import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

// Firebase hanya dipasang bila google-services.json ada. Tanpa file itu aplikasi tetap bisa
// dibuild (CI, kontributor baru) dan berjalan terhadap backend lokal dengan APP_CHECK=off.
val adaGoogleServices = file("google-services.json").exists()
if (adaGoogleServices) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
}

val lokal = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun pengaturan(nama: String, bawaan: String): String =
    (project.findProperty(nama) as String?) ?: lokal.getProperty(nama) ?: bawaan

// Kunci upload untuk Play (Play App Signing memegang kunci aplikasi). File tidak di-commit.
val kunciRilis = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.danibanan.notakulakan"
    compileSdk = 36

    defaultConfig {
        // PERMANEN setelah unggahan pertama ke Play. Ganti sekarang bila perlu.
        applicationId = "com.danibanan.notakulakan"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        // Parameter kompresi = variabel evaluasi (lihat docs/EVALUASI.md). Ubah bersama.
        buildConfigField("int", "FOTO_MAX_SISI", "1024")
        buildConfigField("int", "FOTO_KUALITAS", "85")
    }

    signingConfigs {
        if (kunciRilis.isNotEmpty()) {
            create("rilis") {
                storeFile = rootProject.file(kunciRilis.getProperty("storeFile"))
                storePassword = kunciRilis.getProperty("storePassword")
                keyAlias = kunciRilis.getProperty("keyAlias")
                keyPassword = kunciRilis.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Sengaja tanpa applicationIdSuffix: satu paket saja yang perlu didaftarkan di Firebase.
            buildConfigField(
                "String", "API_BASE_URL",
                "\"${pengaturan("nota.api.url.debug", "http://10.0.2.2:8080/")}\"",
            )
            buildConfigField("boolean", "NOTA_PUNYA_SERVER", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Tanpa nota.api.url.release, APK rilis tidak punya server: kamera dan galeri nonaktif, mode contoh tetap ada
            // (docs/KEPUTUSAN.md 20). Alamat cadangan hanya agar Retrofit punya URL dasar yang sah.
            val urlRilis = pengaturan("nota.api.url.release", "")
            buildConfigField("String", "API_BASE_URL", "\"${urlRilis.ifBlank { "https://contoh.invalid/" }}\"")
            buildConfigField("boolean", "NOTA_PUNYA_SERVER", urlRilis.isNotBlank().toString())
            signingConfigs.findByName("rilis")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    lint {
        abortOnError = true
        checkDependencies = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

tasks.withType<Test>().configureEach {
    // Vektor uji bersama dengan backend Python: aturan hitungan kedua bahasa harus sama.
    systemProperty("nota.vektor", rootProject.file("shared/test-vectors.json").absolutePath)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.exifinterface)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    debugImplementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    debugImplementation(libs.firebase.appcheck.debug)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
