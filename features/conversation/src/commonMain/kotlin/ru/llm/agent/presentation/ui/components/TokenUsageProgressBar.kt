package ru.llm.agent.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.llm.agent.model.SummarizationInfo

/**
 * ProgressBar для отображения использования токенов
 */
@Composable
public fun TokenUsageProgressBar(
    usedTokens: Int,
    maxTokens: Int,
    requestTokens: Int?,
    summarizationInfo: SummarizationInfo?,
    isSummarizing: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = if (maxTokens > 0) usedTokens.toFloat() / maxTokens.toFloat() else 0f
    val progressClamped = progress.coerceIn(0f, 1f)

    // Определяем цвет в зависимости от использования
    val progressColor = when {
        progressClamped < 0.5f -> MaterialTheme.colorScheme.primary
        progressClamped < 0.8f -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Использование токенов",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$usedTokens / $maxTokens",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            // Отображаем токены текущего запроса, если они подсчитаны
            if (requestTokens != null && requestTokens > 0) {
                Text(
                    text = "Текущий запрос: ~$requestTokens токенов",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            LinearProgressIndicator(
                progress = { progressClamped },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            // Индикатор процесса суммаризации
            if (isSummarizing) {
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
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Информация о суммаризации
            if (!isSummarizing && summarizationInfo != null && summarizationInfo.hasSummarizedMessages) {
                Text(
                    text = "📝 История сжата: ${summarizationInfo.summarizedMessagesCount} сообщений (сохранено ~${summarizationInfo.savedTokens} токенов)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CAF50), // Зеленый цвет
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Предупреждение, если токены заканчиваются
            if (!isSummarizing && progressClamped > 0.8f) {
                Text(
                    text = "⚠️ Токены заканчиваются",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp
                )
            }
        }
    }
}