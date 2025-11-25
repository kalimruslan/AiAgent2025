package ru.llm.agent.common.app

import ru.llm.agent.core.utils.PlatformContext
import org.koin.core.KoinApplication
import org.koin.core.logger.Level
import org.koin.core.logger.Logger
import org.koin.core.module.Module
import ru.llm.agent.di.networkModule
import ru.llm.agent.di.repositoriesModule
import ru.llm.agent.di.servicesModule
import ru.llm.agent.di.domainKoinModule
import ru.llm.agent.di.databaseModule
import ru.llm.agent.di.exportersModule
import ru.llm.agent.di.platformDatabaseModule
import ru.llm.agent.di.settingsModule
import ru.llm.agent.di.platformSettingsModule
import ru.llm.agent.di.ragModule
import ru.llm.agent.mcp.di.mcpModule

internal expect fun defaultKoinLogger(level: Level): Logger

fun KoinApplication.initKoinApp(
    platformContext: PlatformContext,
    isDebug: Boolean,
    logLevel: Level = Level.INFO,
) {
    allowOverride(false)
    if (isDebug) {
        logger(defaultKoinLogger(logLevel))
    }

    modules(
        platformKoinModule(platformContext),
        platformSettingsModule,
        settingsModule,
        platformDatabaseModule,
        databaseModule,
        networkModule,
        servicesModule,
        repositoriesModule,
        ragModule,
        exportersModule,
        domainKoinModule,
        mcpModule,
    )
}

internal expect fun platformKoinModule(platformContext: PlatformContext): Module
