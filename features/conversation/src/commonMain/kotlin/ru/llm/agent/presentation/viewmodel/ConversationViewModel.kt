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
import ru.llm.agent.core.utils.FileSaveResult
import ru.llm.agent.core.utils.getFileManager
import ru.llm.agent.model.ExportFormat
import ru.llm.agent.usecase.ChatWithMcpToolsUseCase
import ru.llm.agent.usecase.CommitteeResult
import ru.llm.agent.usecase.ConversationUseCase
import ru.llm.agent.usecase.ExecuteCommitteeUseCase
import ru.llm.agent.usecase.ExportConversationUseCase
import ru.llm.agent.usecase.GetMessagesWithExpertOpinionsUseCase
import ru.llm.agent.usecase.GetMessageTokenCountUseCase
import ru.llm.agent.usecase.GetSelectedProviderUseCase
import ru.llm.agent.usecase.GetSummarizationInfoUseCase
import ru.llm.agent.usecase.GetTokenUsageUseCase
import ru.llm.agent.usecase.SaveSelectedProviderUseCase
import ru.llm.agent.usecase.SendConversationMessageUseCase
import ru.llm.agent.usecase.SummarizeHistoryUseCase
import ru.llm.agent.utils.settings.AppSettings
import java.util.logging.Logger
import ru.llm.agent.mcp.presentation.viewmodel.McpViewModel
import ru.llm.agent.mcp.utils.extractToolName
import ru.llm.agent.mcp.utils.extractToolResult

