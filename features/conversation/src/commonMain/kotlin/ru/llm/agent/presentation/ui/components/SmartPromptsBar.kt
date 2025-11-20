package ru.llm.agent.presentation.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Умные промпты для быстрого взаимодействия с Trello
 */
@Composable
public fun SmartPromptsBar(
    onPromptClick: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "⚡ Быстрые действия",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmartPromptChip(
                    label = "Что на сегодня?",
                    icon = Icons.Default.DateRange,
                    onClick = {
                        onPromptClick("Покажи все задачи на сегодня из Trello")
                    },
                    enabled = enabled
                )

                SmartPromptChip(
                    label = "Что просрочено?",
                    icon = Icons.Default.Warning,
                    onClick = {
                        onPromptClick("Покажи все просроченные задачи из Trello")
                    },
                    enabled = enabled
                )

                SmartPromptChip(
                    label = "Создать задачу",
                    icon = Icons.Default.Add,
                    onClick = {
                        onPromptClick("Создай быструю задачу в Trello")
                    },
                    enabled = enabled
                )

                SmartPromptChip(
                    label = "Статистика доски",
                    icon = Icons.Default.Info,
                    onClick = {
                        onPromptClick("Покажи статистику по моей Trello доске")
                    },
                    enabled = enabled
                )

                SmartPromptChip(
                    label = "Поиск задач",
                    icon = Icons.Default.Search,
                    onClick = {
                        onPromptClick("Найди задачи в Trello по ключевому слову")
                    },
                    enabled = enabled
                )
            }
        }
    }
}

/**
 * Чип для одного умного промпта
 */
@Composable
private fun SmartPromptChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        enabled = enabled,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface
        )
    )
}