import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    buildFeatures {
        buildConfig = true
    }

    namespace = "com.example.ecosystems"
    compileSdk = 33

    // Чтение файла secrets.properties из корня проекта
    val secretsPropertiesFile = rootProject.file("secrets.properties")
    val secrets = Properties()
    if (secretsPropertiesFile.exists()) {
        secrets.load(FileInputStream(secretsPropertiesFile))
    }

    defaultConfig {
        applicationId = "com.example.ecosystems"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // ключ в BuildConfig и в Manifest placeholders
        buildConfigField("String", "YANDEX_MAPS_API_KEY", "\"${secrets["YANDEX_MAPS_API_KEY"]}\"")
        manifestPlaceholders["YANDEX_MAPS_API_KEY"] = secrets["YANDEX_MAPS_API_KEY"] as String

        buildConfigField("String", "BUTTON_TEXT", "\"${secrets["BUTTON_TEXT"]}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.google.code.gson:gson:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("com.google.crypto.tink:tink-android:1.13.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.8")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation("com.yandex.android:maps.mobile:4.19.0-lite")
}