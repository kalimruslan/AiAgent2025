package ru.llm.agent.committee.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.llm.agent.committee.presentation.state.CommitteeEvent
import ru.llm.agent.committee.presentation.state.CommitteeState
import ru.llm.agent.model.Expert

/**
 * ViewModel для управления модулем Committee (Комитет экспертов)
 */
class CommitteeViewModel : ViewModel() {

    private val _state = MutableStateFlow(CommitteeState.empty())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<CommitteeEvent>()

    init {
        viewModelScope.launch {
            _events.collect { event ->
                handleEvent(event)
            }
        }
    }

    /**
     * Обработка событий от UI
     */
    fun onEvent(event: CommitteeEvent) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }

    private fun handleEvent(event: CommitteeEvent) {
        when (event) {
            is CommitteeEvent.ToggleExpert -> toggleExpert(event.expert)
            is CommitteeEvent.ClearError -> clearError()
            is CommitteeEvent.TogglePanel -> togglePanel()
        }
    }

    private fun toggleExpert(expert: Expert) {
        _state.update { currentState ->
            val newSelected = if (currentState.selectedExperts.contains(expert)) {
                currentState.selectedExperts - expert
            } else {
                currentState.selectedExperts + expert
            }
            currentState.copy(selectedExperts = newSelected)
        }
    }

    private fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun togglePanel() {
        _state.update { it.copy(isPanelExpanded = !it.isPanelExpanded) }
    }

    // ===== Публичные методы для интеграции с другими модулями =====

    /**
     * Получить список выбранных экспертов (для использования из ConversationViewModel)
     */
    fun getSelectedExperts(): List<Expert> = _state.value.selectedExperts

    /**
     * Установить список выбранных экспертов
     */
    fun setSelectedExperts(experts: List<Expert>) {
        _state.update { it.copy(selectedExperts = experts) }
    }
}