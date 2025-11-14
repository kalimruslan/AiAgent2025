package ru.llm.agent.model.conversation

import ru.llm.agent.core.utils.model.TokenCount

/**
 * Sealed interface для представления состояния диалога.
 * Обеспечивает типобезопасное управление состоянием диалога.
 */
public sealed interface DialogState {
    /**
     * Диалог в процессе (активен)
     */
    public data class Active(
        /** Текущее количество использованных токенов */
        val currentTokens: TokenCount,
        /** Максимальное количество токенов для контекста */
        val maxTokens: TokenCount,
        /** Флаг необходимости суммаризации */
        val needsSummarization: Boolean = false
    ) : DialogState {
        /** Процент заполнения контекста */
        val contextUsagePercent: Float
            get() = (currentTokens.value.toFloat() / maxTokens.value) * 100f

        /** Достигнут ли лимит токенов */
        val isAtLimit: Boolean
            get() = currentTokens >= maxTokens
    }

    /**
     * Диалог ожидает ответа от LLM
     */
    public data class AwaitingResponse(
        val currentTokens: TokenCount
    ) : DialogState

    /**
     * Диалог завершён успешно
     */
    public data class Completed(
        /** Финальный результат диалога */
        val finalResult: String,
        /** Общее количество использованных токенов */
        val totalTokens: TokenCount,
        /** Время завершения */
        val completedAt: Long = 0L
    ) : DialogState

    /**
     * Диалог завершён с ошибкой
     */
    public data class Failed(
        /** Сообщение об ошибке */
        val errorMessage: String,
        /** Исключение, вызвавшее ошибку (если есть) */
        val cause: Throwable? = null,
        /** Время возникновения ошибки */
        val failedAt: Long = 0L
    ) : DialogState

    /**
     * Диалог приостановлен (например, для review)
     */
    public data class Paused(
        /** Причина приостановки */
        val reason: String,
        /** Текущее состояние до приостановки */
        val previousState: Active
    ) : DialogState
}

/**
 * Extension функции для проверки состояния
 */
public val DialogState.isActive: Boolean
    get() = this is DialogState.Active || this is DialogState.AwaitingResponse

public val DialogState.isTerminal: Boolean
    get() = this is DialogState.Completed || this is DialogState.Failed

public val DialogState.canContinue: Boolean
    get() = when (this) {
        is DialogState.Active -> !isAtLimit
        is DialogState.Paused -> true
        else -> false
    }