import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL

plugins {
    id("maven-publish")
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization")
    id("org.jetbrains.dokka") version "1.9.20" apply true
}

subprojects {
    apply(plugin = "org.jetbrains.dokka")
}

android {
    namespace = "com.streamamg.playback_sdk_android"
    compileSdk = 34

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.2"
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
tasks.dokkaGfm {
    outputDirectory.set(layout.projectDirectory.dir("docs/"))
}
tasks.dokkaHtmlPartial {
    outputDirectory.set(layout.projectDirectory.dir("docs/"))
}

tasks.withType<DokkaTask>().configureEach {
    moduleName.set(project.name)
    moduleVersion.set(project.version.toString())
    outputDirectory.set(layout.buildDirectory.dir("dokka/$name"))
    failOnWarning.set(false)
    suppressObviousFunctions.set(true)
    suppressInheritedMembers.set(false)
    offlineMode.set(false)
    dokkaSourceSets.configureEach {
        externalDocumentationLink {
            url.set(URL("https://www.streamamg.com/"))
            packageListUrl.set(
                rootProject.projectDir.resolve("serialization.package.list").toURL()
            )
        }
    }
}

dependencies {
    implementation("androidx.compose.runtime:runtime:1.6.2")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.compose.foundation:foundation-layout-android:1.6.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("com.bitmovin.player:player:3.61.0")
}