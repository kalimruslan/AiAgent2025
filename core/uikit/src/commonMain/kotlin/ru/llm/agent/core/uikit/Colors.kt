@file:Suppress("MagicNumber")

package ru.llm.agent.core.uikit

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ColorScheme as M3ColorScheme

internal val LocalColorScheme = staticCompositionLocalOf { lightColorScheme() }

internal fun lightColorScheme(): ColorScheme {
    return ColorScheme(
        // Основной цвет приложения (например, для кнопок и акцентов)
        primary = Color(0xFF6200EE), // 🔵 Ярко-фиолетовый (акцентный)
        // Цвет текста и элементов поверх primary
        onPrimary = Color.White, // ⚪ Белый
        // Контейнер с прозрачностью или оттенком primary
        primaryContainer = Color(0xFFEADDFF), // 🟣 Светло-лавандовый
        // Цвет текста поверх primaryContainer
        onPrimaryContainer = Color(0xFF1D1976), // 🟦 Тёмно-синий
        // Обратный оттенок primary для тёмного фона
        inversePrimary = Color(0xFFE0BFFF), // 🌸 Светло-фиолетовый
        // Вторичный цвет (дополнительные акценты)
        secondary = Color(0xFF03DAC5), // 💠 Бирюзовый (акцент 2)
        // Текст на фоне secondary
        onSecondary = Color.White, // ⚪ Белый
        // Контейнер вторичного цвета
        secondaryContainer = Color(0xFFC8FAD6), // 🟩 Светло-зелёный
        // Текст на контейнере secondary
        onSecondaryContainer = Color(0xFF00362D), // 🟫 Тёмно-зелёный
        // Третичный цвет (для дополнительных интерактивов)
        tertiary = Color(0xFF7D52FF), // 🟪 Сиреневый
        // Текст на фоне tertiary
        onTertiary = Color.White, // ⚪ Белый
        // Контейнер tertiary
        tertiaryContainer = Color(0xFFEDE0FF), // 🟪 Светло-сиреневый
        // Текст на контейнере tertiary
        onTertiaryContainer = Color(0xFF240066), // 🟪 Тёмно-сиреневый
        // Основной фон приложения
        background = Color(0xFFF8FAFD), // ⚪ Почти белый (фон)
        // Основной текст на фоне
        onBackground = Color(0xFF1C1B1F), // ⚫ Тёмно-серый (почти чёрный)
        // Поверхностные элементы (карточки, поля ввода)
        surface = Color.White, // ⚪ Белый
        // Текст на поверхностях
        onSurface = Color(0xFF1C1B1F), // ⚫ Тёмно-серый
        // Вариант поверхности (для разделителей и т.п.)
        surfaceVariant = Color(0xFFE3E3E6), // 🟤 Светло-серый
        // Текст на surfaceVariant
        onSurfaceVariant = Color(0xFF45454A), // 🟤 Тёмно-серый
        // Цвет tint для кнопок и элементов с surface tint
        surfaceTint = Color(0xFF6200EE), // 🔵 Ярко-фиолетовый (акцент)
        // Инвертированный фон для модальных окон
        inverseSurface = Color(0xFF1C1B1F), // ⚫ Тёмно-серый
        // Текст на инвертированном фоне
        inverseOnSurface = Color(0xFFF4F3F4), // ⚪ Светло-серый
        // Цвет ошибки
        error = Color(0xFFB3261E), // 🔴 Красный (ошибка)
        // Текст ошибки на фоне error
        onError = Color(0xFFFCEDEC), // 🟥 Светло-розовый
        // Контейнер ошибки
        errorContainer = Color(0xFFF9DEDC), // 🟥 Очень светлый красный
        // Текст на контейнере ошибки
        onErrorContainer = Color(0xFF410002), // 🟥 Тёмно-красный
        // Линии, границы, разделители
        outline = Color(0xFF757378), // 🟤 Серый
        // Вариант outline (для более тусклых линий)
        outlineVariant = Color(0xFFCFCED3), // 🟤 Светло-серый
        // Цвет затемнения для overlay
        scrim = Color(0xFF000000), // ⚫ Чёрный
        // Более яркий вариант surface
        surfaceBright = Color(0xFFF0F2F5), // ⚪ Светло-серый (фон)
        // Менее яркий вариант surface
        surfaceDim = Color(0xFFE3E6EB), // ⚪ Серый
        // Стандартный контейнер
        surfaceContainer = Color(0xFFFBFCFF), // ⚪ Почти белый
        // Высокий контейнер (например, для модалок)
        surfaceContainerHigh = Color(0xFFFCFEFF), // ⚪ Очень светлый
        // Наивысший контейнер (крайне высокий приоритет)
        surfaceContainerHighest = Color(0xFFFFFFFF), // ⚪ Белый
        // Низкий контейнер (менее активные элементы)
        surfaceContainerLow = Color(0xFFEFF1F5), // ⚪ Светлый
        // Самый низкий контейнер (фон)
        surfaceContainerLowest = Color(0xFFF8FAFD), // ⚪ Почти белый
        // Текст на surfaceDim
        onSurfaceDim = Color(0xFF6E6D71), // 🟤 Тёмно-серый
        // Outline для dimmed состояний
        outlineDim = Color(0xFFA9A8AB), // 🟤 Средний серый
        // Outline для инвертированных состояний
        inverseOutline = Color(0xFF999999), // 🟤 Серый
        // Outline вариант для инвертированного состояния
        inverseOnSurfaceVariant = Color(0xFF999999), // 🟤 Серый
        // Текст на инвертированном surfaceDim
        inverseOnSurfaceDim = Color(0xFF8C8C8C), // 🟤 Средний серый
        // Состояние без статуса (например, ошибка загрузки)
        assetNoStatus = Color(0xFFFF5F5F), // 🔴 Ярко-красный
        // Состояние "на проверке"
        assetNeedsReview = Color(0xFF4DD0E1), // 🔵 Светло-голубой
        // Состояние "в процессе"
        assetInProgress = Color(0xFF4CD964), // 🟢 Ярко-зелёный
        // Состояние "утверждено"
        assetApproved = Color(0xFF4B5065) // 🔵 Тёмно-серый с синим оттенком
    )
}

