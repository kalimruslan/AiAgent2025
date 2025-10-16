plugins {
    alias(libs.plugins.frameio.kmplib)
}

kotlin {
    sourceSets {
    }

    explicitApi()
}

android {
    namespace = "ru.llm.agent.utils"
}
