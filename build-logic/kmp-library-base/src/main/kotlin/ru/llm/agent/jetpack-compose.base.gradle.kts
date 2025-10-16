package ru.llm.agent

import ru.llm.agent.utils.androidConfig
import ru.llm.agent.utils.applyIfNeeded
import ru.llm.agent.utils.libs

plugins.applyIfNeeded(libs.plugins.jetbrains.compose.compiler.get().pluginId)

androidConfig {
    buildFeatures {
        compose = true
    }
}
