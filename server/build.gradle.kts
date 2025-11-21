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
    // По умолчанию запускаем Ktor HTTP сервер
    mainClass.set("ru.llm.agent.KtorServerKt")
}

// Создаём Fat JAR вручную
tasks.register<Jar>("fatJarProxy") {
    group = "build"
    description = "Create fat JAR for Stdio Proxy"
    archiveFileName.set("mcp-server-proxy.jar")

    manifest {
        attributes["Main-Class"] = "ru.llm.agent.HttpToStdioProxyKt"
    }

    // Включаем все зависимости
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get())

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Задача для запуска HTTP-to-Stdio прокси (для удалённого сервера)
tasks.register<JavaExec>("runProxy") {
    group = "application"
    description = "Запускает HTTP-to-Stdio прокси для подключения к удалённому MCP серверу"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.llm.agent.HttpToStdioProxyKt")
    standardInput = System.`in`
    standardOutput = System.out
    errorOutput = System.err

    // URL удалённого сервера
    environment("REMOTE_MCP_SERVER_URL", localProperties.getProperty("REMOTE_MCP_SERVER_URL") ?: "https://kalimruslan-rt.ru")
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

    // Kotlinx Datetime для работы с датами
    implementation(libs.kotlinx.datetime)

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
