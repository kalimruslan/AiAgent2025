package ru.llm.agent.core.uikit.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Объект с константами размеров для единообразного дизайна приложения.
 * Использует принцип 8dp grid system.
 */
public object Dimensions {
    /**
     * Отступы (Padding)
     */
    public object Padding {
        /** Минимальный отступ - 4dp */
        public val extraSmall: Dp = 4.dp

        /** Маленький отступ - 8dp */
        public val small: Dp = 8.dp

        /** Средний отступ - 16dp */
        public val medium: Dp = 16.dp

        /** Большой отступ - 24dp */
        public val large: Dp = 24.dp

        /** Очень большой отступ - 32dp */
        public val extraLarge: Dp = 32.dp
    }

    /**
     * Скругления углов (Corner Radius)
     */
    public object CornerRadius {
        /** Нет скругления */
        public val none: Dp = 0.dp

        /** Маленькое скругление - 4dp */
        public val small: Dp = 4.dp

        /** Среднее скругление - 8dp */
        public val medium: Dp = 8.dp

        /** Большое скругление - 12dp */
        public val large: Dp = 12.dp

        /** Очень большое скругление - 16dp */
        public val extraLarge: Dp = 16.dp

        /** Полностью круглое */
        public val full: Dp = 50.dp
    }

    /**
     * Размеры элементов (Element Sizes)
     */
    public object ElementSize {
        /** Высота маленькой кнопки - 32dp */
        public val buttonSmall: Dp = 32.dp

        /** Высота средней кнопки - 40dp */
        public val buttonMedium: Dp = 40.dp

        /** Высота большой кнопки - 48dp */
        public val buttonLarge: Dp = 48.dp

        /** Размер маленькой иконки - 16dp */
        public val iconSmall: Dp = 16.dp

        /** Размер средней иконки - 24dp */
        public val iconMedium: Dp = 24.dp

        /** Размер большой иконки - 32dp */
        public val iconLarge: Dp = 32.dp

        /** Минимальная высота текстового поля - 56dp */
        public val textFieldMinHeight: Dp = 56.dp
    }

    /**
     * Границы (Borders)
     */
    public object Border {
        /** Тонкая граница - 1dp */
        public val thin: Dp = 1.dp

        /** Средняя граница - 2dp */
        public val medium: Dp = 2.dp

        /** Толстая граница - 4dp */
        public val thick: Dp = 4.dp
    }

    /**
     * Промежутки между элементами (Spacing)
     */
    public object Spacing {
        /** Минимальный промежуток - 4dp */
        public val extraSmall: Dp = 4.dp

        /** Маленький промежуток - 8dp */
        public val small: Dp = 8.dp

        /** Средний промежуток - 16dp */
        public val medium: Dp = 16.dp

        /** Большой промежуток - 24dp */
        public val large: Dp = 24.dp

        /** Очень большой промежуток - 32dp */
        public val extraLarge: Dp = 32.dp
    }

    /**
     * Elevation (высота тени)
     */
    public object Elevation {
        /** Нет тени */
        public val none: Dp = 0.dp

        /** Маленькая тень - 2dp */
        public val small: Dp = 2.dp

        /** Средняя тень - 4dp */
        public val medium: Dp = 4.dp

        /** Большая тень - 8dp */
        public val large: Dp = 8.dp

        /** Очень большая тень - 16dp */
        public val extraLarge: Dp = 16.dp
    }

    /**
     * Размеры карточек сообщений
     */
    public object MessageCard {
        /** Минимальная высота карточки сообщения */
        public val minHeight: Dp = 60.dp

        /** Скругление углов карточки */
        public val cornerRadius: Dp = CornerRadius.medium

        /** Внутренний отступ карточки */
        public val padding: Dp = Padding.medium

        /** Промежуток между карточками */
        public val spacing: Dp = Spacing.small
    }

    /**
     * Размеры индикаторов прогресса
     */
    public object Progress {
        /** Размер маленького индикатора - 24dp */
        public val indicatorSmall: Dp = 24.dp

        /** Размер среднего индикатора - 40dp */
        public val indicatorMedium: Dp = 40.dp

        /** Размер большого индикатора - 56dp */
        public val indicatorLarge: Dp = 56.dp

        /** Высота линейного прогресс-бара - 4dp */
        public val linearHeight: Dp = 4.dp
    }
}