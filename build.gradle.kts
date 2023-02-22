import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.21"
    kotlin("plugin.serialization") version "1.7.21"
    id("com.github.johnrengelman.shadow").version("7.1.2")
    id("com.google.devtools.ksp").version("1.7.21-1.0.8")
    id("org.flywaydb.flyway") version "9.10.2"
    id("nu.studer.jooq") version "8.1"
    id("com.apollographql.apollo3").version("3.7.3")
}

group = "org.snd"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("ch.qos.logback:logback-core:1.4.5")
    implementation("ch.qos.logback:logback-classic:1.4.5")
    implementation("org.slf4j:slf4j-api:2.0.5")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.4")

    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.okhttp3:okhttp-sse:4.10.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")
    implementation("com.squareup.moshi:moshi:1.14.0")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.14.0")
    implementation("com.apollographql.apollo3:apollo-runtime")

    implementation("io.javalin:javalin:5.3.2")

    implementation("io.github.resilience4j:resilience4j-ratelimiter:2.0.2")
    implementation("io.github.resilience4j:resilience4j-retry:2.0.2")
    implementation("io.github.resilience4j:resilience4j-kotlin:2.0.2")

    implementation("org.flywaydb:flyway-core:9.15.0")
    implementation("org.jooq:jooq:3.17.8")
    implementation("org.xerial:sqlite-jdbc:3.40.1.0")
    jooqGenerator("org.xerial:sqlite-jdbc:3.40.1.0")
    implementation("com.zaxxer:HikariCP:5.0.1")

    implementation("io.github.pdvrieze.xmlutil:core-jvm:0.85.0")
    implementation("io.github.pdvrieze.xmlutil:serialization-jvm:0.85.0")
    implementation("com.charleskorn.kaml:kaml:0.51.0")
    implementation("com.github.ajalt.clikt:clikt:3.5.1")
    implementation("commons-io:commons-io:2.11.0")
    implementation("org.apache.commons:commons-compress:1.21")
    implementation("org.apache.commons:commons-text:1.10.0")
    implementation("org.apache.velocity:velocity-engine-core:2.3")
    implementation("org.jsoup:jsoup:1.15.4")
    implementation("com.microsoft.signalr:signalr:6.0.10")
    implementation("org.bitbucket.b_c:jose4j:0.9.3")

}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlin.ExperimentalStdlibApi"
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

tasks {
    shadowJar {
        manifest {
            attributes(Pair("Main-Class", "org.snd.ApplicationKt"))
        }
    }
}

sourceSets {
    // add a flyway sourceSet
    val flyway by creating {
        compileClasspath += sourceSets.main.get().compileClasspath
        runtimeClasspath += sourceSets.main.get().runtimeClasspath
    }
    // main sourceSet depends on the output of flyway sourceSet
    main {
        output.dir(flyway.output)
    }
}

val dbSqlite = mapOf(
    "url" to "jdbc:sqlite:${project.buildDir}/generated/flyway/database.sqlite"
)
val migrationDirsSqlite = listOf(
    "$projectDir/src/flyway/resources/db/migration/sqlite",
)
flyway {
    url = dbSqlite["url"]
    locations = arrayOf("classpath:db/migration/sqlite")
}
tasks.flywayMigrate {
    // in order to include the Java migrations, flywayClasses must be run before flywayMigrate
    dependsOn("flywayClasses")
    migrationDirsSqlite.forEach { inputs.dir(it) }
    outputs.dir("${project.buildDir}/generated/flyway")
    doFirst {
        delete(outputs.files)
        mkdir("${project.buildDir}/generated/flyway")
    }
    mixed = true
}

jooq {
    version.set("3.17.8")
    configurations {
        create("main") {
            jooqConfiguration.apply {
                logging = org.jooq.meta.jaxb.Logging.WARN
                jdbc.apply {
                    driver = "org.sqlite.JDBC"
                    url = dbSqlite["url"]
                }
                generator.apply {
                    database.apply {
                        name = "org.jooq.meta.sqlite.SQLiteDatabase"
                    }
                    target.apply {
                        packageName = "org.snd.jooq"
                    }
                }
            }
        }
    }
}

tasks.named<nu.studer.gradle.jooq.JooqGenerate>("generateJooq") {
    migrationDirsSqlite.forEach { inputs.dir(it) }
    allInputsDeclared.set(true)
    dependsOn("flywayMigrate")
}

apollo {
    service("service") {
        packageName.set("org.snd")
    }
}

tasks.wrapper {
    gradleVersion = "7.6"
    distributionType = Wrapper.DistributionType.ALL
}
