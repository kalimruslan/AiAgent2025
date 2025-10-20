package ru.llm.agent.common.app

import ru.llm.agent.core.utils.PlatformContext
import ru.llm.agent.core.utils.UrlLauncher
import org.koin.core.logger.Level
import org.koin.core.logger.Logger
import org.koin.core.logger.PrintLogger
import org.koin.core.module.Module
import org.koin.dsl.module
import ru.llm.agent.database.DatabaseDriverFactory
import ru.llm.agent.database.MessageDatabase

internal actual fun defaultKoinLogger(level: Level): Logger {
    return PrintLogger(level)
}

internal actual fun platformKoinModule(platformContext: PlatformContext): Module {
    return module {
        factory { platformContext }
        factory { UrlLauncher() }
        single<DatabaseDriverFactory> { DatabaseDriverFactory() }
        single<MessageDatabase> { get<DatabaseDriverFactory>().createDatabase() }
    }
}
