plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.frameio.kmplib)
    alias(libs.plugins.jetbrains.compose.multiplatform)
    alias(libs.plugins.jetbrains.compose.compiler)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.components.uiToolingPreview)
            api(compose.material3AdaptiveNavigationSuite)
            api(compose.ui)
        }
    }
}

android {
    namespace = "ru.llm.agent.core.uikit"

    buildFeatures {
        compose = true
    }
}
