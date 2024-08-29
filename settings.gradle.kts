rootProject.name = "komf"

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
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
