package ru.llm.agent.committee.presentation.state

import ru.llm.agent.model.Expert

/**
 * События для модуля Committee (Комитет экспертов)
 */
sealed interface CommitteeEvent {
    /** Переключить эксперта (добавить/убрать из выбранных) */
    data class ToggleExpert(val expert: Expert) : CommitteeEvent

    /** Очистить ошибку */
    data object ClearError : CommitteeEvent

    /** Переключить раскрытие панели */
    data object TogglePanel : CommitteeEvent
}