plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// İmzalama bilgileri ortam değişkenlerinden okunur (CI secrets veya yerel).
// KEYSTORE_PATH tanımlı değilse release APK imzasız üretilir.
val ksPath: String? = System.getenv("KEYSTORE_PATH")
val ksPass: String? = System.getenv("KEYSTORE_PASSWORD")
val ksAlias: String? = System.getenv("KEY_ALIAS")
val ksKeyPass: String? = System.getenv("KEY_PASSWORD")
val canSign = !ksPath.isNullOrBlank() && file(ksPath).exists() &&
    !ksPass.isNullOrBlank() && !ksAlias.isNullOrBlank() && !ksKeyPass.isNullOrBlank()

android {
    namespace = "com.kodlama.feridunmp3"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kodlama.feridunmp3"
        minSdk = 21
        targetSdk = 33
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "1.0.0"
        resourceConfigurations += listOf("en", "tr", "fa")
    }

    signingConfigs {
        if (canSign) {
            create("release") {
                storeFile = file(ksPath!!)
                storePassword = ksPass
                keyAlias = ksAlias
                keyPassword = ksKeyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (canSign) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    lint { abortOnError = false }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-session:1.2.1")
    implementation("com.google.guava:guava:32.1.3-android")
}
