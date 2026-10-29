# Rilis APK bertanda tangan

Langkah untuk Dani di PowerShell. Alasan: `docs/KEPUTUSAN.md` 17 (distribusi lewat GitHub Releases).

> **Peringatan.** Tanpa Play App Signing, keystore ini satu-satunya kunci aplikasi. Kalau berkas `.jks`
> atau sandinya hilang, versi baru tidak bisa dipasang menimpa versi lama; pengguna harus menghapus
> aplikasi dulu (data lokal ikut hilang). Keystore dan sandi tidak pernah masuk repo, chat, atau laporan.

## 1. Buat keystore (sekali saja, di luar repo)

```powershell
New-Item -ItemType Directory -Force "<folder keystore di luar repo>"
keytool -genkeypair -v -keystore "<folder keystore di luar repo>\nota-rilis.jks" -keyalg RSA -keysize 2048 -validity 10000 -alias rilis
```

`keytool` akan menanyakan sandi dan identitas (nama, dll.). Pilih sandi yang panjang. Kalau ditanya sandi
kunci, boleh sama dengan sandi keystore.

## 2. Simpan sandi dan cadangkan

- Simpan sandi dan alias (`rilis`) di pengelola sandi.
- Salin `nota-rilis.jks` ke **dua tempat berbeda** (mis. flashdisk dan penyimpanan awan pribadi).
  Jangan simpan sandi di folder yang sama dengan berkas `.jks`.

## 3. Isi `keystore.properties`

```powershell
Copy-Item keystore.properties.example keystore.properties
notepad keystore.properties
```

Isi:

```
storeFile=../keystore/nota-rilis.jks
storePassword=<sandi keystore>
keyAlias=rilis
keyPassword=<sandi kunci>
```

`storeFile` dibaca relatif terhadap akar repo, jadi `../keystore/nota-rilis.jks` menunjuk ke
`nota-rilis.jks` di `<folder keystore di luar repo>`. Pastikan `git status` tidak menampilkan `keystore.properties`.

## 4. Naikkan versi dan bangun

Naikkan `versionCode` dan `versionName` di `app/build.gradle.kts` untuk setiap rilis, lalu:

```powershell
./gradlew assembleRelease
```

Hasil: `app/build/outputs/apk/release/app-release.apk`. Tanpa `keystore.properties` hasilnya
`app-release-unsigned.apk` (tidak bisa dipasang).

## 5. Periksa tanda tangan

`apksigner` ada di folder build-tools Android SDK (sesuaikan versi):

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\<versi>\apksigner.bat" verify --print-certs app/build/outputs/apk/release/app-release.apk
```

Harus menampilkan sidik jari sertifikat (`Signer #1 certificate SHA-256 digest`) tanpa galat. Catat
SHA-256 itu; APK rilis berikutnya harus memiliki sidik jari yang sama.

## 6. Unggah

Buat rilis di GitHub Releases dan lampirkan `app-release.apk` (RUNBOOK Fase 6).
