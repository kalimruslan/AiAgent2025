package ru.llm.agent.presentation.state

import ru.llm.agent.model.ConversationMode
import ru.llm.agent.model.Expert
import ru.llm.agent.model.LlmProvider

/**
 * Sealed interface для событий пользователя в диалоге.
 * Представляет все возможные действия пользователя.
 */
public sealed interface ConversationEvent {
    /**
     * Отправить сообщение в диалог
     */
    public data class SendMessage(val text: String) : ConversationEvent

    /**
     * Очистить текущий диалог
     */
    public data object ClearConversation : ConversationEvent

    /**
     * Открыть настройки
     */
    public data object OpenSettings : ConversationEvent

    /**
     * Выбрать провайдера LLM
     */
    public data class SelectProvider(val provider: LlmProvider) : ConversationEvent

    /**
     * Выбрать режим работы
     */
    public data class SelectMode(val mode: ConversationMode) : ConversationEvent

    /**
     * Переключить эксперта (добавить/убрать из выбранных)
     */
    public data class ToggleExpert(val expert: Expert) : ConversationEvent

    /**
     * Запустить суммаризацию истории
     */
    public data object TriggerSummarization : ConversationEvent

    /**
     * Очистить ошибку
     */
    public data object ClearError : ConversationEvent

    /**
     * Перезагрузить данные
     */
    public data object Reload : ConversationEvent
}