rootProject.name = "komf"

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        val kotlinVersion = "2.0.10"
        val agpVersion = "8.2.0"
        val mavenPublishVersion = "0.29.0"
        kotlin("jvm").version(kotlinVersion)
        kotlin("multiplatform").version(kotlinVersion)
        kotlin("plugin.serialization").version(kotlinVersion)
        kotlin("android").version(kotlinVersion)
        id("com.android.library").version(agpVersion)
        id("com.vanniktech.maven.publish").version(mavenPublishVersion)
    }
}
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

include(":komf-app")
include(":komf-core")
include(":komf-mediaserver")
include(":komf-notifications")
include(":komf-client")
include(":komf-api-models")
