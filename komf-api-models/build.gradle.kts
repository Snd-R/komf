@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.mavenPublish)
    signing
}

group = "io.github.snd-r"
version = libs.versions.app.version.get()

kotlin {
    jvmToolchain(17)
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        publishLibraryVariants("release")
    }
    jvm { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "komf-api-models"
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
        }
    }

}
android {
    namespace = "snd.komf"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

mavenPublishing {
    publishToMavenCentral( automaticRelease = false)
    coordinates("io.github.snd-r.komf", "api-models", libs.versions.app.version.get())
    signAllPublications()

    pom {
        name.set("Komf API models")
        description.set("Komf API models")
        url.set("https://github.com/Snd-R/komf")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://github.com/Snd-R/komf/blob/master/LICENSE")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("Snd-R")
                name.set("Snd-R")
                url.set("https://github.com/Snd-R")
            }
        }
        scm {
            url.set("https://github.com/Snd-R/komf")
            connection.set("scm:git:git://github.com/Snd-R/komf.git")
            developerConnection.set("scm:git:ssh://git@github.com/Snd-R/komf.git")
        }
    }
}
signing {
    useGpgCmd()
}
