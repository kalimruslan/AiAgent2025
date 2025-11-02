import kotlin.text.set

plugins {
    kotlin("jvm")
    alias(libs.plugins.ktor)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    application
}

application {
    mainClass.set("ru.llm.agent.KtorServerKt")
}

dependencies {
    // Ktor Server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.websockets)

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
