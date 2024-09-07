import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAtomicfu)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

group = "io.github.snd-r"
version = libs.versions.app.version.get()

kotlin {
    jvmToolchain(17)
    jvm {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

//    @OptIn(ExperimentalWasmDsl::class)
//    wasmJs {
//        moduleName = "komf-core"
//    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.cache4k)
            implementation(libs.commons.compress)
            implementation(libs.commons.text)
            implementation(libs.kotlin.logging)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.io.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.encoding)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ksoup)
            implementation(libs.xmlutil.core)
            implementation(libs.xmlutil.serialization)

        }

        val jvmMain by getting
        jvmMain.dependencies {
            implementation(libs.twelvemonkeys.imageio.core)
            implementation(libs.twelvemonkeys.imageio.jpeg)
            implementation(libs.twelvemonkeys.imageio.webp)

        }
    }

}

android {
    namespace = "snd.komf"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}
