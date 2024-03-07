// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.android.library") version "8.2.2" apply false

    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.10" apply true
    kotlin("jvm") version "1.9.22"
}

dependencies {
    implementation("androidx.compose.runtime:runtime:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}

