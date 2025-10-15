import com.google.devtools.ksp.gradle.KspAATask
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import java.util.Properties

plugins {
    alias(libs.plugins.frameio.kmplib)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.google.ksp)
}

kotlin {
    explicitApi = ExplicitApiMode.Strict

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(projects.llmDomain)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.logging)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.engine.okhttp)
            implementation(libs.ktor.client.android)
        }
    }
}

android {
    namespace = "ru.llm.agent.sdk"
    defaultConfig {
        val localProps = Properties().apply {
            load(rootProject.file("local.properties").inputStream())
        }

        buildConfigField(
            "String",
            "OPENROUTER_API_KEY",
            "\"${localProps.getProperty("OPENROUTER_API_KEY")}\""
        )
        buildConfigField(
            "String",
            "YANDEX_API_KEY",
            "\"${localProps.getProperty("YANDEX_API_KEY")}\""
        )
        buildConfigField(
            "String",
            "PROXY_API_KEY",
            "\"${localProps.getProperty("PROXY_API_KEY")}\""
        )
    }

    buildFeatures {
        buildConfig = true
    }
}

afterEvaluate {
    tasks.withType<KspAATask>().configureEach {
        if (name != "kspCommonMainKotlinMetadata") {
            dependsOn(tasks.named("kspCommonMainKotlinMetadata"))
        }
    }
}

