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
        primary = Color(0xFF6200EE),
        // Цвет текста и элементов поверх primary
        onPrimary = Color.White,
        // Контейнер с прозрачностью или оттенком primary
        primaryContainer = Color(0xFFEADDFF),
        // Цвет текста поверх primaryContainer
        onPrimaryContainer = Color(0xFF1D1976),
        // Обратный оттенок primary для темного фона
        inversePrimary = Color(0xFFE0BFFF),
        // Вторичный цвет (дополнительные акценты)
        secondary = Color(0xFF03DAC5),
        // Текст на фоне secondary
        onSecondary = Color.White,
        // Контейнер вторичного цвета
        secondaryContainer = Color(0xFFC8FAD6),
        // Текст на контейнере secondary
        onSecondaryContainer = Color(0xFF00362D),
        // Третичный цвет (для дополнительных интерактивов)
        tertiary = Color(0xFF7D52FF),
        // Текст на фоне tertiary
        onTertiary = Color.White,
        // Контейнер tertiary
        tertiaryContainer = Color(0xFFEDE0FF),
        // Текст на контейнере tertiary
        onTertiaryContainer = Color(0xFF240066),
        // Основной фон приложения
        background = Color(0xFFF8FAFD),
        // Основной текст на фоне
        onBackground = Color(0xFF1C1B1F),
        // Поверхностные элементы (карточки, поля ввода)
        surface = Color.White,
        // Текст на поверхностях
        onSurface = Color(0xFF1C1B1F),
        // Вариант поверхности (для разделителей и т.п.)
        surfaceVariant = Color(0xFFE3E3E6),
        // Текст на surfaceVariant
        onSurfaceVariant = Color(0xFF45454A),
        // Цвет tint для кнопок и элементов с surface tint
        surfaceTint = Color(0xFF6200EE),
        // Инвертированный фон для модальных окон
        inverseSurface = Color(0xFF1C1B1F),
        // Текст на инвертированном фоне
        inverseOnSurface = Color(0xFFF4F3F4),
        // Цвет ошибки
        error = Color(0xFFB3261E),
        // Текст ошибки на фоне error
        onError = Color(0xFFFCEDEC),
        // Контейнер ошибки
        errorContainer = Color(0xFFF9DEDC),
        // Текст на контейнере ошибки
        onErrorContainer = Color(0xFF410002),
        // Линии, границы, разделители
        outline = Color(0xFF757378),
        // Вариант outline (для более тусклых линий)
        outlineVariant = Color(0xFFCFCED3),
        // Цвет затемнения для overlay
        scrim = Color(0xFF000000),
        // Более яркий вариант surface
        surfaceBright = Color(0xFFF0F2F5),
        // Менее яркий вариант surface
        surfaceDim = Color(0xFFE3E6EB),
        // Стандартный контейнер
        surfaceContainer = Color(0xFFFBFCFF),
        // Высокий контейнер (например, для модалок)
        surfaceContainerHigh = Color(0xFFFCFEFF),
        // Наивысший контейнер (крайне высокий приоритет)
        surfaceContainerHighest = Color(0xFFFFFFFF),
        // Низкий контейнер (менее активные элементы)
        surfaceContainerLow = Color(0xFFEFF1F5),
        // Самый низкий контейнер (фон)
        surfaceContainerLowest = Color(0xFFF8FAFD),
        // Текст на surfaceDim
        onSurfaceDim = Color(0xFF6E6D71),
        // Outline для dimmed состояний
        outlineDim = Color(0xFFA9A8AB),
        // Outline для инвертированных состояний
        inverseOutline = Color(0xFF999999),
        // Outline вариант для инвертированного состояния
        inverseOnSurfaceVariant = Color(0xFF999999),
        // Текст на инвертированном surfaceDim
        inverseOnSurfaceDim = Color(0xFF8C8C8C),
        // Состояние без статуса (например, ошибка загрузки)
        assetNoStatus = Color(0xFFFF5F5F),
        // Состояние "на проверке"
        assetNeedsReview = Color(0xFF4DD0E1),
        // Состояние "в процессе"
        assetInProgress = Color(0xFF4CD964),
        // Состояние "утверждено"
        assetApproved = Color(0xFF4B5065)
    )
}


