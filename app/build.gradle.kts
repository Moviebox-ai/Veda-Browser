import java.io.FileInputStream
import java.util.Base64
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Load release keystore credentials from keystore.properties or environment variables
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(FileInputStream(keystorePropertiesFile))
    }
}

val releaseStoreFilePath: String? = keystoreProperties.getProperty("storeFile")
    ?: System.getenv("KEYSTORE_PATH")
val releaseStorePassword: String = keystoreProperties.getProperty("storePassword")
    ?: System.getenv("KEYSTORE_PASSWORD")
    ?: "android"
val releaseKeyAlias: String = keystoreProperties.getProperty("keyAlias")
    ?: System.getenv("KEY_ALIAS")
    ?: "androiddebugkey"
val releaseKeyPassword: String = keystoreProperties.getProperty("keyPassword")
    ?: System.getenv("KEY_PASSWORD")
    ?: "android"

fun resolveDebugKeystore(): File {
    val standardKeystore = file("${rootDir}/debug.keystore")
    if (standardKeystore.exists() && standardKeystore.length() > 0L) {
        return standardKeystore
    }

    val base64Keystore = file("${rootDir}/debug.keystore.base64")
    if (base64Keystore.exists()) {
        try {
            val decoded = Base64.getDecoder().decode(base64Keystore.readText().trim())
            standardKeystore.writeBytes(decoded)
            if (standardKeystore.exists() && standardKeystore.length() > 0L) {
                return standardKeystore
            }
        } catch (_: Exception) {
        }
    }

    val userHomeKeystore = file("${System.getProperty("user.home")}/.android/debug.keystore")
    if (userHomeKeystore.exists() && userHomeKeystore.length() > 0L) {
        return userHomeKeystore
    }

    val javaHome = System.getProperty("java.home")
    val keytoolExecutable = if (!javaHome.isNullOrEmpty() && file("$javaHome/bin/keytool").exists()) {
        file("$javaHome/bin/keytool").absolutePath
    } else {
        "keytool"
    }

    // Attempt generation at standardKeystore location first
    try {
        val process = ProcessBuilder(
            keytoolExecutable,
            "-genkeypair",
            "-alias", "androiddebugkey",
            "-keypass", "android",
            "-keystore", standardKeystore.absolutePath,
            "-storepass", "android",
            "-dname", "CN=Android Debug,O=Android,C=US",
            "-keyalg", "RSA",
            "-keysize", "2048",
            "-validity", "10000"
        ).redirectErrorStream(true).start()
        process.waitFor()
        if (standardKeystore.exists() && standardKeystore.length() > 0L) {
            return standardKeystore
        }
    } catch (_: Exception) {
    }

    // Fallback logic: Generate a temporary debug keystore if standard debug keystores are missing
    val tempKeystore = File(System.getProperty("java.io.tmpdir"), "velocity_temp_debug.keystore")
    if (!tempKeystore.exists() || tempKeystore.length() == 0L) {
        try {
            val process = ProcessBuilder(
                keytoolExecutable,
                "-genkeypair",
                "-alias", "androiddebugkey",
                "-keypass", "android",
                "-keystore", tempKeystore.absolutePath,
                "-storepass", "android",
                "-dname", "CN=Android Debug,O=Android,C=US",
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-validity", "10000"
            ).redirectErrorStream(true).start()
            process.waitFor()
        } catch (_: Exception) {
        }
    }

    if (tempKeystore.exists() && tempKeystore.length() > 0L) {
        return tempKeystore
    }

    return standardKeystore
}

val resolvedDebugKeystore = resolveDebugKeystore()

val releaseStoreFile = if (!releaseStoreFilePath.isNullOrEmpty() && file(releaseStoreFilePath).exists()) {
    file(releaseStoreFilePath)
} else {
    resolvedDebugKeystore
}

android {
    namespace = "com.aaryo.velocity"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aaryo.velocity"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = releaseStoreFile
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
        getByName("debug") {
            storeFile = resolvedDebugKeystore
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.okhttp)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.gson)

    testImplementation(libs.junit)
}
