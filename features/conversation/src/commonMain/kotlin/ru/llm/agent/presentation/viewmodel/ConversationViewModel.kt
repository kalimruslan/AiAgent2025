package ru.llm.agent.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.llm.agent.doActionIfError
import ru.llm.agent.doActionIfLoading
import ru.llm.agent.doActionIfSuccess
import ru.llm.agent.error.DomainError
import ru.llm.agent.model.ConversationMode
import ru.llm.agent.model.Expert
import ru.llm.agent.model.LlmProvider
import ru.llm.agent.model.Role
import ru.llm.agent.presentation.state.ConversationUIState
import ru.llm.agent.usecase.CommitteeResult
import ru.llm.agent.usecase.ConversationUseCase
import ru.llm.agent.usecase.ExecuteCommitteeUseCase
import ru.llm.agent.usecase.GetMessagesWithExpertOpinionsUseCase
import ru.llm.agent.usecase.GetMessageTokenCountUseCase
import ru.llm.agent.usecase.GetSelectedProviderUseCase
import ru.llm.agent.usecase.GetSummarizationInfoUseCase
import ru.llm.agent.usecase.GetTokenUsageUseCase
import ru.llm.agent.usecase.SaveSelectedProviderUseCase
import ru.llm.agent.usecase.SendConversationMessageUseCase
import ru.llm.agent.usecase.SummarizeHistoryUseCase
import java.util.logging.Logger

