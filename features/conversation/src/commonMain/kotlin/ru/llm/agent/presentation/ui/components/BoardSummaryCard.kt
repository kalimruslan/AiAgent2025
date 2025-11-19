package ru.llm.agent.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.llm.agent.presentation.state.ConversationUIState
import ru.llm.agent.presentation.ui.formatTimestamp

/**
 * Карточка с саммари доски Trello
 * Отображается в верхней части экрана и может быть свёрнута/развёрнута
 */
@Composable
internal fun BoardSummaryCard(
    boardSummary: ConversationUIState.BoardSummary,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Заголовок с кнопкой сворачивания
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Иконка Trello
                    Text(
                        text = "📋",
                        fontSize = 20.sp
                    )
                    Column {
                        Text(
                            text = "Статус доски Trello",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Обновлено: ${formatTimestamp(boardSummary.timestamp)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }

                // Кнопка сворачивания
                IconButton(
                    onClick = { isExpanded = !isExpanded }
                ) {
                    Icon(
                        imageVector = if (isExpanded)
                            Icons.Default.KeyboardArrowUp
                        else
                            Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            // Анимированный контент
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f)
                    )

                    // Индикатор загрузки или текст саммари
                    if (boardSummary.isLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = boardSummary.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        // Текст саммари
                        Text(
                            text = boardSummary.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        // Анализ ассистента (раскрывающийся блок)
                        if (boardSummary.assistantAnalysis != null || boardSummary.isAnalysisLoading) {
                            Spacer(modifier = Modifier.height(12.dp))

                            AssistantAnalysisSection(
                                analysis = boardSummary.assistantAnalysis,
                                isLoading = boardSummary.isAnalysisLoading
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Секция с анализом ассистента (раскрывающийся блок)
 */
@Composable
private fun AssistantAnalysisSection(
    analysis: String?,
    isLoading: Boolean
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column {
        // Кнопка для раскрытия/сворачивания анализа
        TextButton(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier.padding(0.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "🤖",
                    fontSize = 14.sp
                )
                Text(
                    text = if (isExpanded) "Скрыть анализ ассистента" else "Показать анализ ассистента",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
                Icon(
                    imageVector = if (isExpanded)
                        Icons.Default.KeyboardArrowUp
                    else
                        Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        // Анимированный контент анализа
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    if (isLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Анализирую изменения...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp
                            )
                        }
                    } else if (analysis != null) {
                        Text(
                            text = analysis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}