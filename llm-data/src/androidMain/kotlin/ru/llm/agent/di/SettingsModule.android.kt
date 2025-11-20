package ru.llm.agent.di

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Android-реализация Settings через SharedPreferences
 */
public actual val platformSettingsModule: Module = module {
    single<Settings> {
        val context: Context = get()
        val sharedPreferences = context.getSharedPreferences(
            "app_settings",
            Context.MODE_PRIVATE
        )
        SharedPreferencesSettings(sharedPreferences)
    }
}