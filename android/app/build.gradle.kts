import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.20"
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}
val swarmCloudToken = localProperties.getProperty("SWARMCLOUD_TOKEN", "")
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

android {
    namespace = "com.izplay.tv"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.izplay.tv"
        minSdk = 24
        targetSdk = 34
        versionCode = 66
        versionName = "2.1.45"
        vectorDrawables { useSupportLibrary = true }
        buildConfigField("String", "SWARMCLOUD_TOKEN", "\"$swarmCloudToken\"")
    }

    buildTypes {
        release {
            // R8 ligado: em TV box fraca o APK sem otimização do ART/R8 fica
            // visivelmente mais lento (Compose em especial).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Assina com a chave de debug: mesma assinatura dos APKs já instalados
            // nas boxes (fluxo assembleDebug + adb install), então atualiza por cima.
            // Trocar por keystore próprio quando houver assinatura oficial.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    implementation("androidx.activity:activity-compose:1.9.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Media3 / ExoPlayer for HLS/RTMP/TS playback
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.4.1")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // SwarmCloud P2P — used only for full-screen HLS live channels.
    implementation("com.swarmcloud:datachannel_native:latest.release")
    implementation("com.swarmcloud:p2p_engine:latest.release")
    implementation("com.orhanobut:logger:2.2.0")
    implementation("com.google.code.gson:gson:2.9.0")

    // JSON (Xtream API)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    // Image loading (channel logos)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Persist provider config
    implementation("androidx.datastore:datastore-preferences:1.1.1")
}
