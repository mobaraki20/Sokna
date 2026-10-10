plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.excelpricesearch"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.excelpricesearch"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"
    }
}

dependencies {
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
}
