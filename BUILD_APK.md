# Petunjuk Build APK – AI Solver

## 1. Build Debug APK (Siap Uji / Install Langsung)
Jalankan perintah berikut di root direktori proyek:
```bash
gradle assembleDebug
```
Output APK berada di:
`app/build/outputs/apk/debug/app-debug.apk`

## 2. Build Release APK
1. Siapkan keystore atau set environment variable `KEYSTORE_PATH`, `STORE_PASSWORD`, dan `KEY_PASSWORD`.
2. Jalankan:
```bash
gradle assembleRelease
```
Output APK berada di:
`app/build/outputs/apk/release/app-release.apk`

## 3. Build Android App Bundle (AAB untuk Google Play)
```bash
gradle bundleRelease
```
Output AAB berada di:
`app/build/outputs/bundle/release/app-release.aab`
