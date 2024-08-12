import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl


plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("kotlinx-atomicfu")
    id("com.vanniktech.maven.publish")
}

group = "io.github.snd-r"
version = "1.0.0"

val ktorVersion = "3.0.0-rc-1-eap-997"
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

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "komf-core"
    }

    sourceSets {
        commonMain.dependencies {
            implementation("io.github.oshai:kotlin-logging:7.0.0")

            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
            implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.5.1")


            implementation("io.ktor:ktor-client-core:$ktorVersion")
            implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
            implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
            implementation("io.ktor:ktor-client-encoding:$ktorVersion")

            implementation("io.github.pdvrieze.xmlutil:core:0.90.1")
            implementation("io.github.pdvrieze.xmlutil:serialization:0.90.1")

            implementation("com.fleeksoft.ksoup:ksoup:0.1.4")
        }
        androidMain.dependencies {
            implementation("app.cash.sqldelight:android-driver:2.0.2")
            implementation("org.apache.commons:commons-text:1.12.0")
        }

        val jvmMain by getting
        jvmMain.dependencies {
            implementation("com.squareup.okhttp3:okhttp:4.12.0")
            implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")
            implementation("org.apache.commons:commons-compress:1.26.2")
//            implementation("org.flywaydb:flyway-core:10.8.1")
            implementation("app.cash.sqldelight:sqlite-driver:2.0.2")

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


mavenPublishing {
    configure(
        KotlinMultiplatform(
            // configures the -javadoc artifact, possible values:
            // - `JavadocJar.None()` don't publish this artifact
            // - `JavadocJar.Empty()` publish an emprt jar
            // - `JavadocJar.Dokka("dokkaHtml")` when using Kotlin with Dokka, where `dokkaHtml` is the name of the Dokka task that should be used as input
            javadocJar = JavadocJar.Empty(),
            sourcesJar = true,
            androidVariantsToPublish = listOf("debug", "release"),
        )
    )
}
