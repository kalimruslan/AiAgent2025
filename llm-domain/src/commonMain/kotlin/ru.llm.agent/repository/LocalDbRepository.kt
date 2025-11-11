package ru.llm.agent.repository

import ru.llm.agent.model.ConversationContext

/**
 * Репозиторий для работы с локальным контекстом диалогов.
 * Хранит настройки и метаданные диалогов (температура, max tokens и т.д.)
 */
public interface LocalDbRepository{
    /**
     * Сохранить контекст для указанного диалога
     *
     * @param conversationId ID диалога
     * @param conversationContext Контекст диалога для сохранения
     */
    public suspend fun saveContextByConversation(conversationId: String, conversationContext: ConversationContext)

    /**
     * Получить контекст для указанного диалога
     *
     * @param conversationId ID диалога
     * @return Контекст диалога или null, если не найден
     */
    public suspend fun getContextByConversation(conversationId: String): ConversationContext?

    /**
     * Удалить контекст для указанного диалога
     *
     * @param conversationId ID диалога
     */
    public suspend fun removeContextByConversation(conversationId: String)

    /**
     * Удалить все сохраненные контексты
     */
    public suspend fun deleteAllContexts()
}