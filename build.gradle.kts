val logbackVersion: String by project
val kotlinLoggingVersion: String by project
val klerkBomVersion: String by project
val sqliteJdbcVersion: String by project

plugins {
    kotlin("jvm") version "2.3.10"
    id("io.ktor.plugin") version "3.1.2"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
}

group = "dev.klerkframework.chess"
version = "0.0.1"

application {
    mainClass.set("dev.klerkframework.chess.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(platform("dev.klerkframework:klerk-bom:$klerkBomVersion"))
    implementation("dev.klerkframework:klerk")
    implementation("dev.klerkframework:klerk-web:1.0.0-alpha.2-SNAPSHOT")
    implementation("dev.klerkframework:klerk-graphql")

    implementation("com.expediagroup:graphql-kotlin-ktor-server:9.1.0")   // TODO: remove since it is provided as an API

    implementation("io.github.microutils:kotlin-logging-jvm:$kotlinLoggingVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-server-html-builder-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-serialization")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.xerial:sqlite-jdbc:$sqliteJdbcVersion")
    implementation("io.modelcontextprotocol:kotlin-sdk:0.5.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.0.21")
}

kotlin {
    jvmToolchain(17)
}

tasks.jar { enabled = false }
