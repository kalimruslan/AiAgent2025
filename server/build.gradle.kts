import java.util.Properties
import kotlin.text.set

plugins {
    kotlin("jvm")
    alias(libs.plugins.ktor)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    application
}

// Загружаем local.properties
val localProperties = Properties().apply {
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        load(localPropsFile.inputStream())
    }
}

application {
    mainClass.set("ru.llm.agent.KtorServerKt")
}

dependencies {
    // MCP Kotlin SDK
    implementation(libs.mcp.kotlin.sdk)

    // Ktor Server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.websockets)

    // Ktor Client для внешних API запросов
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.engine.cio)
    implementation(libs.ktor.client.contentNegotiation)

    // Логирование
    implementation(libs.server.logback)
}

tasks {
    shadowJar {
        manifest {
            attributes["Main-Class"] = "ru.llm.agent.KtorServerKt"
        }
        archiveFileName.set("mcp-server.jar")
        mergeServiceFiles()
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
