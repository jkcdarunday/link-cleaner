import java.util.Properties

plugins {
    id("com.android.application")
}

val signingProperties = Properties().apply {
    val propertiesFile = rootProject.file(".signing/keystore.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.jkcdarunday.linkcleaner"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jkcdarunday.linkcleaner"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val storePath = providers.environmentVariable("RELEASE_STORE_FILE").orNull
                ?: signingProperties.getProperty("storeFile")
            storeFile = storePath?.let { rootProject.file(it) }
            storePassword = providers.environmentVariable("RELEASE_STORE_PASSWORD").orNull
                ?: signingProperties.getProperty("storePassword")
            keyAlias = providers.environmentVariable("RELEASE_KEY_ALIAS").orNull
                ?: signingProperties.getProperty("keyAlias")
            keyPassword = providers.environmentVariable("RELEASE_KEY_PASSWORD").orNull
                ?: signingProperties.getProperty("keyPassword")
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
