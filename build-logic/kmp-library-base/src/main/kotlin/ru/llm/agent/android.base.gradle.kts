package ru.llm.agent

import ru.llm.agent.utils.ProjectTargets
import ru.llm.agent.utils.androidConfig
import ru.llm.agent.utils.javaVersion
import ru.llm.agent.utils.libs
import ru.llm.agent.utils.config.requestedAndroidAbis

androidConfig {
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    buildToolsVersion = libs.versions.android.buildtools.get()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()

        ndk {
            requestedAndroidAbis.takeUnless { it.isNullOrEmpty() }?.let { abis: List<String> ->
                abiFilters.addAll(abis)
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    compileOptions {
        val javaVersion = libs.javaVersion(ProjectTargets.Android)
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
}

dependencies {
    "testImplementation"(libs.kotlin.test)
    "testImplementation"(libs.kotlin.test.junit)
    "testImplementation"(libs.junit)
    
    "implementation"(libs.koin.android)
    "implementation"(libs.androidx.core)
}
