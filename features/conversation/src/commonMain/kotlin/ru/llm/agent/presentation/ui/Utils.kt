package ru.llm.agent.presentation.ui

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Форматирование временной метки в удобочитаемый формат (ЧЧ:ММ)
 */
fun formatTimestamp(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dateTime.hour.toString().padStart(2, '0')}:${
        dateTime.minute.toString().padStart(2, '0')
    }"
}

/**
 * Форматирование времени ответа в удобочитаемый формат
 */
fun formatResponseTime(milliseconds: Long): String {
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