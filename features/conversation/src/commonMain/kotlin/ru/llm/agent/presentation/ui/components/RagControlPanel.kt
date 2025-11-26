package ru.llm.agent.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Настройки RAG
 */
data class RagSettings(
    val threshold: Double = 0.3,
    val topK: Int = 3,
    val useMmr: Boolean = true,
    val mmrLambda: Double = 0.5
)

/**
 * Панель управления RAG (Retrieval-Augmented Generation)
 * Позволяет включать/выключать RAG, добавлять знания и просматривать статистику
 */
@Composable
fun RagControlPanel(
    isRagEnabled: Boolean,
    indexedCount: Int,
    settings: RagSettings = RagSettings(),
    onToggleRag: (Boolean) -> Unit,
    onAddKnowledge: () -> Unit,
    onClearKnowledge: () -> Unit,
    onThresholdChange: (Double) -> Unit = {},
    onTopKChange: (Int) -> Unit = {},
    onToggleMmr: (Boolean) -> Unit = {},
    onMmrLambdaChange: (Double) -> Unit = {},
    enabled: Boolean = true
) {
    var showSettings by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRagEnabled) {
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
                        text = if (isRagEnabled) {
                            "Используется база знаний ($indexedCount фрагментов)"
                        } else {
                            "База знаний отключена"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = isRagEnabled,
                    onCheckedChange = onToggleRag,
                    enabled = enabled
                )
            }

            // Кнопки управления (показываем только когда RAG включен)
            if (isRagEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Кнопка добавления знаний
                    OutlinedButton(
                        onClick = onAddKnowledge,
                        modifier = Modifier.weight(1f),
                        enabled = enabled
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
                        onClick = { showSettings = !showSettings },
                        enabled = enabled
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Настройки",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Кнопка очистки (показываем только если есть документы)
                    if (indexedCount > 0) {
                        OutlinedButton(
                            onClick = onClearKnowledge,
                            enabled = enabled,
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
                AnimatedVisibility(visible = showSettings) {
                    RagSettingsPanel(
                        settings = settings,
                        onThresholdChange = onThresholdChange,
                        onTopKChange = onTopKChange,
                        onToggleMmr = onToggleMmr,
                        onMmrLambdaChange = onMmrLambdaChange,
                        enabled = enabled
                    )
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
    settings: RagSettings,
    onThresholdChange: (Double) -> Unit,
    onTopKChange: (Int) -> Unit,
    onToggleMmr: (Boolean) -> Unit,
    onMmrLambdaChange: (Double) -> Unit,
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
                    text = String.format("%.2f", settings.threshold),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = settings.threshold.toFloat(),
                onValueChange = { onThresholdChange(it.toDouble()) },
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
                    text = "${settings.topK}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = settings.topK.toFloat(),
                onValueChange = { onTopKChange(it.roundToInt()) },
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
                checked = settings.useMmr,
                onCheckedChange = onToggleMmr,
                enabled = enabled
            )
        }

        // Lambda параметр MMR (показываем только если MMR включен)
        AnimatedVisibility(visible = settings.useMmr) {
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
                        text = String.format("%.2f", settings.mmrLambda),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = settings.mmrLambda.toFloat(),
                    onValueChange = { onMmrLambdaChange(it.toDouble()) },
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