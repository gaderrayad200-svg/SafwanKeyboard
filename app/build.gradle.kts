import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val keystoreProps = Properties()
val keystorePropsFile = rootProject.file("app/keystore.properties")
if (keystorePropsFile.exists()) {
    keystoreProps.load(FileInputStream(keystorePropsFile))
}
fun signingProp(key: String): String? =
    keystoreProps.getProperty(key) ?: System.getenv(key)

android {
    namespace = "com.safwan.keyboard2027"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.safwan.keyboard2027"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = signingProp("KEYSTORE_FILE") ?: "keystore/safwan-release-key.jks"
            val storeFileObj = rootProject.file("app/$storeFilePath")
            if (storeFileObj.exists()) {
                storeFile = storeFileObj
                storePassword = signingProp("KEYSTORE_PASSWORD")
                keyAlias = signingProp("KEY_ALIAS")
                keyPassword = signingProp("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (rootProject.file("app/keystore/safwan-release-key.jks").exists() ||
                System.getenv("KEYSTORE_FILE") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}
