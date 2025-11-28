package ru.llm.agent.rag.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.llm.agent.rag.model.RagOperationStatus
import ru.llm.agent.rag.presentation.state.RagEvent
import ru.llm.agent.rag.presentation.state.RagState
import ru.llm.agent.usecase.rag.ClearRagIndexUseCase
import ru.llm.agent.usecase.rag.GetRagIndexStatsUseCase
import ru.llm.agent.usecase.rag.IndexTextUseCase

/**
 * ViewModel для управления RAG функциональностью
 */
class RagViewModel(
    private val indexTextUseCase: IndexTextUseCase,
    private val getRagIndexStatsUseCase: GetRagIndexStatsUseCase,
    private val clearRagIndexUseCase: ClearRagIndexUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(RagState.empty())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<RagEvent>()

    /** Максимальное количество операций в истории */
    private val maxHistorySize = 5

    init {
        viewModelScope.launch {
            _events.collect { event ->
                handleEvent(event)
            }
        }
        // Загружаем статистику при инициализации
        onEvent(RagEvent.LoadStats)
    }

    /**
     * Обработка событий от UI
     */
    fun onEvent(event: RagEvent) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }

    private fun handleEvent(event: RagEvent) {
        when (event) {
            is RagEvent.ToggleEnabled -> toggleEnabled(event.enabled)
            is RagEvent.LoadStats -> loadStats()
            is RagEvent.ShowKnowledgeBaseDialog -> showKnowledgeBaseDialog()
            is RagEvent.HideKnowledgeBaseDialog -> hideKnowledgeBaseDialog()
            is RagEvent.AddToKnowledgeBase -> addToKnowledgeBase(event.text, event.sourceId)
            is RagEvent.ClearKnowledgeBase -> clearKnowledgeBase()
            is RagEvent.SetThreshold -> setThreshold(event.threshold)
            is RagEvent.SetTopK -> setTopK(event.topK)
            is RagEvent.ToggleMmr -> toggleMmr(event.enabled)
            is RagEvent.SetMmrLambda -> setMmrLambda(event.lambda)
            is RagEvent.ToggleSettings -> toggleSettings()
            is RagEvent.ClearError -> clearError()
            is RagEvent.ClearHistory -> clearHistory()
        }
    }

    // === Приватные методы обработки событий ===

    private fun toggleEnabled(enabled: Boolean) {
        println("RAG toggleEnabled: $enabled")
        _state.update { it.copy(isEnabled = enabled) }
        if (enabled) {
            loadStats()
        }
        println("RAG state after toggle: isEnabled=${_state.value.isEnabled}")
    }

    private fun loadStats() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingStats = true, error = null) }
            try {
                val count = getRagIndexStatsUseCase()
                _state.update { it.copy(indexedCount = count, isLoadingStats = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoadingStats = false,
                        error = "Ошибка загрузки статистики: ${e.message}"
                    )
                }
            }
        }
    }

    private fun showKnowledgeBaseDialog() {
        _state.update { it.copy(showKnowledgeBaseDialog = true) }
    }

    private fun hideKnowledgeBaseDialog() {
        _state.update { it.copy(showKnowledgeBaseDialog = false) }
    }

    private fun addToKnowledgeBase(text: String, sourceId: String) {
        viewModelScope.launch {
            val operation = RagOperationStatus(
                operationType = "indexing",
                description = "Индексация: $sourceId",
                isExecuting = true
            )

            _state.update {
                it.copy(
                    isIndexing = true,
                    currentOperation = operation,
                    error = null
                )
            }

            try {
                val result = indexTextUseCase.invoke(text, sourceId)
                val count = getRagIndexStatsUseCase()

                val completedOperation = operation.copy(
                    isExecuting = false,
                    result = "Добавлено ${result.chunksIndexed} фрагментов"
                )

                _state.update {
                    it.copy(
                        isIndexing = false,
                        indexedCount = count,
                        showKnowledgeBaseDialog = false,
                        currentOperation = null,
                        operationHistory = addToHistory(it.operationHistory, completedOperation)
                    )
                }
            } catch (e: Exception) {
                val errorOperation = operation.copy(
                    isExecuting = false,
                    error = e.message ?: "Неизвестная ошибка"
                )

                _state.update {
                    it.copy(
                        isIndexing = false,
                        currentOperation = null,
                        operationHistory = addToHistory(it.operationHistory, errorOperation),
                        error = "Ошибка индексации: ${e.message}"
                    )
                }
            }
        }
    }

    private fun clearKnowledgeBase() {
        viewModelScope.launch {
            val operation = RagOperationStatus(
                operationType = "clearing",
                description = "Очистка базы знаний",
                isExecuting = true
            )

            _state.update {
                it.copy(
                    currentOperation = operation,
                    error = null
                )
            }

            try {
                clearRagIndexUseCase()

                val completedOperation = operation.copy(
                    isExecuting = false,
                    result = "База знаний очищена"
                )

                _state.update {
                    it.copy(
                        indexedCount = 0,
                        currentOperation = null,
                        operationHistory = addToHistory(it.operationHistory, completedOperation)
                    )
                }
            } catch (e: Exception) {
                val errorOperation = operation.copy(
                    isExecuting = false,
                    error = e.message ?: "Неизвестная ошибка"
                )

                _state.update {
                    it.copy(
                        currentOperation = null,
                        operationHistory = addToHistory(it.operationHistory, errorOperation),
                        error = "Ошибка очистки: ${e.message}"
                    )
                }
            }
        }
    }

    private fun setThreshold(threshold: Double) {
        _state.update { it.copy(threshold = threshold.coerceIn(0.0, 1.0)) }
    }

    private fun setTopK(topK: Int) {
        _state.update { it.copy(topK = topK.coerceIn(1, 10)) }
    }

    private fun toggleMmr(enabled: Boolean) {
        _state.update { it.copy(useMmr = enabled) }
    }

    private fun setMmrLambda(lambda: Double) {
        _state.update { it.copy(mmrLambda = lambda.coerceIn(0.0, 1.0)) }
    }

    private fun toggleSettings() {
        _state.update { it.copy(isSettingsExpanded = !it.isSettingsExpanded) }
    }

    private fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun clearHistory() {
        _state.update { it.copy(operationHistory = emptyList()) }
    }

    /**
     * Добавить операцию в историю, ограничивая размер
     */
    private fun addToHistory(
        history: List<RagOperationStatus>,
        operation: RagOperationStatus
    ): List<RagOperationStatus> {
        return (history + operation).takeLast(maxHistorySize)
    }

    // ===== Публичные методы для интеграции с ConversationViewModel =====

    /**
     * Проверить, включен ли RAG
     */
    fun isRagEnabled(): Boolean = _state.value.isEnabled

    /**
     * Получить текущие настройки RAG для использования в AskWithRagUseCase
     */
    fun getRagSettings(): RagSettings = RagSettings(
        threshold = _state.value.threshold,
        topK = _state.value.topK,
        useMmr = _state.value.useMmr,
        mmrLambda = _state.value.mmrLambda
    )

    /**
     * Уведомить о завершении операции (для использования из ConversationViewModel)
     */
    fun clearCurrentOperation() {
        _state.update { it.copy(currentOperation = null) }
    }
}

/**
 * Настройки RAG для передачи в use cases
 */
data class RagSettings(
    val threshold: Double,
    val topK: Int,
    val useMmr: Boolean,
    val mmrLambda: Double
)
