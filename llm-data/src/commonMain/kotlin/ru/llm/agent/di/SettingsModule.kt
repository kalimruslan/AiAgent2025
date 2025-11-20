package ru.llm.agent.di

import com.russhwolf.settings.Settings
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.llm.agent.utils.settings.AppSettings
import ru.llm.agent.utils.settings.AppSettingsImpl

/**
 * Модуль для работы с настройками приложения
 */
public val settingsModule: Module = module {
    single<AppSettings> {
        AppSettingsImpl(settings = get())
    }
}

/**
 * Platform-specific модуль для создания Settings
 * Должен быть реализован в androidMain и desktopMain
 */
public expect val platformSettingsModule: Module