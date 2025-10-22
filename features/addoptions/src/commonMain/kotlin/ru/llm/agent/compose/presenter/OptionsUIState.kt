package ru.llm.agent.compose.presenter

internal class OptionsUIState {
    data class State(
        val conversationId: String,
        val systemPrompt: String? = null,
        val temperature: Double,
        val maxTokens: Int,
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
    }
}