class ConversationViewModel(
    private val conversationUseCase: ConversationUseCase,
    private val sendConversationMessageUseCase: SendConversationMessageUseCase,
    private val chatWithMcpToolsUseCase: ChatWithMcpToolsUseCase,
    private val getSelectedProviderUseCase: GetSelectedProviderUseCase,
    private val saveSelectedProviderUseCase: SaveSelectedProviderUseCase,
    private val getMessagesWithExpertOpinionsUseCase: GetMessagesWithExpertOpinionsUseCase,
    private val executeCommitteeUseCase: ExecuteCommitteeUseCase,
    private val getTokenUsageUseCase: GetTokenUsageUseCase,
    private val getMessageTokenCountUseCase: GetMessageTokenCountUseCase,
    private val summarizeHistoryUseCase: SummarizeHistoryUseCase,
    private val getSummarizationInfoUseCase: GetSummarizationInfoUseCase,
    private val exportConversationUseCase: ExportConversationUseCase,
    private val appSettings: AppSettings,
    private val indexTextUseCase: ru.llm.agent.usecase.rag.IndexTextUseCase,
    private val askWithRagUseCase: ru.llm.agent.usecase.rag.AskWithRagUseCase,
    private val getRagIndexStatsUseCase: ru.llm.agent.usecase.rag.GetRagIndexStatsUseCase,
    private val clearRagIndexUseCase: ru.llm.agent.usecase.rag.ClearRagIndexUseCase,
    private val mcpViewModel: McpViewModel
) : ViewModel() {


    private val fileManager = getFileManager()

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

        // Подписка на изменения состояния MCP для логирования и синхронизации
        viewModelScope.launch {
            mcpViewModel.state.collect { mcpState ->
                handleMcpStateChange(mcpState)
            }
        }
    }

    /**
     * Обработка изменений состояния MCP модуля
     */
    private fun handleMcpStateChange(mcpState: ru.llm.agent.mcp.presentation.state.McpState) {
        // Логируем изменения для отладки
        val currentExecution = mcpState.currentExecution
        if (currentExecution != null) {
            Logger.getLogger("MCP-Integration")
                .info("Tool execution status: ${currentExecution.toolName}, " +
                        "executing: ${currentExecution.isExecuting}")
        }

        // Можно добавить дополнительную логику реакции на изменения MCP:
        // - Обновление UI индикаторов
        // - Синхронизация с другими компонентами
        // - Логика зависящая от статуса выполнения инструментов
    }

    fun start(){
        viewModelScope.launch {
            // Загружаем настройки приложения
            loadSettings()

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
                Logger.getLogger("TokenUsage").info("Token usage updated: used=${tokenUsage.usedTokens}, max=${tokenUsage.maxTokens}")
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
            is ConversationUIState.Event.ExportConversation -> exportConversation(event.format)
            is ConversationUIState.Event.SetTrelloBoardId -> setTrelloBoardId(event.boardId)
            is ConversationUIState.Event.ToggleRag -> toggleRag(event.enabled)
            ConversationUIState.Event.ShowKnowledgeBaseDialog -> showKnowledgeBaseDialog()
            ConversationUIState.Event.HideKnowledgeBaseDialog -> hideKnowledgeBaseDialog()
            is ConversationUIState.Event.AddToKnowledgeBase -> addToKnowledgeBase(event.text, event.sourceId)
            ConversationUIState.Event.ClearKnowledgeBase -> clearKnowledgeBase()
        }
    }

    /**
     * Установить ID доски Trello
     */
    private fun setTrelloBoardId(boardId: String?) {
        _screeState.value = _screeState.value.copy(trelloBoardId = boardId)
        // Сохраняем в настройки
        appSettings.trelloBoardId = boardId
    }

    /**
     * Загрузить настройки при старте
     */
    private fun loadSettings() {
        // Загружаем сохранённый Board ID
        val savedBoardId = appSettings.trelloBoardId
        if (savedBoardId != null) {
            _screeState.value = _screeState.value.copy(trelloBoardId = savedBoardId)
        }
    }


    /**
     * Отправка сообщения
     * Выбирает режим: Single AI или Committee
     */
    private fun sendMessageToAi(message: String) {
        if (message.isBlank() || _screeState.value.isLoading) return

        when (_screeState.value.selectedMode) {
            ConversationMode.SINGLE -> {
                val mcpState = mcpViewModel.state.value
                val shouldUseMcp = mcpState.isEnabled && mcpState.availableTools.isNotEmpty()

                Logger.getLogger("MCP").info("Отправка сообщения: MCP enabled=${mcpState.isEnabled}, tools=${mcpState.availableTools.size}, shouldUseMcp=$shouldUseMcp")

                if (shouldUseMcp) {
                    sendMessageWithMcpTools(message)
                } else {
                    sendMessageToSingleAi(message)
                }
            }
            ConversationMode.COMMITTEE -> sendMessageToCommittee(message)
        }
    }

    /**
     * Отправка сообщения с полным циклом MCP tool calling
     * Использует ChatWithMcpToolsUseCase и интегрируется с McpViewModel для отображения статуса
     */
    private fun sendMessageWithMcpTools(message: String, needAddToHistory: Boolean = true) {
        viewModelScope.launch {
            chatWithMcpToolsUseCase.invoke(
                conversationId = conversationId,
                message = message,
                provider = _screeState.value.selectedProvider,
                needAddToHistory = needAddToHistory,
                availableTools = mcpViewModel.getAvailableTools()
            ).collect { result ->
                result.doActionIfLoading {
                    _screeState.update { it.copy(isLoading = true, error = "") }
                }
                result.doActionIfSuccess { conversationMessage ->
                    // Если это промежуточный результат tool call, обновляем статус выполнения через McpViewModel
                    if (conversationMessage.isContinue) {
                        Logger.getLogger("MCP").info("Tool execution: ${conversationMessage.text}")

                        // Извлекаем название инструмента из текста сообщения
                        val toolName = conversationMessage.text.extractToolName()
                        val toolResult = conversationMessage.text.extractToolResult()

                        // Обновляем статус через McpViewModel
                        mcpViewModel.updateToolExecution(
                            toolName = toolName,
                            description = "Обработка запроса...\n$toolResult",
                            isExecuting = true
                        )

                        _screeState.update { state ->
                            state.copy(
                                isLoading = false,
                                isConversationComplete = false,
                                requestTokens = null
                            )
                        }
                    } else {
                        // Финальный ответ - очищаем статус выполнения инструмента через McpViewModel
                        mcpViewModel.clearCurrentExecution()

                        _screeState.update { state ->
                            state.copy(
                                isLoading = false,
                                isConversationComplete = false,
                                requestTokens = null
                            )
                        }
                    }
                }
                result.doActionIfError { domainError ->
                    // При ошибке очищаем статус выполнения
                    mcpViewModel.clearCurrentExecution()

                    _screeState.update {
                        it.copy(
                            isLoading = false,
                            error = mapErrorToUserMessage(domainError),
                            requestTokens = null
                        )
                    }
                }
            }
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

            // Выбираем UseCase в зависимости от того, включен ли RAG
            val useCaseFlow = if (_screeState.value.isRagEnabled) {
                Logger.getLogger("RAG").info("Используем RAG для поиска релевантного контекста")
                askWithRagUseCase.invoke(
                    conversationId = conversationId,
                    userMessage = message,
                    provider = _screeState.value.selectedProvider,
                    topK = 3,
                    threshold = 0.3
                )
            } else {
                sendConversationMessageUseCase.invoke(
                    conversationId = conversationId,
                    message = message,
                    provider = _screeState.value.selectedProvider,
                )
            }

            // Обрабатываем ответ
            useCaseFlow.collect { result ->
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
     * Экспортировать диалог в указанном формате
     */
    private fun exportConversation(format: ExportFormat) {
        viewModelScope.launch {
            Logger.getLogger("Export").info("Начинаем экспорт диалога в формате ${format.name}")

            exportConversationUseCase.invoke(
                conversationId = conversationId,
                format = format
            ).collect { result ->
                result.doActionIfLoading {
                    Logger.getLogger("Export").info("Экспортируем диалог...")
                }
                result.doActionIfSuccess { exportedData ->
                    viewModelScope.launch {
                        Logger.getLogger("Export").info("Диалог экспортирован, сохраняем файл...")

                        // Генерируем имя файла
                        val timestamp = System.currentTimeMillis()
                        val fileName = "conversation_${conversationId}_$timestamp.${format.extension}"

                        // Сохраняем файл
                        when (val saveResult = fileManager.saveFile(fileName, exportedData, format.mimeType)) {
                            is FileSaveResult.Success -> {
                                Logger.getLogger("Export").info("Файл успешно сохранен: ${saveResult.filePath}")
                                _screeState.update {
                                    it.copy(error = "Диалог экспортирован: ${saveResult.filePath}")
                                }
                            }
                            is FileSaveResult.Cancelled -> {
                                Logger.getLogger("Export").info("Пользователь отменил сохранение")
                            }
                            is FileSaveResult.Error -> {
                                Logger.getLogger("Export").warning("Ошибка сохранения файла: ${saveResult.message}")
                                _screeState.update {
                                    it.copy(error = saveResult.message)
                                }
                            }
                        }
                    }
                }
                result.doActionIfError { domainError ->
                    val errorMessage = domainError.toUserMessage()
                    Logger.getLogger("Export").warning("Ошибка экспорта: $errorMessage")
                    _screeState.update {
                        it.copy(error = "Ошибка экспорта: $errorMessage")
                    }
                }
            }
        }
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

    // === RAG функции ===

    /**
     * Переключить использование RAG
     */
    private fun toggleRag(enabled: Boolean) {
        _screeState.update { it.copy(isRagEnabled = enabled) }

        // Загружаем статистику индекса при включении
        if (enabled) {
            viewModelScope.launch {
                val count = getRagIndexStatsUseCase()
                _screeState.update { it.copy(ragIndexedCount = count) }
            }
        }
    }

    /**
     * Показать диалог добавления знаний
     */
    private fun showKnowledgeBaseDialog() {
        _screeState.update { it.copy(showKnowledgeBaseDialog = true) }
    }

    /**
     * Скрыть диалог добавления знаний
     */
    private fun hideKnowledgeBaseDialog() {
        _screeState.update { it.copy(showKnowledgeBaseDialog = false) }
    }

    /**
     * Добавить текст в базу знаний
     */
    private fun addToKnowledgeBase(text: String, sourceId: String) {
        viewModelScope.launch {
            try {
                Logger.getLogger("RAG").info("Индексация текста: $sourceId")
                _screeState.update { it.copy(isLoading = true, error = "") }

                val result = indexTextUseCase.invoke(text, sourceId)

                Logger.getLogger("RAG").info("Проиндексировано ${result.chunksIndexed} чанков")

                // Обновляем счетчик документов
                val count = getRagIndexStatsUseCase()
                _screeState.update {
                    it.copy(
                        isLoading = false,
                        ragIndexedCount = count,
                        showKnowledgeBaseDialog = false,
                        error = "✓ Добавлено ${result.chunksIndexed} фрагментов в базу знаний"
                    )
                }
            } catch (e: Exception) {
                Logger.getLogger("RAG").warning("Ошибка индексации: ${e.message}")
                _screeState.update {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка добавления в базу знаний: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Очистить базу знаний
     */
    private fun clearKnowledgeBase() {
        viewModelScope.launch {
            try {
                Logger.getLogger("RAG").info("Очистка базы знаний")
                clearRagIndexUseCase()
                _screeState.update {
                    it.copy(
                        ragIndexedCount = 0,
                        error = "✓ База знаний очищена"
                    )
                }
            } catch (e: Exception) {
                Logger.getLogger("RAG").warning("Ошибка очистки: ${e.message}")
                _screeState.update {
                    it.copy(error = "Ошибка очистки базы знаний: ${e.message}")
                }
            }
        }
    }

    // === MCP функции ===

    /**
     * Выполнить MCP инструмент через LLM
     *
     * Когда пользователь вручную выбирает инструмент из UI,
     * создаём сообщение для LLM с просьбой использовать этот инструмент.
     * LLM получит контекст разговора и сам решит, как использовать инструмент.
     *
     * Для Trello инструментов использует SmartPromptTemplate для создания осмысленных промптов.
     */
    fun executeToolWithLlm(toolName: String, description: String) {
        if (_screeState.value.isLoading) {
            Logger.getLogger("MCP").info("Игнорируем вызов инструмента - уже идёт обработка")
            return
        }

        Logger.getLogger("MCP").info("Ручной вызов инструмента через LLM: $toolName")

        // Формируем промпт для LLM
        val prompt = if (toolName.startsWith("trello_")) {
            // Для Trello инструментов создаём осмысленный промпт с контекстом
            createTrelloPrompt(toolName, description)
        } else {
            // Для других инструментов используем простой формат
            buildString {
                append("Используй инструмент \"$toolName\"")
                if (description.isNotBlank()) {
                    append(" для следующей задачи: $description")
                }
            }
        }

        Logger.getLogger("MCP").info("Сформирован промпт: $prompt")

        // Вызываем обычный flow с MCP
        sendMessageWithMcpTools(message = prompt, needAddToHistory = true)
    }

    /**
     * Создаёт осмысленный промпт для Trello инструмента
     */
    private fun createTrelloPrompt(toolName: String, description: String): String {
        val boardId = _screeState.value.trelloBoardId

        return when (toolName) {
            "trello_getSummary" -> {
                if (boardId != null) {
                    "Покажи статистику по Trello доске $boardId"
                } else {
                    "Покажи статистику по моей Trello доске"
                }
            }
            "trello_getCards" -> {
                if (boardId != null) {
                    "Покажи все задачи из Trello доски $boardId"
                } else {
                    "Покажи все мои задачи из Trello"
                }
            }
            "trello_searchCards" -> {
                if (boardId != null) {
                    "Найди задачи в Trello на доске $boardId"
                } else {
                    "Найди задачи в моей Trello доске"
                }
            }
            "trello_quickTask" -> {
                if (boardId != null) {
                    "Создай быструю задачу в Trello на доске $boardId"
                } else {
                    "Создай быструю задачу в моей Trello доске"
                }
            }
            "trello_createCard" -> {
                if (boardId != null) {
                    "Создай новую карточку в Trello на доске $boardId"
                } else {
                    "Создай новую карточку в моей Trello доске"
                }
            }
            else -> {
                // Для остальных Trello инструментов используем описание
                if (boardId != null) {
                    "Используй Trello доску $boardId: $description"
                } else {
                    description
                }
            }
        }
    }
}
