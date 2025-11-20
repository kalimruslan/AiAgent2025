package ru.llm.agent.presentation.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ru.llm.agent.presentation.model.SmartPromptTemplate
import ru.llm.agent.presentation.model.PromptVariablesContext
import ru.llm.agent.presentation.model.fillTemplate

/**
 * Умные промпты для быстрого взаимодействия с Trello с поддержкой переменных
 */
@Composable
public fun SmartPromptsBar(
    onPromptClick: (String) -> Unit,
    enabled: Boolean = true,
    boardId: String? = null,
    modifier: Modifier = Modifier
) {
    val context = PromptVariablesContext(boardId = boardId)
    val templates = SmartPromptTemplate.TRELLO_TEMPLATES

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "⚡ Быстрые действия",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Показываем индикатор, если boardId настроен
                if (boardId != null) {
                    Text(
                        text = "Board ID: ${boardId.take(8)}...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                templates.forEach { template ->
                    val canUse = !template.requiresBoardId || boardId != null

                    SmartPromptChip(
                        label = template.label,
                        icon = template.icon,
                        onClick = {
                            val filledPrompt = template.fillTemplate(context)
                            onPromptClick(filledPrompt)
                        },
                        enabled = enabled && canUse,
                        tooltip = template.description
                    )
                }
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
    enabled: Boolean,
    tooltip: String? = null
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = tooltip,
                modifier = Modifier.size(18.dp)
            )
        },
        enabled = enabled,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = if (enabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
            labelColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    )
}