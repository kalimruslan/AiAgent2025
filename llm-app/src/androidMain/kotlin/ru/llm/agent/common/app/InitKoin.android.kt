package ru.llm.agent.common.app

import org.koin.android.ext.koin.androidContext
import ru.llm.agent.core.utils.PlatformContext
import ru.llm.agent.core.utils.UrlLauncher
import org.koin.android.logger.AndroidLogger
import org.koin.core.logger.Level
import org.koin.core.logger.Logger
import org.koin.core.module.Module
import org.koin.dsl.module
import ru.llm.agent.database.DatabaseDriverFactory
import ru.llm.agent.database.MessageDatabase

internal actual fun defaultKoinLogger(level: Level): Logger {
    return AndroidLogger(level)
}

internal actual fun platformKoinModule(platformContext: PlatformContext): Module {
    return module {
        factory { UrlLauncher(platformContext) }
        single<DatabaseDriverFactory> { DatabaseDriverFactory(androidContext()) }
        single<MessageDatabase> { get<DatabaseDriverFactory>().createDatabase() }
    }
}
