package ru.llm.agent

import ru.llm.agent.utils.ProjectTargets
import ru.llm.agent.utils.applyIfNeeded
import ru.llm.agent.utils.jvmTarget
import ru.llm.agent.utils.kmpConfig
import ru.llm.agent.utils.libs
import org.gradle.kotlin.dsl.getting

plugins.applyIfNeeded(libs.plugins.jetbrains.kotlin.multiplatform.get().pluginId)
plugins.applyIfNeeded("ru.llm.agent.kmp.library.base")

kmpConfig {
    jvm("desktop") {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions.jvmTarget.set(libs.jvmTarget(ProjectTargets.Desktop))
            }
        }
    }

    sourceSets {
        val desktopMain by getting
        desktopMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.androidx.collection.ktx)
            implementation(libs.ktor.client.engine.okhttp)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.java)
        }

        val desktopTest by getting
        desktopTest.dependencies {
            implementation(libs.kotlin.test.junit)
        }
    }
}

