package ru.llm.agent.utils.config

import org.gradle.api.Project

internal val requestedAndroidAbisParam = ProjectConfigParam(
    cmdParamName = "android.ndk.abis",
    androidLocalPropertyParamName = "android.ndk.abis",
    envParamName = "ANDROID_NDK_ABIS",
)

val Project.requestedAndroidAbis: List<String>?
    get() = readConfigParam(requestedAndroidAbisParam)?.split(",")
