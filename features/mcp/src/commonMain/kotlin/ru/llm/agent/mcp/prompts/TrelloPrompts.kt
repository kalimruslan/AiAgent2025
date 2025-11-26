package ru.llm.agent.mcp.prompts

/**
 * Промпты для Trello инструментов
 * Генерирует осмысленные запросы к LLM для выполнения Trello операций
 */
public enum class TrelloPrompts(
    public val toolName: String,
    private val promptWithBoard: String,
    private val promptWithoutBoard: String
) {
    GET_SUMMARY(
        toolName = "trello_getSummary",
        promptWithBoard = "Покажи статистику по Trello доске %s",
        promptWithoutBoard = "Покажи статистику по моей Trello доске"
    ),

    GET_CARDS(
        toolName = "trello_getCards",
        promptWithBoard = "Покажи все задачи из Trello доски %s",
        promptWithoutBoard = "Покажи все мои задачи из Trello"
    ),

    SEARCH_CARDS(
        toolName = "trello_searchCards",
        promptWithBoard = "Найди задачи в Trello на доске %s",
        promptWithoutBoard = "Найди задачи в моей Trello доске"
    ),

    QUICK_TASK(
        toolName = "trello_quickTask",
        promptWithBoard = "Создай быструю задачу в Trello на доске %s",
        promptWithoutBoard = "Создай быструю задачу в моей Trello доске"
    ),

    CREATE_CARD(
        toolName = "trello_createCard",
        promptWithBoard = "Создай новую карточку в Trello на доске %s",
        promptWithoutBoard = "Создай новую карточку в моей Trello доске"
    );

    /**
     * Генерирует промпт с учётом наличия boardId
     */
    public fun buildPrompt(boardId: String?): String {
        return if (boardId != null) {
            promptWithBoard.format(boardId)
        } else {
            promptWithoutBoard
        }
    }

    public companion object {
        /**
         * Найти промпт по имени инструмента
         */
        public fun findByToolName(toolName: String): TrelloPrompts? {
            return entries.find { it.toolName == toolName }
        }

        /**
         * Создать промпт для Trello инструмента
         * @param toolName имя инструмента
         * @param boardId ID доски (опционально)
         * @param fallbackDescription описание для неизвестных инструментов
         * @return сформированный промпт
         */
        public fun createPrompt(
            toolName: String,
            boardId: String?,
            fallbackDescription: String = ""
        ): String {
            val prompt = findByToolName(toolName)

            return if (prompt != null) {
                prompt.buildPrompt(boardId)
            } else {
                // Для неизвестных Trello инструментов
                if (boardId != null) {
                    "Используй Trello доску $boardId: $fallbackDescription"
                } else {
                    fallbackDescription
                }
            }
        }

        /**
         * Проверить, является ли инструмент Trello инструментом
         */
        public fun isTrelloTool(toolName: String): Boolean {
            return toolName.startsWith("trello_")
        }
    }
}