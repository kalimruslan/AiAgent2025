package ru.llm.agent.common.app

import ru.llm.agent.core.utils.PlatformContext
import ru.llm.agent.core.utils.UrlLauncher
import org.koin.android.logger.AndroidLogger
import org.koin.core.logger.Level
import org.koin.core.logger.Logger
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun defaultKoinLogger(level: Level): Logger {
    return AndroidLogger(level)
}

internal actual fun platformKoinModule(platformContext: PlatformContext): Module {
    return module {
        factory { UrlLauncher(platformContext) }
    }
}
