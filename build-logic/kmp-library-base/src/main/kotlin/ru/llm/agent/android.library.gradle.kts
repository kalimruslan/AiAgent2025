package ru.llm.agent

import ru.llm.agent.utils.ProjectTargets
import ru.llm.agent.utils.applyIfNeeded
import ru.llm.agent.utils.javaVersion
import ru.llm.agent.utils.jvmTarget
import ru.llm.agent.utils.kotlinJvmCompilerOptions
import ru.llm.agent.utils.libs
import org.gradle.kotlin.dsl.dependencies

plugins.apply("ru.llm.agent.android.base")
if (plugins.hasPlugin(libs.plugins.jetbrains.kotlin.multiplatform.get().pluginId)) {
    plugins.applyIfNeeded(libs.plugins.jetbrains.kotlin.android.get().pluginId)
}

project.dependencies {
    "implementation"(libs.kotlinx.coroutines.android)
    "implementation"(libs.androidx.core)
    "implementation"(libs.androidx.annotation)
}

kotlinJvmCompilerOptions {
    jvmTarget.set(libs.jvmTarget(ProjectTargets.Android))
    freeCompilerArgs.add("-Xjdk-release=${libs.javaVersion(ProjectTargets.Android)}")
    freeCompilerArgs.add("-Xexpect-actual-classes")
    freeCompilerArgs.add("-opt-in=org.koin.core.annotation.KoinExperimentalAPI")
}
