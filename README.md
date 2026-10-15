# Feridun-Mp3 (Android MP3 Player)

Samsung Galaxy M10 (Android 10) hedefli, hafif MP3/M4A/WAV oynatıcı. Türkçe, Farsça (RTL) ve İngilizce.

## Mimari ve kararlar
- **minSdk 21**: Media3, AppCompat per-app dil API'si (setApplicationLocales) ve vektör çizimler API 21'den itibaren çalışır; en geniş cihaz kapsamı. M10 (API 29) rahatça üstündedir.
- **compileSdk 34 / targetSdk 33**: Android 10'da geriye dönük uyumlu; scoped storage uyumlu (yalnızca MediaStore, ham dosya yolu yok); API 33 izinleri (READ_MEDIA_AUDIO, POST_NOTIFICATIONS) kullanılır.
- **Media3 (ExoPlayer + MediaSessionService)**: Ön plan servisi, bildirim, kilit ekranı kontrolleri ve ses odağını otomatik yönetir. Küçük tampon (2-8 sn, 2 MB) ile düşük RAM. Compose/Material kütüphanesi yok, yalnızca AppCompat + RecyclerView.
- **UI**: tek ekranlı liste + alt oynatıcı paneli; ilerleme güncellemesi yalnızca çalarken ve ekran açıkken 500 ms'de bir.
- **i18n**: values (en), values-tr, values-fa; `supportsRtl=true`, yalnızca start/end; ayarlardan Sistem/EN/TR/FA seçimi; API 32 ve altında seçim AppCompat servisiyle kalıcı saklanır, API 33+ sistem dil ayarıyla eşlenir (locales_config.xml).

## Dosya ağacı
```
Feridun-Mp3/
├─ .github/workflows/release.yml
├─ build.gradle.kts, settings.gradle.kts, gradle.properties
├─ gradlew, gradle/wrapper/{gradle-wrapper.jar,gradle-wrapper.properties}
└─ app/
   ├─ build.gradle.kts, proguard-rules.pro
   └─ src/main/
      ├─ AndroidManifest.xml
      ├─ java/com/kodlama/feridunmp3/{MainActivity,PlaybackService,TrackRepository,TrackAdapter,SettingsActivity}.kt
      └─ res/{layout,drawable,menu,xml,values,values-tr,values-fa}
```

## Adım adım: APK'yı GitHub Release'e koyma
1. **Keystore üret** (bir kez, güvenli yerde sakla; kaybedersen güncelleme yayınlayamazsın):
   ```
   keytool -genkeypair -v -keystore release.jks -alias feridun -keyalg RSA -keysize 2048 -validity 10000
   ```
2. **Base64'e çevir**: Linux/macOS `base64 -w0 release.jks > keystore.b64` (macOS: `base64 -i release.jks -o keystore.b64`); Windows PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.jks")) | Set-Content keystore.b64`.
3. **GitHub Secrets ekle** (Repo → Settings → Secrets and variables → Actions → New repository secret):
   `KEYSTORE_BASE64` (keystore.b64 içeriği), `KEYSTORE_PASSWORD`, `KEY_ALIAS` (örn. feridun), `KEY_PASSWORD`.
   Secrets eklemezsen workflow **imzasız** APK üretir (`...-unsigned.apk`, yüklemeden önce imzalanmalı).
4. **Projeyi repoya yükle** (`Feridun-Mp3` reposunun köküne) ve push et.
5. **Tag at**:
   ```
   git tag v1.0.0
   git push origin v1.0.0
   ```
   (Alternatif: GitHub'da Releases → Draft a new release → tag oluştur → Publish.)
6. Actions sekmesinde "Release APK" çalışır; bitince APK ilgili Release'in Assets bölümünde görünür.
7. Telefonda APK'yı indir, "bilinmeyen kaynaklardan yükleme" iznini ver ve kur.

## Yerel derleme
JDK 17 + Android SDK ile: `./gradlew assembleDebug` (APK: app/build/outputs/apk/debug). `local.properties` içine `sdk.dir=...` yazın.
