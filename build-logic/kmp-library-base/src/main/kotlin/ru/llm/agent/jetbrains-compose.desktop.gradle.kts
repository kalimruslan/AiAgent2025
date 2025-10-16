package ru.llm.agent

import ru.llm.agent.utils.applyIfNeeded
import ru.llm.agent.utils.composeExt
import ru.llm.agent.utils.kmpConfig
import org.gradle.kotlin.dsl.getting

plugins.applyIfNeeded("ru.llm.agent.jetbrains-compose.base")

kmpConfig {
    sourceSets {
        val desktopMain by getting
        desktopMain.dependencies {
            implementation(composeExt.dependencies.desktop.common)
            implementation(composeExt.dependencies.desktop.currentOs)
        }
    }
}
