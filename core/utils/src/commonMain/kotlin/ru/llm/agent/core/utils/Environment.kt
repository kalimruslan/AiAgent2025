package ru.llm.agent.core.utils

public interface Environment {
    public val osName: String
    public val osVersion: String

    public val platform: Platform

    /**
     * Represents the platform for which the application is developed for.
     */
    public enum class Platform {
        ANDROID,
        IOS,
        DESKTOP,
    }

    public companion object {
        public val current: Environment
            get() = currentEnvironment
    }
}

public expect val currentEnvironment: Environment

/**
 * Executes the given [block] if the current platform matches the specified [platform].
 *
 * @param platform The platform to check against.
 * @param block The code block to execute if the platform matches.
 */
public inline fun onPlatform(
    platform: Environment.Platform,
    block: () -> Unit,
) {
    if (Environment.current.platform == platform) {
        block()
    }
}

public inline fun onDesktop(block: () -> Unit) {
    onPlatform(platform = Environment.Platform.DESKTOP, block = block)
}

public inline fun onAndroid(block: () -> Unit) {
    onPlatform(platform = Environment.Platform.ANDROID, block = block)
}

public inline fun onIOS(block: () -> Unit) {
    onPlatform(platform = Environment.Platform.IOS, block = block)
}

/**
 * Returns a value based on the current platform.
 *
 * @param onAndroid The value to return when the platform is Android.
 * @param onIOS The value to return when the platform is iOS.
 * @param onDesktop The value to return when the platform is Desktop.
 *
 * @return The value corresponding to the current platform.
 */
public fun <T> platformValue(
    onAndroid: (() -> T)? = null,
    onIOS: (() -> T)? = null,
    onDesktop: (() -> T)? = null,
): T {
    return when (Environment.current.platform) {
        Environment.Platform.ANDROID -> requireNotNull(onAndroid)
        Environment.Platform.IOS -> requireNotNull(onIOS)
        Environment.Platform.DESKTOP -> requireNotNull(onDesktop)
    }.invoke()
}
