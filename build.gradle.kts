import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"
    id("com.github.johnrengelman.shadow").version("7.1.2")
    id("com.google.devtools.ksp").version("1.7.20-1.0.7")
    id("org.flywaydb.flyway") version "9.4.0"
    id("nu.studer.jooq") version "7.1.1"
    id("com.apollographql.apollo3").version("3.6.2")
}

group = "org.snd"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("ch.qos.logback:logback-core:1.4.4")
    implementation("ch.qos.logback:logback-classic:1.4.4")
    implementation("org.slf4j:slf4j-api:2.0.3")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.2")

    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.okhttp3:okhttp-sse:4.10.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")
    implementation("com.squareup.moshi:moshi:1.14.0")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.14.0")
    implementation("com.apollographql.apollo3:apollo-runtime")

    implementation("io.javalin:javalin:5.1.1")

    implementation("io.github.resilience4j:resilience4j-ratelimiter:1.7.1")
    implementation("io.github.resilience4j:resilience4j-retry:1.7.1")
    implementation("io.github.resilience4j:resilience4j-kotlin:1.7.1")

    implementation("org.flywaydb:flyway-core:9.4.0")
    implementation("org.jooq:jooq:3.16.10")
    implementation("org.xerial:sqlite-jdbc:3.39.3.0")
    jooqGenerator("org.xerial:sqlite-jdbc:3.39.3.0")
    implementation("com.zaxxer:HikariCP:5.0.1")

    implementation("io.github.pdvrieze.xmlutil:core-jvm:0.84.3")
    implementation("io.github.pdvrieze.xmlutil:serialization-jvm:0.84.3")
    implementation("com.charleskorn.kaml:kaml:0.49.0")
    implementation("com.github.ajalt.clikt:clikt:3.5.0")
    implementation("commons-io:commons-io:2.11.0")
    implementation("org.apache.commons:commons-compress:1.21")
    implementation("org.apache.commons:commons-text:1.10.0")
    implementation("org.apache.velocity:velocity-engine-core:2.3")
    implementation("org.jsoup:jsoup:1.15.3")

}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlin.ExperimentalStdlibApi"
        )
    }
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
    version.set("3.16.10")
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
    packageName.set("org.snd")
}

tasks.wrapper {
    gradleVersion = "7.5.1"
    distributionType = Wrapper.DistributionType.ALL
}
