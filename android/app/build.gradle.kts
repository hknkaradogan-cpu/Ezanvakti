import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

val versionCodeEnv = (System.getenv("VERSION_CODE") ?: "1").toInt()
val versionNameEnv = System.getenv("VERSION_NAME") ?: "1.0.0"

android {
    namespace = "com.nurvakti.global"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nurvakti.global"
        minSdk = 26
        targetSdk = 36
        versionCode = versionCodeEnv
        versionName = versionNameEnv
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "WEB_BASE", "\"https://hknkaradogan-cpu.github.io/Ezanvakti/\"")
    }

    // Release imzalama: anahtar GitHub Secrets'tan ortam değişkeni olarak gelir, depoda saklanmaz.
    val ksPath = System.getenv("SIGNING_KEYSTORE_PATH")
    signingConfigs {
        // Sabit debug anahtarı: her CI derlemesi aynı imzayı taşır, böylece test APK'sı üstüne güncellenebilir.
        getByName("debug") {
            storeFile = file("debug.keystore"); storePassword = "android"; keyAlias = "androiddebugkey"; keyPassword = "android"
        }
        if (ksPath != null && file(ksPath).exists()) {
            create("release") {
                storeFile = file(ksPath)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
        debug { versionNameSuffix = "-test" }
    }
    // Ortak içerik (Kur'an, dualar, konumlar, ses katalogları) web/data klasöründen gelir: tek kaynak, iki platform.
    sourceSets["main"].assets.srcDirs("src/main/assets", "../../web/data", "../../web/vendor", layout.buildDirectory.dir("generated/ezanAssets"))
    sourceSets["test"].resources.srcDirs("../../shared/tests")
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
    testOptions { unitTests.isReturnDefaultValues = true }
    lint { abortOnError = false; checkReleaseBuilds = false }
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.06.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.navigation:navigation-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-process:2.9.1")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")
    implementation("androidx.work:work-runtime-ktx:2.10.2")
    implementation("androidx.media3:media3-exoplayer:1.7.1")
    implementation("androidx.media3:media3-session:1.7.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}

// Uygulamaya gömülü (internetsiz) iki ezan kaydı: web/audio/ezan klasöründen kopyalanır.
val copyBundledEzan by tasks.registering(Copy::class) {
    from("../../web/audio/ezan") { include("klasik.mp3", "kisa.mp3") }
    into(layout.buildDirectory.dir("generated/ezanAssets/ezan"))
}
tasks.named("preBuild") { dependsOn(copyBundledEzan) }
