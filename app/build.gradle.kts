plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}
android {
    namespace = "love.yu.jaychou"
    compileSdk { version = release(37) }
    buildToolsVersion = "36.0.0"
    defaultConfig {
        applicationId = "love.yu.jaychou"
        minSdk = 26
        targetSdk = 37
        versionCode = 5
        versionName = "0.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        getByName("debug") {
            // 保留调试能力，不启用正式版混淆
            isMinifyEnabled = false
        }

        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }
    testOptions { unitTests.isReturnDefaultValues = true }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.compose.tooling.preview)
    implementation(libs.miuix.ui)
    implementation(libs.miuix.blur)
    implementation(libs.miuix.preference)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.compose)
    implementation(libs.datastore)
    implementation(libs.coil.compose)
    debugImplementation(libs.compose.tooling)
    debugImplementation(libs.compose.test.manifest)
    testImplementation(libs.junit)
    testImplementation(libs.test.json)
    androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.test.junit)
    androidTestImplementation(libs.test.runner)
    // Override Compose test's old Espresso dependency: 3.7 uses public InputManager APIs.
    androidTestImplementation(libs.espresso.core)
}

