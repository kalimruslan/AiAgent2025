package ru.llm.agent.presentation.state

import ru.llm.agent.model.mcp.McpServer
import ru.llm.agent.model.mcp.McpToolInfo

internal class OptionsUIState {
    data class State(
        val conversationId: String,
        val systemPrompt: String? = null,
        val temperature: Double,
        val maxTokens: Int,
        val mcpTools: List<McpToolInfo> = emptyList(),
        val isToolsLoading: Boolean = false,
        val toolsError: String? = null,
        val isToolsSectionExpanded: Boolean = false,
        val mcpServers: List<McpServer> = emptyList(),
        val isServersSectionExpanded: Boolean = false,
        val showAddServerDialog: Boolean = false
    ) {
        companion object Companion {
            fun default(conversationId: String) = State(
                conversationId = conversationId,
                systemPrompt = """
                    Ты — консультант по Андроид разработке.

                    ПРАВИЛА ДИАЛОГА:
                    1. Задавай уточняющие вопросы, чтобы понять идею пользователя
                    2. Когда соберешь достаточно информации (не более 3 вопроса), дай финальный совет

                    Отвечай строго в JSON формате по следующей схеме:
                    {
                      "answer": "текст ответа",
                      "is_continue": "флаг, если нужно продолжить диалог, например true или false",
                      "is_complete": "флаг, если готов дать финальный ответ, например true или false",
                    }
                    Не добавляй никакого текста до или после JSON.
                """.trimIndent(),
                temperature = 0.1,
                maxTokens = 2000,
            )
        }
    }

    sealed interface Event {
        data class ApplyClick(
            val navigateAction: () -> Unit,
            val systemPrompt: String?,
            val temperature: String,
            val maxTokens: String
        ) : Event

        data object ResetOptions : Event

        data object LoadMcpTools : Event

        data object ToggleToolsSection : Event

        data object ToggleServersSection : Event

        data object ShowAddServerDialog : Event

        data object HideAddServerDialog : Event

        data class AddServer(
            val name: String,
            val url: String,
            val description: String
        ) : Event

        data class DeleteServer(val serverId: Long) : Event

        data class ToggleServerActive(val serverId: Long, val isActive: Boolean) : Event
    }
}

