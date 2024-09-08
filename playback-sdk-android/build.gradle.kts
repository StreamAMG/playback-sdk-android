import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URI

buildscript {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        gradlePluginPortal()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.3.1")
    }
}

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization")
    id("org.jetbrains.dokka") version "1.9.20" apply true
    id("com.github.gmazzo.buildconfig") version "5.3.5"
    `maven-publish`
}

group = "com.streamamg"
version = "1.0.4"

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
        jvmTarget = "17"
    }

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        aarMetadata {
            minCompileSdk = 24
        }
    }

    buildConfig {
        buildConfigField("SDK_VERSION", provider { "${project.version}" })
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            consumerProguardFiles("consumer-rules.pro")
        }
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    libraryVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                val outputFileName = "${project.name}-${variant.name}-${version}.aar"
                println("OutputFileName: $outputFileName")
                output.outputFileName = outputFileName
            }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
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
            url.set(URI("https://www.streamamg.com/").toURL())
            packageListUrl.set(
                rootProject.projectDir.resolve("serialization.package.list").toURI().toURL()
            )
        }
    }
}

afterEvaluate {
    publishing {
        repositories {
            maven {
                url = uri("$project.layout.buildDirectory/repository")
            }
        }
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifactId = "playback-sdk-android"
            }
        }
    }
}

tasks.create<org.gradle.jvm.tasks.Jar>("releaseSourcesJar") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

dependencies {
    implementation("androidx.compose.runtime:runtime:1.6.2")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.compose.foundation:foundation-layout-android:1.6.2")
    implementation(platform("androidx.compose:compose-bom:2024.03.00"))
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("com.bitmovin.player:player:3.61.0")
    implementation("com.google.android.gms:play-services-cast-framework:21.4.0")
    implementation("com.google.accompanist:accompanist-permissions:0.28.0")
    implementation("com.mux.stats.sdk.muxstats:muxstatssdkbitmovinplayer_r3_11_1:0.5.1")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0-RC")
}