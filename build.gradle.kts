val logback_version: String by project
val kotlin_logging_version: String by project
val klerk_version: String by project
val sqlite_jdbc_version: String by project

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
    implementation(platform("dev.klerkframework:klerk-bom:$klerk_version"))
    implementation("dev.klerkframework:klerk")
    implementation("dev.klerkframework:klerk-web")
    implementation("dev.klerkframework:klerk-graphql")
    implementation("io.github.microutils:kotlin-logging-jvm:$kotlin_logging_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-server-html-builder-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-serialization")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.xerial:sqlite-jdbc:$sqlite_jdbc_version")
    implementation("io.modelcontextprotocol:kotlin-sdk:0.5.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.0.21")
}

kotlin {
    jvmToolchain(17)
}

tasks.jar { enabled = false }
