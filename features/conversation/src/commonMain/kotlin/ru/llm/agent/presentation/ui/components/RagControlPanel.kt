package ru.llm.agent.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Панель управления RAG (Retrieval-Augmented Generation)
 * Позволяет включать/выключать RAG, добавлять знания и просматривать статистику
 */
@Composable
fun RagControlPanel(
    isRagEnabled: Boolean,
    indexedCount: Int,
    onToggleRag: (Boolean) -> Unit,
    onAddKnowledge: () -> Unit,
    onClearKnowledge: () -> Unit,
    enabled: Boolean = true
) {
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
            }
        }
    }
}