@file:Suppress("OPT_IN_USAGE")

package ru.llm.agent

import ru.llm.agent.utils.ProjectTargets
import ru.llm.agent.utils.androidConfig
import ru.llm.agent.utils.applyIfNeeded
import ru.llm.agent.utils.jvmTarget
import ru.llm.agent.utils.kmpConfig
import ru.llm.agent.utils.libs

plugins.applyIfNeeded(libs.plugins.jetbrains.kotlin.multiplatform.get().pluginId)
plugins.applyIfNeeded(
    libs.plugins.android.library.get().pluginId,
    libs.plugins.android.application.get().pluginId,
)
plugins.applyIfNeeded("ru.llm.agent.kmp.library.base")

kmpConfig {
    applyDefaultHierarchyTemplate {
        common {
            group("nonAndroid") {
                withIos()
                withIosX64()
                withIosArm64()
                withIosSimulatorArm64()
                withNative()
                withJvm()
            }
        }
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(libs.jvmTarget(ProjectTargets.Android))
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.core)
            implementation(libs.kotlinx.coroutines.android)

            implementation(libs.koin.android)
        }

        named("nonAndroidMain")

        val androidUnitTest by getting
        androidUnitTest.dependencies {
            implementation(libs.kotlin.test.junit)
            implementation(libs.junit)
        }
    }
}

androidConfig {
    sourceSets["main"].apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        res.srcDirs("src/androidMain/res")
    }
}

plugins.apply("ru.llm.agent.android.base")