internal fun darkColorScheme(): ColorScheme {
    return ColorScheme(
        // Основной цвет приложения (для кнопок и акцентов)
        primary = Color(0xFFD5C6FF), // 🟣 Светло-фиолетовый (акцент)
        // Цвет текста и элементов поверх primary
        onPrimary = Color(0xFF3800B4), // 🟦 Тёмно-фиолетовый
        // Контейнер с прозрачностью или оттенком primary
        primaryContainer = Color(0xFF4F00BD), // 🟪 Глубокий фиолетовый
        // Цвет текста поверх primaryContainer
        onPrimaryContainer = Color(0xFFEADDFF), // 🟣 Светло-лавандовый
        // Обратный оттенок primary для тёмного фона
        inversePrimary = Color(0xFF9D8CFF), // 🟣 Средний фиолетовый
        // Вторичный цвет (дополнительные акценты)
        secondary = Color(0xFF80CBC4), // 💠 Светло-бирюзовый
        // Текст на фоне secondary
        onSecondary = Color(0xFF004D48), // 🟩 Тёмно-бирюзовый
        // Контейнер вторичного цвета
        secondaryContainer = Color(0xFF006A62), // 🟩 Глубокий бирюзовый
        // Текст на контейнере secondary
        onSecondaryContainer = Color(0xFFA0F4EC), // 🟩 Светло-бирюзовый
        // Третичный цвет (для дополнительных интерактивов)
        tertiary = Color(0xFFD8B7FF), // 🟪 Светло-сиреневый
        // Текст на фоне tertiary
        onTertiary = Color(0xFF3D00D1), // 🟪 Глубокий сиреневый
        // Контейнер tertiary
        tertiaryContainer = Color(0xFF5326D3), // 🟪 Тёмно-сиреневый
        // Текст на контейнере tertiary
        onTertiaryContainer = Color(0xFFEDE0FF), // 🟪 Светло-сиреневый
        // Основной фон приложения
        background = Color(0xFF121212), // ⚫ Чёрный
        // Основной текст на фоне
        onBackground = Color(0xFFE6E6E6), // ⚪ Светло-серый
        // Поверхностные элементы (карточки, поля ввода)
        surface = Color(0xFF1C1C1E), // ⚫ Тёмно-серый
        // Текст на поверхностях
        onSurface = Color(0xFFE6E6E6), // ⚪ Светло-серый
        // Вариант поверхности (для разделителей и т.п.)
        surfaceVariant = Color(0xFF45454A), // 🟤 Тёмно-серый
        // Текст на surfaceVariant
        onSurfaceVariant = Color(0xFFCCCCCC), // ⚪ Средне-серый
        // Цвет tint для кнопок и элементов с surface tint
        surfaceTint = Color(0xFF6200EE), // 🔵 Ярко-фиолетовый (акцент)
        // Инвертированный фон для модальных окон
        inverseSurface = Color(0xFFE6E6E6), // ⚪ Светло-серый
        // Текст на инвертированном фоне
        inverseOnSurface = Color(0xFF1C1C1E), // ⚫ Тёмно-серый
        // Цвет ошибки
        error = Color(0xFFFFB4AB), // 🟥 Светло-красный
        // Текст ошибки на фоне error
        onError = Color(0xFF690005), // 🔴 Тёмно-красный
        // Контейнер ошибки
        errorContainer = Color(0xFF93000A), // 🔴 Глубокий красный
        // Текст на контейнере ошибки
        onErrorContainer = Color(0xFFFFDAD6), // 🟥 Очень светлый красный
        // Линии, границы, разделители
        outline = Color(0xFF8C8C8C), // 🟤 Серый
        // Вариант outline (для более тусклых линий)
        outlineVariant = Color(0xFF45454A), // 🟤 Тёмно-серый
        // Цвет затемнения для overlay
        scrim = Color(0xFF000000), // ⚫ Чёрный
        // Более яркий вариант surface
        surfaceBright = Color(0xFF2C2C2E), // ⚫ Тёмно-серый
        // Менее яркий вариант surface
        surfaceDim = Color(0xFF121212), // ⚫ Чёрный
        // Стандартный контейнер
        surfaceContainer = Color(0xFF262628), // ⚫ Тёмно-серый
        // Высокий контейнер (например, для модалок)
        surfaceContainerHigh = Color(0xFF2C2C2E), // ⚫ Тёмно-серый
        // Наивысший контейнер (крайне высокий приоритет)
        surfaceContainerHighest = Color(0xFF363638), // ⚫ Тёмно-серый
        // Низкий контейнер (менее активные элементы)
        surfaceContainerLow = Color(0xFF1A1A1C), // ⚫ Очень тёмный
        // Самый низкий контейнер (фон)
        surfaceContainerLowest = Color(0xFF121212), // ⚫ Чёрный
        // Текст на surfaceDim
        onSurfaceDim = Color(0xFF999999), // 🟤 Средний серый
        // Outline для dimmed состояний
        outlineDim = Color(0xFF666666), // 🟤 Серый
        // Outline для инвертированных состояний
        inverseOutline = Color(0xFF666666), // 🟤 Серый
        // Outline вариант для инвертированного состояния
        inverseOnSurfaceVariant = Color(0xFF999999), // 🟤 Серый
        // Текст на инвертированном surfaceDim
        inverseOnSurfaceDim = Color(0xFF8C8C8C), // 🟤 Средний серый
        // Состояние без статуса (например, ошибка загрузки)
        assetNoStatus = Color(0xFFFF5F5F), // 🔴 Ярко-красный
        // Состояние "на проверке"
        assetNeedsReview = Color(0xFF4DD0E1), // 🔵 Светло-голубой
        // Состояние "в процессе"
        assetInProgress = Color(0xFF4CD964), // 🟢 Ярко-зелёный
        // Состояние "утверждено"
        assetApproved = Color(0xFF4B5065) // 🔵 Тёмно-серый с синим оттенком
    )
}

@Immutable
data class ColorScheme(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val inversePrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceTint: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val outline: Color,
    val outlineVariant: Color,
    val scrim: Color,
    val surfaceBright: Color,
    val surfaceDim: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainerLowest: Color,
    val onSurfaceDim: Color,
    val outlineDim: Color,
    val inverseOutline: Color,
    val inverseOnSurfaceVariant: Color,
    val inverseOnSurfaceDim: Color,
    val assetNoStatus: Color,
    val assetNeedsReview: Color,
    val assetInProgress: Color,
    val assetApproved: Color,
)

internal fun ColorScheme.toM3ColorScheme(): M3ColorScheme {
    return M3ColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim,
        surfaceBright = surfaceBright,
        surfaceDim = surfaceDim,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainerLowest = surfaceContainerLowest,
    )
}
