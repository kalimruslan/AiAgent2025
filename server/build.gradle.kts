plugins {
    kotlin("jvm")
    alias(libs.plugins.ktor)
    application
}

application {
    mainClass.set("ru.llm.agent.ApplicationKt")
}

dependencies {
    // Ktor Server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Логирование
    implementation(libs.server.logback)
}

tasks {
    shadowJar {
        manifest {
            attributes["Main-Class"] = "ru.llm.agent.ApplicationKt"
        }
        archiveBaseName.set("agentAi")
        archiveClassifier.set("")
        archiveVersion.set("")
    }
}
