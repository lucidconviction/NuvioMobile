plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "org.drinkless.tdlib"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.9.1")
}
