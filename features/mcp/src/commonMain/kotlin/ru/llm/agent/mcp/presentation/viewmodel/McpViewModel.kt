package ru.llm.agent.mcp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.llm.agent.mcp.model.McpToolExecutionStatus
import ru.llm.agent.mcp.presentation.state.McpEvent
import ru.llm.agent.mcp.presentation.state.McpState
import ru.llm.agent.usecase.GetMcpToolsUseCase

/**
 * ViewModel для управления MCP функциональностью
 */
class McpViewModel(
    private val getMcpToolsUseCase: GetMcpToolsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(McpState.empty())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<McpEvent>()

    init {
        viewModelScope.launch {
            _events.collect { event ->
                handleEvent(event)
            }
        }
        // Загружаем инструменты при инициализации
        onEvent(McpEvent.LoadTools)
    }

    /**
     * Обработка событий от UI
     */
    fun onEvent(event: McpEvent) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }

    private fun handleEvent(event: McpEvent) {
        when (event) {
            is McpEvent.LoadTools -> loadTools()
            is McpEvent.ToggleEnabled -> toggleEnabled(event.enabled)
            is McpEvent.ExecuteTool -> executeTool(event.toolName, event.arguments)
            is McpEvent.CancelExecution -> cancelExecution()
            is McpEvent.ClearHistory -> clearHistory()
            is McpEvent.TogglePanel -> togglePanel()
            is McpEvent.ClearError -> clearError()
        }
    }

    private fun loadTools() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingTools = true, error = null) }
            try {
                val tools = getMcpToolsUseCase.invoke()
                _state.update {
                    it.copy(
                        availableTools = tools,
                        isLoadingTools = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoadingTools = false,
                        error = "Ошибка загрузки инструментов: ${e.message}"
                    )
                }
            }
        }
    }

    private fun toggleEnabled(enabled: Boolean) {
        _state.update { it.copy(isEnabled = enabled) }
    }

    private fun executeTool(toolName: String, arguments: Map<String, Any>) {
        viewModelScope.launch {
            val tool = _state.value.availableTools.find { it.name == toolName }
            if (tool == null) {
                _state.update {
                    it.copy(error = "Инструмент '$toolName' не найден")
                }
                return@launch
            }

            val executionStatus = McpToolExecutionStatus(
                toolName = toolName,
                description = tool.description,
                isExecuting = true
            )

            _state.update {
                it.copy(
                    currentExecution = executionStatus,
                    error = null
                )
            }

            // TODO: Реализовать вызов инструмента через use case
            // Пока просто симулируем выполнение
            kotlinx.coroutines.delay(2000)

            val completedExecution = executionStatus.copy(
                isExecuting = false,
                result = "Выполнено успешно"
            )

            _state.update {
                it.copy(
                    currentExecution = null,
                    executionHistory = it.executionHistory + completedExecution
                )
            }
        }
    }

    private fun cancelExecution() {
        _state.update { it.copy(currentExecution = null) }
    }

    private fun clearHistory() {
        _state.update { it.copy(executionHistory = emptyList()) }
    }

    private fun togglePanel() {
        _state.update { it.copy(isPanelExpanded = !it.isPanelExpanded) }
    }

    private fun clearError() {
        _state.update { it.copy(error = null) }
    }

    // ===== Публичные методы для интеграции с другими модулями =====

    /**
     * Обновить статус выполнения инструмента (для использования из ConversationViewModel)
     */
    fun updateToolExecution(
        toolName: String,
        description: String,
        isExecuting: Boolean,
        result: String? = null
    ) {
        val execution = McpToolExecutionStatus(
            toolName = toolName,
            description = description,
            isExecuting = isExecuting,
            result = result
        )

        _state.update {
            it.copy(currentExecution = execution)
        }
    }

    /**
     * Очистить текущее выполнение и добавить в историю
     */
    fun clearCurrentExecution() {
        val current = _state.value.currentExecution
        if (current != null && !current.isExecuting) {
            // Добавляем в историю только завершённые выполнения
            _state.update {
                it.copy(
                    currentExecution = null,
                    executionHistory = it.executionHistory + current
                )
            }
        } else {
            _state.update {
                it.copy(currentExecution = null)
            }
        }
    }

    /**
     * Получить список доступных инструментов (для использования в других модулях)
     */
    fun getAvailableTools() = _state.value.availableTools

    /**
     * Проверить, включены ли MCP инструменты
     */
    fun isMcpEnabled() = _state.value.isEnabled
}