package ru.llm.agent.core.utils

import android.os.Build

public actual val currentEnvironment: Environment = AndroidEnvironment

public object AndroidEnvironment : Environment {
    override val osName: String = "Android"

    override val platform: Environment.Platform
        get() = Environment.Platform.ANDROID

    override val osVersion: String
        get() = Build.VERSION.RELEASE
}
