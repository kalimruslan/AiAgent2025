package ru.llm.agent.utils.config

import org.gradle.api.Project

internal val supportedIOSTargetsParam = ProjectConfigParam(
    cmdParamName = "kmp.ios.platforms",
    androidLocalPropertyParamName = "kmp.ios.platforms",
    envParamName = "KMP_IOS_PLATFORMS",
    defaultValue = "arm64,simulatorArmX64,simulatorIntelX64",
)

enum class IOSPlatform(
    val value: String,
) {
    ARM_64("arm64"),
    SIMULATOR_ARM64("simulatorArmX64"),
    SIMULATOR_X64("simulatorIntelX64"),
}

//val Project.kmpIosPlatforms: List<IOSPlatform>
//    get() {
//        return readConfigParam(supportedIOSTargetsParam)?.split(",")?.map { paramValue ->
//            IOSPlatform.values().first { it.value == paramValue }
//        } ?: IOSPlatform.values().toList()
//    }
val Project.kmpIosPlatforms: List<IOSPlatform>
    get() {
        return emptyList()
    }
