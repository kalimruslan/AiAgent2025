
plugins {
    alias(libs.plugins.frameio.kmplib)
    alias(libs.plugins.frameio.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(projects.llmDomain)
            implementation(projects.core.utils)
        }
    }
}

android {
    namespace = "ru.llm.agent.features.committee"
}