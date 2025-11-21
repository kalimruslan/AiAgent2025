package ru.llm.agent.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.llm.agent.InteractYaGptWithMcpService
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
import ru.llm.agent.usecase.GetMcpToolsUseCase
import ru.llm.agent.usecase.GetMessagesWithExpertOpinionsUseCase
import ru.llm.agent.usecase.GetMessageTokenCountUseCase
import ru.llm.agent.usecase.GetSelectedProviderUseCase
import ru.llm.agent.usecase.GetSummarizationInfoUseCase
import ru.llm.agent.usecase.GetTokenUsageUseCase
import ru.llm.agent.usecase.MonitorBoardSummaryUseCase
import ru.llm.agent.usecase.SaveSelectedProviderUseCase
import ru.llm.agent.usecase.SendConversationMessageUseCase
import ru.llm.agent.usecase.SummarizeHistoryUseCase
import ru.llm.agent.utils.settings.AppSettings
import java.util.logging.Logger
import kotlinx.coroutines.Job

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
    private val getSummarizationInfoUseCase: GetSummarizationInfoUseCase,
    private val exportConversationUseCase: ExportConversationUseCase,
    private val getMcpToolsUseCase: GetMcpToolsUseCase,
    private val interactYaGptWithMcpService: InteractYaGptWithMcpService,
    private val chatWithMcpToolsUseCase: ChatWithMcpToolsUseCase,
    private val monitorBoardSummaryUseCase: MonitorBoardSummaryUseCase,
    private val appSettings: AppSettings
) : ViewModel() {

    private val fileManager = getFileManager()

    private val _screeState = MutableStateFlow(ConversationUIState.State.empty())
    internal val screeState = _screeState.asStateFlow()

    private val _events = MutableSharedFlow<ConversationUIState.Event>()
    val conversationId = "default_conversation"

    // Job для мониторинга Trello доски
    private var monitoringJob: Job? = null

    init {
        viewModelScope.launch {
            _events.collect {
                handleEvent(it)
            }
        }
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

            // Запускаем мониторинг Trello доски только если MCP инструменты включены
//            if (_screeState.value.isUsedMcpTools) {
//                startBoardMonitoring()
//            }
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
            is ConversationUIState.Event.ExportConversation -> exportConversation(event.format)
            is ConversationUIState.Event.SwitchNeedMcpTools -> switchNeedMcpTools(event.useTools)
            is ConversationUIState.Event.SetTrelloBoardId -> setTrelloBoardId(event.boardId)
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
     * Переключить использование MCP инструментов
     */
    private fun switchNeedMcpTools(useTools: Boolean) {
        _screeState.update { it.copy(isUsedMcpTools = useTools) }

        // Управляем мониторингом доски в зависимости от флага
//        if (useTools) {
//            startBoardMonitoring()
//        } else {
//            stopBoardMonitoring()
//        }
    }

    /**
     * Отправка сообщения
     * Выбирает режим: Single AI или Committee
     */
    private fun sendMessageToAi(message: String) {
        if (message.isBlank() || _screeState.value.isLoading) return

        when (_screeState.value.selectedMode) {
            ConversationMode.SINGLE -> {
                if(_screeState.value.isUsedMcpTools){
                    sendMessageWithMcpTools(message)
                } else{
                    sendMessageToSingleAi(message)
                }
            }
            ConversationMode.COMMITTEE -> sendMessageToCommittee(message)
        }
    }

    /**
     * Отправка сообщения с полным циклом MCP tool calling
     * Использует новый ChatWithMcpToolsUseCase для автоматической обработки tool calls
     */
    private fun sendMessageWithMcpTools(message: String, needAddToHistory: Boolean = true) {
        viewModelScope.launch {
            chatWithMcpToolsUseCase.invoke(
                conversationId = conversationId,
                message = message,
                provider = _screeState.value.selectedProvider,
                needAddToHistory = needAddToHistory,
                availableTools = _screeState.value.availableTools
            ).collect { result ->
                result.doActionIfLoading {
                    _screeState.update { it.copy(isLoading = true, error = "") }
                }
                result.doActionIfSuccess { conversationMessage ->
                    // Если это промежуточный результат tool call, обновляем статус выполнения
                    if (conversationMessage.isContinue) {
                        Logger.getLogger("MCP").info("Tool execution: ${conversationMessage.text}")

                        // Извлекаем название инструмента из текста сообщения
                        val toolName = extractToolNameFromMessage(conversationMessage.text)
                        val result = extractToolResultFromMessage(conversationMessage.text)

                        _screeState.update { state ->
                            state.copy(
                                isLoading = false,
                                isConversationComplete = false,
                                requestTokens = null,
                                currentToolExecution = ConversationUIState.ToolExecutionStatus(
                                    toolName = toolName,
                                    description = "Обработка запроса...\n$result",
                                    isExecuting = true
                                )
                            )
                        }
                    } else {
                        // Финальный ответ - очищаем статус выполнения инструмента
                        _screeState.update { state ->
                            state.copy(
                                isLoading = false,
                                isConversationComplete = false,
                                requestTokens = null,
                                currentToolExecution = null
                            )
                        }
                    }
                }
                result.doActionIfError { domainError ->
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

            // Отправляем сообщение
            sendConversationMessageUseCase.invoke(
                conversationId = conversationId,
                message = message,
                provider = _screeState.value.selectedProvider,
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
     * Запустить мониторинг Trello доски
     * Каждые 5 минут получает саммари и отправляет агенту для анализа
     */
    private fun startBoardMonitoring() {
        // Останавливаем предыдущий мониторинг, если он был запущен
        monitoringJob?.cancel()

        // ID доски Trello (можно сделать настраиваемым позже)
        val boardId = "691da04e5be13a45aeb63b0a"

        monitoringJob = viewModelScope.launch {

            // Показываем индикатор загрузки
            _screeState.update {
                it.copy(boardSummary = ConversationUIState.BoardSummary(
                    text = "Загрузка саммари доски...",
                    isLoading = true
                ))
            }

            monitorBoardSummaryUseCase.invoke(
                boardId = boardId,
                intervalMinutes = 5
            ).collect { summary ->
                // Обновляем state с полученным саммари
                _screeState.update {
                    it.copy(boardSummary = ConversationUIState.BoardSummary(
                        text = summary,
                        timestamp = System.currentTimeMillis(),
                        isLoading = false,
                        isAnalysisLoading = true
                    ))
                }

                // Отправляем саммари агенту для анализа
                analyzeBoardSummary(summary)
            }
        }
    }

    /**
     * Отправить саммари агенту для анализа
     * Ответ сохраняется в BoardSummary, а не добавляется в чат
     */
    private fun analyzeBoardSummary(summary: String) {
        viewModelScope.launch {
            val agentPrompt = buildString {
                appendLine("📊 Периодический отчёт по доске Trello:")
                appendLine()
                appendLine(summary)
                appendLine()
                appendLine("Пожалуйста, проанализируй изменения и предоставь краткий обзор.")
            }

            chatWithMcpToolsUseCase.invoke(
                conversationId = conversationId,
                message = agentPrompt,
                provider = _screeState.value.selectedProvider,
                needAddToHistory = false,
                availableTools = _screeState.value.availableTools
            ).collect { result ->
                result.doActionIfSuccess { conversationMessage ->
                    // Игнорируем промежуточные результаты tool calls
                    if (!conversationMessage.isContinue) {
                        Logger.getLogger("BoardMonitoring").info("Получен анализ от ассистента: ${conversationMessage.text}")

                        // Сохраняем анализ в BoardSummary
                        _screeState.update { state ->
                            state.copy(
                                boardSummary = state.boardSummary?.copy(
                                    assistantAnalysis = conversationMessage.text,
                                    isAnalysisLoading = false
                                )
                            )
                        }
                    }
                }
                result.doActionIfError { domainError ->
                    Logger.getLogger("BoardMonitoring").warning("Ошибка анализа саммари: ${domainError.toUserMessage()}")

                    // Убираем индикатор загрузки при ошибке
                    _screeState.update { state ->
                        state.copy(
                            boardSummary = state.boardSummary?.copy(
                                assistantAnalysis = "Ошибка анализа: ${domainError.toUserMessage()}",
                                isAnalysisLoading = false
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Остановить мониторинг доски
     */
    private fun stopBoardMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        Logger.getLogger("BoardMonitoring").info("Мониторинг доски остановлен")
    }

    /**
     * Извлечь название инструмента из текста сообщения
     * Формат: "Выполнение инструмента: tool_name\nРезультат: ..."
     */
    private fun extractToolNameFromMessage(messageText: String): String {
        return try {
            // Пытаемся найти паттерн "Выполнение инструмента: название"
            val pattern = "Выполнение инструмента: ([^\\n]+)".toRegex()
            val match = pattern.find(messageText)
            match?.groupValues?.getOrNull(1)?.trim() ?: "Инструмент"
        } catch (e: Exception) {
            "Инструмент"
        }
    }

    private fun extractToolResultFromMessage(messageText: String): String {
        return try {
            // Пытаемся найти паттерн "Выполнение инструмента: название"
            val pattern = "Результат: ([^\\n]+)".toRegex()
            val match = pattern.find(messageText)
            match?.groupValues?.getOrNull(1)?.trim() ?: ""
        } catch (e: Exception) {
            ""
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
}
