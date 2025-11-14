package ru.llm.agent.core.utils.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Value class для типобезопасного представления идентификатора сообщения.
 * Гарантирует, что идентификатор будет положительным числом.
 *
 * @property value Числовое значение идентификатора
 * @throws IllegalArgumentException если value меньше или равно нулю
 */
@Serializable
@JvmInline
public value class MessageId(public val value: Long) {
    init {
        require(value > 0) { "MessageId должен быть положительным числом, получено: $value" }
    }

    override fun toString(): String = value.toString()

    public companion object {
        /**
         * Создаёт MessageId из Long, возвращая null если значение невалидно
         */
        public fun fromLongOrNull(value: Long?): MessageId? {
            return if (value != null && value > 0) {
                MessageId(value)
            } else {
                null
            }
        }

        /**
         * Минимально возможное значение MessageId
         */
        public val MIN: MessageId = MessageId(1)
    }
}