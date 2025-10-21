@file:Suppress("UnstableApiUsage")


rootProject.name = "AiAgent2025"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
                includeGroupAndSubgroups("org.chromium.net")
                includeGroupAndSubgroups("com.crashlytics.sdk")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
                includeGroupAndSubgroups("org.chromium.net")
                includeGroupAndSubgroups("com.crashlytics.sdk")
            }
        }
        mavenCentral()
    }
}

private fun isDirectoryGradleModule(file: File): Boolean {
    val buildGradleFile = File(file, "build.gradle.kts")
    return file.isDirectory && buildGradleFile.isFile && buildGradleFile.exists()
}

private fun includeAllModules(dir: String) {
    file(dir)
        .listFiles { dirFile -> isDirectoryGradleModule(dirFile) }
        ?.forEach { dirFile -> include(":${dir.replace('/', ':')}:${dirFile.name}") }
}

includeAllModules(dir = "core")
include(":llm-app")
include(":llm-data")
include(":llm-domain")
include(":features:chat")
include(":features:conversation")
include(":server")
include(":features:addoptions")
