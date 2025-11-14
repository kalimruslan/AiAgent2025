package ru.llm.agent.presentation.state

/**
 * Sealed interface для побочных эффектов (side effects) в диалоге.
 * Представляет одноразовые действия, которые не изменяют состояние напрямую.
 * Используется для навигации, показа уведомлений и т.д.
 */
public sealed interface ConversationEffect {
    /**
     * Показать toast сообщение пользователю
     */
    public data class ShowToast(val message: String) : ConversationEffect

    /**
     * Показать snackbar с возможностью действия
     */
    public data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null
    ) : ConversationEffect

    /**
     * Навигация к экрану настроек
     */
    public data object NavigateToSettings : ConversationEffect

    /**
     * Прокрутить список сообщений вниз
     */
    public data object ScrollToBottom : ConversationEffect

    /**
     * Показать диалог подтверждения очистки
     */
    public data object ShowClearConfirmation : ConversationEffect

    /**
     * Скопировать текст в буфер обмена
     */
    public data class CopyToClipboard(val text: String) : ConversationEffect

    /**
     * Показать индикатор суммаризации
     */
    public data class ShowSummarizationProgress(val message: String) : ConversationEffect
}