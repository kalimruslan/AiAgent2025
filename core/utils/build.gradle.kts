plugins {
    alias(libs.plugins.frameio.kmplib)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.multiplatform.settings.noarg)
        }
    }

    explicitApi()
}

android {
    namespace = "ru.llm.agent.utils"
}
