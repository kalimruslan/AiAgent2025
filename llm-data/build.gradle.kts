import com.google.devtools.ksp.gradle.KspAATask
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import java.util.Properties

plugins {
    alias(libs.plugins.frameio.kmplib)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.androidx.room)
}

kotlin {
    explicitApi = ExplicitApiMode.Strict

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(projects.llmDomain)
            implementation(projects.core.utils)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.logging)

            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)

            implementation(libs.multiplatform.settings)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.engine.okhttp)
            implementation(libs.ktor.client.android)
            implementation(libs.androidx.room.ktx)
        }

        desktopMain.dependencies {
            implementation(libs.pdfbox)
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

dependencies{
    add("kspCommonMainMetadata", "androidx.room:room-compiler:2.7.2")
    add("kspAndroid", "androidx.room:room-compiler:2.7.2")
    add("kspDesktop", "androidx.room:room-compiler:2.7.2")
}

room {
    schemaDirectory("$projectDir/schemas")
}


