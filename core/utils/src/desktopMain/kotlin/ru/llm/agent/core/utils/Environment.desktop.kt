package ru.llm.agent.core.utils

public actual val currentEnvironment: Environment = DesktopEnvironment

public object DesktopEnvironment : Environment {
    override val osName: String
        get() = System.getProperty("os.name") ?: ""

    override val osVersion: String
        get() = System.getProperty("os.version") ?: ""

    override val platform: Environment.Platform
        get() = Environment.Platform.DESKTOP
}
