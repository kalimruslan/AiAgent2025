package ru.llm.agent.utils.settings

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set

/**
 * Интерфейс для работы с настройками приложения.
 *
 * В KMP реализуется через:
 * - Android: SharedPreferences
 * - Desktop/JVM: java.util.prefs.Preferences
 * - iOS: NSUserDefaults
 */
public interface AppSettings {
    /**
     * ID доски Trello для умных промптов
     */
    public var trelloBoardId: String?

    /**
     * Очистить все настройки
     */
    public fun clear()
}

/**
 * Реализация AppSettings на основе multiplatform-settings
 */
public class AppSettingsImpl(
    private val settings: Settings
) : AppSettings {

    override var trelloBoardId: String?
        get() = settings.getStringOrNull(KEY_TRELLO_BOARD_ID)
        set(value) {
            if (value != null) {
                settings[KEY_TRELLO_BOARD_ID] = value
            } else {
                settings.remove(KEY_TRELLO_BOARD_ID)
            }
        }

    override fun clear() {
        settings.clear()
    }

    private companion object {
        private const val KEY_TRELLO_BOARD_ID = "trello_board_id"
    }
}