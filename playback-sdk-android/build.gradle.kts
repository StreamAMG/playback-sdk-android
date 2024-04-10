import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import java.net.URL

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
    `maven-publish`
}

group = "com.streamamg"
version = "0.3.0"

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
        freeCompilerArgs = listOf("-Xinline-classes")
    }

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        aarMetadata {
            minCompileSdk = 24
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            consumerProguardFiles("consumer-rules.pro")
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
                val outputFileName = "${project.name}-${variant.name}-${defaultConfig.versionName}.aar"
                println("OutputFileName: $outputFileName")
                output.outputFileName = outputFileName
            }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

//publishing {
//    publications {
//        register("release", MavenPublication::class) {//            groupId = "com.streamamg"
//            groupId = "com.streamamg"
//            artifactId = "playback-sdk-android"
//            version = "0.3.0"
//        }

//        create<MavenPublication>("ReleaseAar") {
//            groupId = "com.streamamg"
//            artifactId = "playback-sdk-android"
//            version = "0.3"
//            afterEvaluate { artifact(tasks.getByName("bundleReleaseAar")) }
//        }
//        register("mavenJava", MavenPublication::class) {
//            pom {
//                description = "Playback SDK for Android"
//                withXml {
//                    val dependenciesNode = asNode().appendNode("dependencies")
//                    configurations.getByName("implementation") {
//                        dependencies.forEach {
//                            val dependencyNode = dependenciesNode.appendNode("dependency")
//                            dependencyNode.appendNode("groupId", it.group)
//                            dependencyNode.appendNode("artifactId", it.name)
//                            dependencyNode.appendNode("version", it.version)
//                        }
//                    }
//                }
//            }
//            artifact("${layout.buildDirectory}/outputs/aar/${artifactId}}-release.aar")
//        }

//    }
//    repositories {
//        mavenLocal()
//    }
//}

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
                rootProject.projectDir.resolve("serialization.package.list").toURI().toURL()
            )
        }
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk7", KotlinCompilerVersion.VERSION))
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