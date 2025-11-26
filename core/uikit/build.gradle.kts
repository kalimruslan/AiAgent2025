plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.frameio.kmplib)
    alias(libs.plugins.jetbrains.compose.multiplatform)
    alias(libs.plugins.jetbrains.compose.compiler)
}

compose.resources {
    // Делаем ресурсы публичными для использования в других модулях
    publicResClass = true
    // Кастомный пакет для более удобного импорта
    packageOfResClass = "ru.llm.agent.core.uikit.resources"
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
            // Compose Multiplatform Resources
            api(compose.components.resources)
        }
    }
}

android {
    namespace = "ru.llm.agent.core.uikit"

    buildFeatures {
        compose = true
    }
}
