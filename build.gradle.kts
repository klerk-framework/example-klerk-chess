val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val kotlin_logging_version: String by project
val klerk_version: String by project
val klerk_web_version: String by project
val klerk_graphql_version: String by project
val sqlite_jdbc_version: String by project
val graphql_version: String by project

plugins {
    kotlin("jvm") version "2.0.21"
    id("io.ktor.plugin") version "2.3.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.6.10"
    id("com.expediagroup.graphql") version "7.1.1"
}

group = "dev.klerkframework.chess"
version = "0.0.1"

application {
    mainClass.set("dev.klerkframework.chess.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation("com.github.klerk-framework:klerk:$klerk_version")
    implementation("com.github.klerk-framework:klerk-web:$klerk_web_version")
    implementation("com.github.klerk-framework:klerk-graphql:$klerk_graphql_version")
    implementation("com.expediagroup:graphql-kotlin-ktor-server:$graphql_version")
    implementation("io.github.microutils:kotlin-logging-jvm:$kotlin_logging_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-server-html-builder-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-serialization:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.xerial:sqlite-jdbc:$sqlite_jdbc_version")
    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}

kotlin {
    jvmToolchain(17)
}

tasks.jar { enabled = false }