class ConversationViewModel(
    private val conversationUseCase: ConversationUseCase,
    private val sendConversationMessageUseCase: SendConversationMessageUseCase,
    private val getSelectedProviderUseCase: GetSelectedProviderUseCase,
    private val saveSelectedProviderUseCase: SaveSelectedProviderUseCase,
    private val getMessagesWithExpertOpinionsUseCase: GetMessagesWithExpertOpinionsUseCase,
    private val executeCommitteeUseCase: ExecuteCommitteeUseCase,
    private val getTokenUsageUseCase: GetTokenUsageUseCase,
    private val getMessageTokenCountUseCase: GetMessageTokenCountUseCase,
    private val summarizeHistoryUseCase: SummarizeHistoryUseCase,
    private val getSummarizationInfoUseCase: GetSummarizationInfoUseCase
) : ViewModel() {

    private val _screeState = MutableStateFlow(ConversationUIState.State.empty())
    internal val screeState = _screeState.asStateFlow()

    private val _events = MutableSharedFlow<ConversationUIState.Event>()
    val conversationId = "default_conversation"

    init {
        viewModelScope.launch {
            _events.collect {
                handleEvent(it)
            }
        }
    }

    fun start(){
        viewModelScope.launch {
            // Загружаем сохраненный провайдер
            val savedProvider = getSelectedProviderUseCase(conversationId)
            _screeState.update { it.copy(selectedProvider = savedProvider) }

            // Загружаем сообщения
            loadMessages()

            // Загружаем использование токенов
            loadTokenUsage()

            // Загружаем информацию о суммаризации
            loadSummarizationInfo()
        }
    }

    /**
     * Загрузить информацию о суммаризации истории
     */
    private fun loadSummarizationInfo() {
        viewModelScope.launch {
            getSummarizationInfoUseCase(conversationId).collect { summarizationInfo ->
                _screeState.update {
                    it.copy(summarizationInfo = summarizationInfo)
                }
            }
        }
    }

    /**
     * Загрузить информацию об использовании токенов
     */
    private fun loadTokenUsage() {
        viewModelScope.launch {
            getTokenUsageUseCase(conversationId).collect { tokenUsage ->
                _screeState.update {
                    it.copy(
                        usedTokens = tokenUsage.usedTokens,
                        maxTokens = tokenUsage.maxTokens
                    )
                }
            }
        }
    }

    /**
     * Загрузить сообщения в зависимости от режима
     */
    private fun loadMessages() {
        viewModelScope.launch {
            val currentMode = _screeState.value.selectedMode

            when (currentMode) {
                ConversationMode.SINGLE -> {
                    // В режиме Single загружаем только обычные сообщения
                    conversationUseCase.invoke(conversationId).collect { messages ->
                        _screeState.update {
                            it.copy(
                                messages = messages.filter { msg -> msg.role != Role.SYSTEM }
                            )
                        }
                    }
                }

                ConversationMode.COMMITTEE -> {
                    // В режиме Committee загружаем сообщения вместе с мнениями экспертов
                    getMessagesWithExpertOpinionsUseCase(conversationId).collect { messages ->
                        Logger.getLogger("Committe").info("messages - $messages")

                        _screeState.update {
                            it.copy(
                                messages = messages.filter { msg -> msg.role != Role.SYSTEM }
                            )
                        }
                    }
                }
            }
        }
    }

    internal fun setEvent(event: ConversationUIState.Event) {
        viewModelScope.launch { _events.emit(event) }
    }

    private fun handleEvent(event: ConversationUIState.Event) {
        when (event) {
            is ConversationUIState.Event.SendMessage -> sendMessageToAi(event.message)
            ConversationUIState.Event.ResetAll -> resetConversation()
            ConversationUIState.Event.ClearError -> clearError()
            ConversationUIState.Event.OpenSettings -> {}
            is ConversationUIState.Event.SelectProvider -> selectProvider(event.provider)
            is ConversationUIState.Event.SelectMode -> selectMode(event.mode)
            is ConversationUIState.Event.ToggleExpert -> toggleExpert(event.expert)
        }
    }

    /**
     * Отправка сообщения
     * Выбирает режим: Single AI или Committee
     */
    private fun sendMessageToAi(message: String) {
        if (message.isBlank() || _screeState.value.isLoading) return

        when (_screeState.value.selectedMode) {
            ConversationMode.SINGLE -> sendMessageToSingleAi(message)
            ConversationMode.COMMITTEE -> sendMessageToCommittee(message)
        }
    }

    /**
     * Проверить и выполнить суммаризацию истории при необходимости
     */
    private suspend fun checkAndSummarizeIfNeeded() {
        val state = _screeState.value
        val currentTokens = state.usedTokens
        val maxTokens = state.maxTokens
        val requestTokens = state.requestTokens ?: 0

        // Проверяем, не превысим ли мы порог с учетом текущего запроса
        val projectedTokens = currentTokens + requestTokens
        val usageRatio = projectedTokens.toDouble() / maxTokens.toDouble()

        // Порог 75% для суммаризации
        if (usageRatio >= 0.75) {
            Logger.getLogger("Summarization").info("Превышен порог использования токенов: ${(usageRatio * 100).toInt()}%. Начинаем суммаризацию...")

            _screeState.update { it.copy(isSummarizing = true) }

            // Вызываем суммаризацию
            summarizeHistoryUseCase(
                conversationId = conversationId,
                currentTokens = currentTokens,
                maxTokens = maxTokens,
                provider = _screeState.value.selectedProvider
            ).collect { result ->
                result.doActionIfSuccess { wasSummarized ->
                    if (wasSummarized) {
                        Logger.getLogger("Summarization").info("Суммаризация выполнена успешно")
                    }
                    _screeState.update { it.copy(isSummarizing = false) }
                }
                result.doActionIfError { error ->
                    Logger.getLogger("Summarization").warning("Ошибка при суммаризации: ${error.toUserMessage()}")
                    _screeState.update { it.copy(isSummarizing = false) }
                }
            }
        }
    }

    /**
     * Отправка сообщения в режиме Single AI
     */
    private fun sendMessageToSingleAi(message: String) {
        viewModelScope.launch {
            // Подсчитываем токены ПЕРЕД отправкой
            Logger.getLogger("TokenCount").info("Подсчёт токенов для сообщения: $message")
            getMessageTokenCountUseCase(
                conversationId = conversationId,
                newMessage = message,
                modelUri = _screeState.value.selectedProvider.modelId
            ).collect { tokenResult ->
                tokenResult.doActionIfSuccess { tokenCount ->
                    Logger.getLogger("TokenCount").info("Токенов в запросе: $tokenCount")
                    // Сохраняем количество токенов в state для отображения в UI
                    _screeState.update { it.copy(requestTokens = tokenCount) }
                }
                tokenResult.doActionIfError { domainError ->
                    Logger.getLogger("TokenCount").warning("Ошибка подсчёта токенов: ${domainError.toUserMessage()}")
                    // Сбрасываем токены при ошибке
                    _screeState.update { it.copy(requestTokens = null) }
                }
            }

            // Проверяем и выполняем суммаризацию при необходимости
            checkAndSummarizeIfNeeded()

            // Отправляем сообщение
            sendConversationMessageUseCase.invoke(
                conversationId = conversationId,
                message = message,
                provider = _screeState.value.selectedProvider
            ).collect { result ->
                result.doActionIfLoading {
                    _screeState.update { it.copy(isLoading = true, error = "") }
                }
                result.doActionIfSuccess {
                    _screeState.update { state ->
                        state.copy(
                            isLoading = false,
                            isConversationComplete = it.isComplete,
                            requestTokens = null // Сбрасываем после отправки
                        )
                    }
                }
                result.doActionIfError { domainError ->
                    _screeState.update {
                        it.copy(
                            isLoading = false,
                            error = mapErrorToUserMessage(domainError),
                            requestTokens = null // Сбрасываем при ошибке
                        )
                    }
                }
            }
        }
    }

    /**
     * Отправка сообщения в режиме Committee of Experts
     */
    private fun sendMessageToCommittee(message: String) {
        viewModelScope.launch {
            executeCommitteeUseCase.invoke(
                conversationId = conversationId,
                userMessage = message,
                experts = _screeState.value.selectedExperts,
                provider = _screeState.value.selectedProvider
            ).collect { result ->
                result.doActionIfLoading {
                    _screeState.update { it.copy(isLoading = true, error = "") }
                }
                result.doActionIfSuccess { committeeResult ->
                    when (committeeResult) {
                        is CommitteeResult.ExpertOpinion -> {
                            // Мнение эксперта получено
                            // UI обновится через Flow из БД
                            _screeState.update { it.copy(isLoading = true) }
                        }
                        is CommitteeResult.FinalSynthesis -> {
                            // Финальный ответ получен
                            _screeState.update { state ->
                                state.copy(
                                    isLoading = false,
                                    isConversationComplete = true
                                )
                            }
                        }
                    }
                }
                result.doActionIfError { domainError ->
                    _screeState.update {
                        it.copy(
                            isLoading = false,
                            error = mapErrorToUserMessage(domainError)
                        )
                    }
                }
            }
        }
    }

    /**
     * Выбор провайдера LLM
     */
    private fun selectProvider(provider: LlmProvider) {
        viewModelScope.launch {
            saveSelectedProviderUseCase(conversationId, provider)
            _screeState.update { it.copy(selectedProvider = provider) }
        }
    }

    /**
     * Выбор режима работы (Single AI / Committee)
     */
    private fun selectMode(mode: ConversationMode) {
        _screeState.update { it.copy(selectedMode = mode) }
        // Перезагружаем сообщения при смене режима
        loadMessages()
    }

    /**
     * Переключить эксперта (добавить/убрать из выбранных)
     */
    private fun toggleExpert(expert: Expert) {
        _screeState.update { state ->
            val currentExperts = state.selectedExperts
            val updatedExperts = if (currentExperts.contains(expert)) {
                // Убрать эксперта (но минимум 1 должен остаться)
                if (currentExperts.size > 1) {
                    currentExperts - expert
                } else {
                    currentExperts // Не даем удалить последнего
                }
            } else {
                // Добавить эксперта
                currentExperts + expert
            }
            state.copy(selectedExperts = updatedExperts)
        }
    }

    private fun resetConversation() {
        viewModelScope.launch {
            conversationUseCase.clearConversation(conversationId)
            _screeState.update { it.copy(
                isConversationComplete = false,
                error = ""
            )}
        }
    }

    private fun clearError() {
        _screeState.update { it.copy(error = "") }
    }

    /**
     * Преобразование DomainError в пользовательское сообщение
     * с возможностью кастомизации для специфичных ошибок
     */
    private fun mapErrorToUserMessage(error: DomainError): String {
        return when (error) {
            is DomainError.NetworkError -> {
                when (error.code) {
                    400 -> "Неверный запрос. Проверьте данные и попробуйте снова."
                    401 -> "Ошибка авторизации. Проверьте API ключ."
                    403 -> "Доступ запрещен. Проверьте права доступа."
                    404 -> "Сервис не найден. Проверьте настройки."
                    429 -> "Слишком много запросов. Попробуйте позже."
                    500, 502, 503 -> "Сервер временно недоступен. Попробуйте позже."
                    else -> "Ошибка сети: ${error.message}"
                }
            }
            is DomainError.ValidationError -> {
                "Ошибка валидации: ${error.message}"
            }
            is DomainError.DatabaseError -> {
                "Ошибка сохранения данных: ${error.message}"
            }
            is DomainError.ParseError -> {
                "Ошибка обработки ответа от AI. Попробуйте еще раз."
            }
            is DomainError.BusinessLogicError -> {
                error.message // Бизнес-логика обычно возвращает готовые сообщения
            }
            is DomainError.ConfigurationError -> {
                "Ошибка конфигурации: ${error.message}. Проверьте настройки."
            }
            is DomainError.UnknownError -> {
                "Произошла неизвестная ошибка: ${error.message}"
            }
        }
    }
}