internal fun darkColorScheme(): ColorScheme {
    return ColorScheme(
        // Основной цвет приложения (для кнопок и акцентов)
        primary = Color(0xFFD5C6FF),
        // Цвет текста и элементов поверх primary
        onPrimary = Color(0xFF3800B4),
        // Контейнер с прозрачностью или оттенком primary
        primaryContainer = Color(0xFF4F00BD),
        // Цвет текста поверх primaryContainer
        onPrimaryContainer = Color(0xFFEADDFF),
        // Обратный оттенок primary для темного фона
        inversePrimary = Color(0xFF9D8CFF),
        // Вторичный цвет (дополнительные акценты)
        secondary = Color(0xFF80CBC4),
        // Текст на фоне secondary
        onSecondary = Color(0xFF004D48),
        // Контейнер вторичного цвета
        secondaryContainer = Color(0xFF006A62),
        // Текст на контейнере secondary
        onSecondaryContainer = Color(0xFFA0F4EC),
        // Третичный цвет (для дополнительных интерактивов)
        tertiary = Color(0xFFD8B7FF),
        // Текст на фоне tertiary
        onTertiary = Color(0xFF3D00D1),
        // Контейнер tertiary
        tertiaryContainer = Color(0xFF5326D3),
        // Текст на контейнере tertiary
        onTertiaryContainer = Color(0xFFEDE0FF),
        // Основной фон приложения
        background = Color(0xFF121212),
        // Основной текст на фоне
        onBackground = Color(0xFFE6E6E6),
        // Поверхностные элементы (карточки, поля ввода)
        surface = Color(0xFF1C1C1E),
        // Текст на поверхностях
        onSurface = Color(0xFFE6E6E6),
        // Вариант поверхности (для разделителей и т.п.)
        surfaceVariant = Color(0xFF45454A),
        // Текст на surfaceVariant
        onSurfaceVariant = Color(0xFFCCCCCC),
        // Цвет tint для кнопок и элементов с surface tint
        surfaceTint = Color(0xFF6200EE),
        // Инвертированный фон для модальных окон
        inverseSurface = Color(0xFFE6E6E6),
        // Текст на инвертированном фоне
        inverseOnSurface = Color(0xFF1C1C1E),
        // Цвет ошибки
        error = Color(0xFFFFB4AB),
        // Текст ошибки на фоне error
        onError = Color(0xFF690005),
        // Контейнер ошибки
        errorContainer = Color(0xFF93000A),
        // Текст на контейнере ошибки
        onErrorContainer = Color(0xFFFFDAD6),
        // Линии, границы, разделители
        outline = Color(0xFF8C8C8C),
        // Вариант outline (для более тусклых линий)
        outlineVariant = Color(0xFF45454A),
        // Цвет затемнения для overlay
        scrim = Color(0xFF000000),
        // Более яркий вариант surface
        surfaceBright = Color(0xFF2C2C2E),
        // Менее яркий вариант surface
        surfaceDim = Color(0xFF121212),
        // Стандартный контейнер
        surfaceContainer = Color(0xFF262628),
        // Высокий контейнер (например, для модалок)
        surfaceContainerHigh = Color(0xFF2C2C2E),
        // Наивысший контейнер (крайне высокий приоритет)
        surfaceContainerHighest = Color(0xFF363638),
        // Низкий контейнер (менее активные элементы)
        surfaceContainerLow = Color(0xFF1A1A1C),
        // Самый низкий контейнер (фон)
        surfaceContainerLowest = Color(0xFF121212),
        // Текст на surfaceDim
        onSurfaceDim = Color(0xFF999999),
        // Outline для dimmed состояний
        outlineDim = Color(0xFF666666),
        // Outline для инвертированных состояний
        inverseOutline = Color(0xFF666666),
        // Outline вариант для инвертированного состояния
        inverseOnSurfaceVariant = Color(0xFF999999),
        // Текст на инвертированном surfaceDim
        inverseOnSurfaceDim = Color(0xFF8C8C8C),
        // Состояние без статуса (например, ошибка загрузки)
        assetNoStatus = Color(0xFFFF5F5F),
        // Состояние "на проверке"
        assetNeedsReview = Color(0xFF4DD0E1),
        // Состояние "в процессе"
        assetInProgress = Color(0xFF4CD964),
        // Состояние "утверждено"
        assetApproved = Color(0xFF4B5065)
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
