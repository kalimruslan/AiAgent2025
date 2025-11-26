package ru.llm.agent.committee.presentation.state

import ru.llm.agent.model.Expert
import ru.llm.agent.model.ExpertOpinion

/**
 * UI состояние для модуля Committee (Комитет экспертов)
 */
data class CommitteeState(
    /** Список выбранных экспертов */
    val selectedExperts: List<Expert> = Expert.getPredefinedExperts().take(3),
    /** Список всех доступных экспертов */
    val availableExperts: List<Expert> = Expert.getPredefinedExperts(),
    /** Текущие мнения экспертов (во время обсуждения) */
    val currentOpinions: List<ExpertOpinion> = emptyList(),
    /** Флаг выполнения */
    val isExecuting: Boolean = false,
    /** Ошибка */
    val error: String? = null,
    /** Раскрыта ли панель экспертов */
    val isPanelExpanded: Boolean = true
) {
    companion object {
        fun empty() = CommitteeState()
    }
}