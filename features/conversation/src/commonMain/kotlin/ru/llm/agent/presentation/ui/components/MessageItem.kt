package ru.llm.agent.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ru.llm.agent.model.ExpertOpinion
import ru.llm.agent.model.Role
import ru.llm.agent.model.conversation.ConversationMessage
import kotlin.time.Instant

/**
 * Компонент для отображения одного сообщения в диалоге
 */
@Composable
public fun MessageItem(message: ConversationMessage) {
    val isUser = message.role == Role.USER
    var showOriginalJson by remember { mutableStateOf(false) }
    var showMetadata by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        if(!isUser) {
            Text("Model: ${message.model}")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            Card(
                modifier = Modifier.widthIn(max = 400.dp),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.secondaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isUser)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    // Показать метаданные (токены и время) если есть
                    if (!isUser && hasMetadata(message)) {
                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { showMetadata = !showMetadata },
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Text(
                                text = if (showMetadata) "Скрыть статистику" else "Показать статистику",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        if (showMetadata) {
                            Spacer(modifier = Modifier.height(4.dp))
                            MetadataCard(message)
                        }
                    }

                    // Show original JSON response if available (for assistant messages)
                    if (!isUser && message.originalResponse != null) {
                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { showOriginalJson = !showOriginalJson },
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Text(
                                text = if (showOriginalJson) "Скрыть JSON" else "Показать оригинальный JSON",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        if (showOriginalJson) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Text(
                                    text = message.originalResponse.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(8.dp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser)
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Отображаем мнения экспертов (если есть)
        if (isUser && message.expertOpinions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                message.expertOpinions.forEach { opinion ->
                    ExpertOpinionCard(opinion)
                }
            }
        }
    }
}

/**
 * Карточка с мнением эксперта
 */
@Composable
public fun ExpertOpinionCard(opinion: ExpertOpinion) {
    var showOriginalJson by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Spacer(modifier = Modifier.width(24.dp)) // Отступ слева для визуального отличия
        Card(
            modifier = Modifier.widthIn(max = 380.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Заголовок с иконкой и именем эксперта
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = opinion.expertIcon,
                        fontSize = 16.sp
                    )
                    Text(
                        text = opinion.expertName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Текст мнения
                Text(
                    text = opinion.opinion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontSize = 14.sp
                )

                // Показать оригинальный JSON (если есть)
                if (opinion.originalResponse != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = { showOriginalJson = !showOriginalJson },
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Text(
                            text = if (showOriginalJson) "Скрыть JSON" else "Показать JSON",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }

                    if (showOriginalJson) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(
                                text = opinion.originalResponse.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(6.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Временная метка
                Text(
                    text = formatTimestamp(opinion.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Карточка с метаданными (токены и время)
 */
@Composable
public fun MetadataCard(message: ConversationMessage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "📊 Статистика ответа",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            if (message.inputTokens != null) {
                MetadataRow(
                    label = "Входные токены:",
                    value = "${message.inputTokens}"
                )
            }

            if (message.completionTokens != null) {
                MetadataRow(
                    label = "Токены ответа:",
                    value = "${message.completionTokens}"
                )
            }

            if (message.totalTokens != null) {
                MetadataRow(
                    label = "Всего токенов:",
                    value = "${message.totalTokens}",
                    isBold = true
                )
            }

            message.responseTimeMs?.let { responseTime ->
                MetadataRow(
                    label = "Время ответа:",
                    value = formatResponseTime(responseTime)
                )
            }
        }
    }
}

/**
 * Строка с меткой и значением
 */
@Composable
public fun MetadataRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Проверка наличия метаданных у сообщения
 */
public fun hasMetadata(message: ConversationMessage): Boolean {
    return message.totalTokens != null ||
           message.inputTokens != null ||
           message.completionTokens != null ||
           message.responseTimeMs != null
}

/**
 * Форматирование временной метки
 */
public fun formatTimestamp(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dateTime.hour.toString().padStart(2, '0')}:${
        dateTime.minute.toString().padStart(2, '0')
    }"
}

/**
 * Форматирование времени ответа
 */
public fun formatResponseTime(milliseconds: Long): String {
    return when {
        milliseconds < 1000 -> "${milliseconds} мс"
        milliseconds < 60000 -> String.format("%.1f сек", milliseconds / 1000.0)
        else -> {
            val minutes = milliseconds / 60000
            val seconds = (milliseconds % 60000) / 1000
            "${minutes} мин ${seconds} сек"
        }
    }
}