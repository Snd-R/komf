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
    }
}

include(":komf-app")
include(":komf-core")
include(":komf-mediaserver")
include(":komf-notifications")
include(":komf-client")
include(":komf-api-models")
