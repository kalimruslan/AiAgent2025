package ru.llm.agent.mcp.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.llm.agent.mcp.presentation.state.McpEvent
import ru.llm.agent.mcp.presentation.viewmodel.McpViewModel
import ru.llm.agent.mcp.prompts.TrelloPrompts

/**
 * Панель управления MCP инструментами
 *
 * Основной UI компонент модуля MCP, который можно встраивать в другие экраны
 *
 * @param viewModel ViewModel для управления состоянием MCP
 * @param onToolExecute Callback для выполнения инструмента через LLM. Принимает (toolName, description)
 * @param trelloBoardId Текущий ID доски Trello (null если не задан)
 * @param onTrelloBoardIdChange Callback для изменения ID доски Trello
 * @param modifier Модификатор для UI
 */
@Composable
fun McpToolsPanel(
    viewModel: McpViewModel,
    modifier: Modifier = Modifier,
    onToolExecute: ((String, String) -> Unit)? = null,
    trelloBoardId: String? = null,
    onTrelloBoardIdChange: ((String?) -> Unit)? = null
) {
    val state by viewModel.state.collectAsState()

    // Проверяем, есть ли Trello инструменты
    val hasTrelloTools = remember(state.availableTools) {
        state.availableTools.any { TrelloPrompts.isTrelloTool(it.name) }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Заголовок с переключателем
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "MCP Инструменты",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Кнопка обновления
                    IconButton(
                        onClick = { viewModel.onEvent(McpEvent.LoadTools) },
                        enabled = !state.isLoadingTools
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Обновить инструменты"
                        )
                    }

                    // Переключатель включения/выключения
                    Switch(
                        checked = state.isEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.onEvent(McpEvent.ToggleEnabled(enabled))
                        }
                    )

                    // Кнопка сворачивания/разворачивания
                    IconButton(
                        onClick = { viewModel.onEvent(McpEvent.TogglePanel) }
                    ) {
                        Icon(
                            imageVector = if (state.isPanelExpanded) {
                                Icons.Default.ExpandLess
                            } else {
                                Icons.Default.ExpandMore
                            },
                            contentDescription = if (state.isPanelExpanded) {
                                "Свернуть"
                            } else {
                                "Развернуть"
                            }
                        )
                    }
                }
            }

            // Индикатор загрузки
            if (state.isLoadingTools) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Ошибка
            state.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }

            // Текущее выполнение
            state.currentExecution?.let { execution ->
                McpToolExecutionStatusIndicator(
                    status = execution,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Контент панели (сворачиваемый)
            AnimatedVisibility(
                visible = state.isPanelExpanded && !state.isLoadingTools,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.isEnabled) {
                        if (state.availableTools.isEmpty()) {
                            Text(
                                text = "Нет доступных инструментов",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp)
                            )
                        } else {
                            Text(
                                text = "Доступно инструментов: ${state.availableTools.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Поле Trello Board ID (показываем только если есть Trello инструменты)
                            if (hasTrelloTools && onTrelloBoardIdChange != null) {
                                TrelloBoardIdField(
                                    boardId = trelloBoardId,
                                    onBoardIdChange = onTrelloBoardIdChange
                                )
                            }

                            // Список инструментов
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 400.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(state.availableTools) { tool ->
                                    McpToolCard(
                                        tool = tool,
                                        onExecute = { toolName ->
                                            // Если передан callback, используем его для выполнения через LLM
                                            if (onToolExecute != null) {
                                                onToolExecute(toolName, tool.description)
                                            } else {
                                                // Fallback: старый способ через McpEvent (для совместимости)
                                                viewModel.onEvent(
                                                    McpEvent.ExecuteTool(
                                                        toolName = toolName,
                                                        arguments = emptyMap()
                                                    )
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "MCP инструменты отключены. Включите переключатель для использования.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    // История выполнений (если есть)
                    if (state.executionHistory.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "История выполнений",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            TextButton(
                                onClick = { viewModel.onEvent(McpEvent.ClearHistory) }
                            ) {
                                Text("Очистить")
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(state.executionHistory.takeLast(5).reversed()) { execution ->
                                McpToolExecutionStatusIndicator(
                                    status = execution,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Поле ввода ID доски Trello
 */
@Composable
private fun TrelloBoardIdField(
    boardId: String?,
    onBoardIdChange: (String?) -> Unit
) {
    var textValue by remember(boardId) { mutableStateOf(boardId ?: "") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Trello настройки",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        OutlinedTextField(
            value = textValue,
            onValueChange = { newValue ->
                textValue = newValue
                // Сохраняем null если поле пустое, иначе значение
                onBoardIdChange(newValue.ifBlank { null })
            },
            label = { Text("ID доски Trello") },
            placeholder = { Text("Например: abc123def456") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            supportingText = {
                Text(
                    text = if (textValue.isNotBlank()) {
                        "Доска: $textValue"
                    } else {
                        "Укажите ID для работы с конкретной доской"
                    },
                    style = MaterialTheme.typography.labelSmall
                )
            }
        )
    }
}