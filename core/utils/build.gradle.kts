plugins {
    alias(libs.plugins.frameio.kmplib)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.multiplatform.settings.noarg)
        }

        desktopMain.dependencies {
            implementation(libs.pdfbox)
            implementation(libs.apache.poi)
            implementation(libs.apache.poi.ooxml)
            implementation(libs.apache.poi.scratchpad)
        }
    }

    explicitApi()
}

android {
    namespace = "ru.llm.agent.utils"
}
