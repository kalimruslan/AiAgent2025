package ru.llm.agent

import ru.llm.agent.utils.androidConfig
import ru.llm.agent.utils.applyIfNeeded
import ru.llm.agent.utils.composeExt
import ru.llm.agent.utils.kmpConfig
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.invoke
import ru.llm.agent.utils.libs

plugins.applyIfNeeded("ru.llm.agent.jetbrains-compose.base")
plugins.apply("ru.llm.agent.jetpack-compose.base")

kmpConfig {
    androidConfig {
        buildFeatures {
            compose = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(composeExt.dependencies.preview)
            implementation(libs.jetbrains.compose.uiTooling.preview)
        }
    }
}

dependencies {
    "debugImplementation"(composeExt.dependencies.uiTooling)
}
