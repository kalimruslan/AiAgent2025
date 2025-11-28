package ru.llm.agent.rag.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import ru.llm.agent.rag.model.RagOperationStatus
import ru.llm.agent.rag.presentation.state.RagEvent
import ru.llm.agent.rag.presentation.state.RagState
import ru.llm.agent.rag.presentation.viewmodel.RagViewModel

/**
 * Панель управления RAG (Retrieval-Augmented Generation)
 * Позволяет включать/выключать RAG, добавлять знания и просматривать статистику
 */
@Composable
fun RagControlPanel(
    viewModel: RagViewModel,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val state by viewModel.state.collectAsState()

    RagControlPanelContent(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
        enabled = enabled
    )
}

/**
 * Содержимое панели управления RAG (для тестирования без ViewModel)
 */
@Composable
fun RagControlPanelContent(
    state: RagState,
    onEvent: (RagEvent) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (state.isEnabled) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Главный переключатель
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "RAG (База знаний)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = if (state.isEnabled) {
                            "Используется база знаний (${state.indexedCount} фрагментов)"
                        } else {
                            "База знаний отключена"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Индикатор загрузки
                if (state.isLoadingStats || state.isIndexing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }

                Switch(
                    checked = state.isEnabled,
                    onCheckedChange = { onEvent(RagEvent.ToggleEnabled(it)) },
                    enabled = enabled
                )
            }

            // Текущая операция
            state.currentOperation?.let { operation ->
                RagOperationStatusIndicator(operation = operation)
            }

            // Ошибка
            state.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { onEvent(RagEvent.ClearError) }) {
                            Text("OK")
                        }
                    }
                }
            }

            // Кнопки управления (показываем только когда RAG включен)
            if (state.isEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Кнопка добавления знаний
                    OutlinedButton(
                        onClick = { onEvent(RagEvent.ShowKnowledgeBaseDialog) },
                        modifier = Modifier.weight(1f),
                        enabled = enabled && !state.isIndexing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Добавить знания",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Добавить знания")
                    }

                    // Кнопка настроек
                    OutlinedButton(
                        onClick = { onEvent(RagEvent.ToggleSettings) },
                        enabled = enabled
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Настройки",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Кнопка очистки (показываем только если есть документы)
                    if (state.indexedCount > 0) {
                        OutlinedButton(
                            onClick = { onEvent(RagEvent.ClearKnowledgeBase) },
                            enabled = enabled && !state.isIndexing,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Очистить",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Панель настроек (разворачивается по кнопке)
                AnimatedVisibility(visible = state.isSettingsExpanded) {
                    RagSettingsPanel(
                        state = state,
                        onEvent = onEvent,
                        enabled = enabled
                    )
                }

                // История операций
                if (state.operationHistory.isNotEmpty()) {
                    RagOperationHistoryPanel(
                        history = state.operationHistory,
                        onClearHistory = { onEvent(RagEvent.ClearHistory) }
                    )
                }
            }
        }
    }
}

/**
 * Индикатор статуса текущей операции
 */
@Composable
private fun RagOperationStatusIndicator(operation: RagOperationStatus) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                operation.error != null -> MaterialTheme.colorScheme.errorContainer
                operation.isExecuting -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (operation.isExecuting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else if (operation.error != null) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = operation.description,
                    style = MaterialTheme.typography.bodySmall
                )
                operation.result?.let { result ->
                    Text(
                        text = result,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                operation.error?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Панель истории операций
 */
@Composable
private fun RagOperationHistoryPanel(
    history: List<RagOperationStatus>,
    onClearHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "История операций",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onClearHistory) {
                Text("Очистить", style = MaterialTheme.typography.labelSmall)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 120.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(history.reversed()) { operation ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (operation.error != null) {
                                Icons.Default.Error
                            } else {
                                Icons.Default.CheckCircle
                            },
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (operation.error != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = operation.result ?: operation.error ?: operation.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * Панель расширенных настроек RAG
 */
@Composable
private fun RagSettingsPanel(
    state: RagState,
    onEvent: (RagEvent) -> Unit,
    enabled: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HorizontalDivider()

        Text(
            text = "Настройки поиска",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // Порог схожести (threshold)
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Порог релевантности",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = String.format("%.2f", state.threshold),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = state.threshold.toFloat(),
                onValueChange = { onEvent(RagEvent.SetThreshold(it.toDouble())) },
                valueRange = 0f..1f,
                steps = 19, // 0.05 шаг
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Чем выше значение, тем строже фильтрация нерелевантных документов",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Количество документов (topK)
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Количество документов",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${state.topK}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = state.topK.toFloat(),
                onValueChange = { onEvent(RagEvent.SetTopK(it.roundToInt())) },
                valueRange = 1f..10f,
                steps = 8, // Целые числа от 1 до 10
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            )
        }

        HorizontalDivider()

        // MMR настройки
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MMR (разнообразие)",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Избегает дублирования похожих фрагментов",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = state.useMmr,
                onCheckedChange = { onEvent(RagEvent.ToggleMmr(it)) },
                enabled = enabled
            )
        }

        // Lambda параметр MMR (показываем только если MMR включен)
        AnimatedVisibility(visible = state.useMmr) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Баланс релевантность/разнообразие",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = String.format("%.2f", state.mmrLambda),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = state.mmrLambda.toFloat(),
                    onValueChange = { onEvent(RagEvent.SetMmrLambda(it.toDouble())) },
                    valueRange = 0f..1f,
                    steps = 9, // 0.1 шаг
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Разнообразие",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Релевантность",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
