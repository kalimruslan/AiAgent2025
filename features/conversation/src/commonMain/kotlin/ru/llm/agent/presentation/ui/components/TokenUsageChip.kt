package ru.llm.agent.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.llm.agent.model.SummarizationInfo

/**
 * Компактный чип для отображения использования токенов в TopBar
 * При клике показывает детальную информацию в dropdown
 */
@Composable
fun TokenUsageChip(
    usedTokens: Int,
    maxTokens: Int,
    requestTokens: Int?,
    summarizationInfo: SummarizationInfo?,
    isSummarizing: Boolean,
    modifier: Modifier = Modifier
) {
    var showDetails by remember { mutableStateOf(false) }

    // Логирование для отладки
    androidx.compose.runtime.LaunchedEffect(usedTokens, maxTokens) {
        println("TokenUsageChip recomposed: usedTokens=$usedTokens, maxTokens=$maxTokens")
    }

    val progress = if (maxTokens > 0) usedTokens.toFloat() / maxTokens.toFloat() else 0f

    val backgroundColor = when {
        progress < 0.5f -> MaterialTheme.colorScheme.primaryContainer
        progress < 0.8f -> Color(0xFFFFE0B2) // Светло-оранжевый
        else -> MaterialTheme.colorScheme.errorContainer
    }

    val textColor = when {
        progress < 0.5f -> MaterialTheme.colorScheme.onPrimaryContainer
        progress < 0.8f -> Color(0xFFE65100) // Темно-оранжевый
        else -> MaterialTheme.colorScheme.onErrorContainer
    }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier.clickable { showDetails = !showDetails },
            shape = RoundedCornerShape(12.dp),
            color = backgroundColor
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSummarizing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = textColor
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.DataUsage,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = textColor
                    )
                }
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // Dropdown с деталями
        DropdownMenu(
            expanded = showDetails,
            onDismissRequest = { showDetails = false }
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .widthIn(min = 250.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Заголовок
                Text(
                    text = "Использование токенов",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Divider()

                // Основная информация
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Использовано:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$usedTokens",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Максимум:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$maxTokens",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Прогресс:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }

                // Текущий запрос
                if (requestTokens != null && requestTokens > 0) {
                    Divider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Текущий запрос:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "~$requestTokens",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Индикатор суммаризации
                if (isSummarizing) {
                    Divider()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "⏳ Сжатие истории...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Информация о суммаризации
                if (!isSummarizing && summarizationInfo != null && summarizationInfo.hasSummarizedMessages) {
                    Divider()
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "📝 История сжата",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Сообщений: ${summarizationInfo.summarizedMessagesCount}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Сохранено: ~${summarizationInfo.savedTokens} токенов",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Предупреждение
                if (!isSummarizing && progress > 0.8f) {
                    Divider()
                    Text(
                        text = "⚠️ Токены заканчиваются",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}