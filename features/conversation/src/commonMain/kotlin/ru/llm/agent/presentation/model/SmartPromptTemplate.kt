package ru.llm.agent.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Шаблон умного промпта с поддержкой переменных
 *
 * Поддерживаемые переменные:
 * - {boardId} - ID доски Trello
 * - {date} - текущая дата
 * - {tomorrow} - завтрашняя дата
 * - {weekStart} - начало недели
 * - {weekEnd} - конец недели
 */
data class SmartPromptTemplate(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val template: String,
    val description: String? = null,
    val requiresBoardId: Boolean = false
) {
    companion object {
        /**
         * Предустановленные шаблоны для Trello
         */
        val TRELLO_TEMPLATES = listOf(
            SmartPromptTemplate(
                id = "today_tasks",
                label = "Что на сегодня?",
                icon = Icons.Default.DateRange,
                template = "Покажи все задачи на {date} из Trello доски {boardId}",
                description = "Показать задачи с дедлайном на сегодня",
                requiresBoardId = true
            ),
            SmartPromptTemplate(
                id = "tomorrow_tasks",
                label = "Что на завтра?",
                icon = Icons.Default.DateRange,
                template = "Покажи все задачи на {tomorrow} из Trello доски {boardId}",
                description = "Показать задачи с дедлайном на завтра",
                requiresBoardId = true
            ),
            SmartPromptTemplate(
                id = "overdue",
                label = "Что просрочено?",
                icon = Icons.Default.Warning,
                template = "Покажи все просроченные задачи из Trello доски {boardId}",
                description = "Показать просроченные задачи",
                requiresBoardId = true
            ),
            SmartPromptTemplate(
                id = "week_tasks",
                label = "Задачи на неделю",
                icon = Icons.Default.DateRange,
                template = "Покажи все задачи на эту неделю ({weekStart} - {weekEnd}) из Trello доски {boardId}",
                description = "Показать задачи на текущую неделю",
                requiresBoardId = true
            ),
            SmartPromptTemplate(
                id = "create_task_today",
                label = "Задача на сегодня",
                icon = Icons.Default.Add,
                template = "Создай быструю задачу в Trello на доске {boardId} с дедлайном {date}",
                description = "Создать задачу с дедлайном на сегодня",
                requiresBoardId = true
            ),
            SmartPromptTemplate(
                id = "create_task_tomorrow",
                label = "Задача на завтра",
                icon = Icons.Default.Add,
                template = "Создай быструю задачу в Trello на доске {boardId} с дедлайном {tomorrow}",
                description = "Создать задачу с дедлайном на завтра",
                requiresBoardId = true
            ),
            SmartPromptTemplate(
                id = "board_summary",
                label = "Статистика",
                icon = Icons.Default.Info,
                template = "Покажи статистику по Trello доске {boardId}",
                description = "Показать общую статистику по доске",
                requiresBoardId = true
            ),
            SmartPromptTemplate(
                id = "search_tasks",
                label = "Поиск задач",
                icon = Icons.Default.Search,
                template = "Найди задачи в Trello на доске {boardId} по ключевому слову",
                description = "Поиск задач по ключевым словам",
                requiresBoardId = true
            )
        )
    }
}

/**
 * Контекст для подстановки переменных в шаблоны
 */
data class PromptVariablesContext(
    val boardId: String? = null,
    val date: String = getCurrentDate(),
    val tomorrow: String = getTomorrowDate(),
    val weekStart: String = getWeekStartDate(),
    val weekEnd: String = getWeekEndDate()
) {
    companion object {
        /**
         * Получить текущую дату в формате "сегодня" или ISO
         */
        private fun getCurrentDate(): String = "сегодня"

        /**
         * Получить завтрашнюю дату
         */
        private fun getTomorrowDate(): String = "завтра"

        /**
         * Получить дату начала недели
         */
        private fun getWeekStartDate(): String {
            // TODO: Реализовать расчёт начала недели
            return "начало недели"
        }

        /**
         * Получить дату конца недели
         */
        private fun getWeekEndDate(): String {
            // TODO: Реализовать расчёт конца недели
            return "конец недели"
        }
    }
}

/**
 * Подставляет переменные в шаблон промпта
 */
fun SmartPromptTemplate.fillTemplate(context: PromptVariablesContext): String {
    var result = template

    // Проверяем, что boardId указан, если он требуется
    if (requiresBoardId && context.boardId.isNullOrBlank()) {
        return template // Возвращаем шаблон без подстановки, если boardId отсутствует
    }

    // Подставляем переменные
    result = result.replace("{boardId}", context.boardId ?: "")
    result = result.replace("{date}", context.date)
    result = result.replace("{tomorrow}", context.tomorrow)
    result = result.replace("{weekStart}", context.weekStart)
    result = result.replace("{weekEnd}", context.weekEnd)

    return result
}