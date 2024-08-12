import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow").version("8.1.1")
}

group = "io.github.snd-r"
version = "1.0.0-SNAPSHOT"

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}
java {
    targetCompatibility = JavaVersion.VERSION_17
    sourceCompatibility = JavaVersion.VERSION_17
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

val ktorVersion = "3.0.0-rc-1-eap-997"
val exposedVersion = "0.52.0"
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")

    implementation("ch.qos.logback:logback-core:1.5.6")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-cio:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-sse:$ktorVersion")


    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-encoding:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")


    implementation("com.charleskorn.kaml:kaml:0.59.0")

    implementation("org.apache.velocity:velocity-engine-core:2.3")

    implementation("com.microsoft.signalr:signalr:8.0.5")
    implementation("org.bitbucket.b_c:jose4j:0.9.6")

    implementation(project(":komf-core"))
    implementation(project(":komf-mediaserver"))
    implementation(project(":komf-notifications"))
    implementation(project(":komf-api-models"))
    implementation("io.github.snd-r:komga-client:0.1.0-SNAPSHOT")

}

tasks {
    shadowJar {
        manifest {
            attributes(Pair("Main-Class", "snd.komf.app.ApplicationKt"))
        }
    }
}

tasks.register("depsize") {
    description = "Prints dependencies for \"runtime\" configuration"
    doLast {
        listConfigurationDependencies(configurations["runtimeClasspath"])
    }
}

fun listConfigurationDependencies(configuration: Configuration) {
    val formatStr = "%,10.2f"

    val size = configuration.sumOf { it.length() / (1024.0 * 1024.0) }

    val out = StringBuffer()
    out.append("\nConfiguration name: \"${configuration.name}\"\n")
    if (size > 0) {
        out.append("Total dependencies size:".padEnd(65))
        out.append("${String.format(formatStr, size)} Mb\n\n")

        configuration.sortedBy { -it.length() }
            .forEach {
                out.append(it.name.padEnd(65))
                out.append("${String.format(formatStr, (it.length() / 1024.0))} kb\n")
            }
    } else {
        out.append("No dependencies found")
    }
    println(out)
}
