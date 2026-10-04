plugins {
    id("com.android.application")
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
}
