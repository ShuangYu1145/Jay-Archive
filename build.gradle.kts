// AGP 9 built-in Kotlin: use the same Kotlin version as the Compose compiler.
buildscript {
    dependencies { classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20") }
}
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
}

