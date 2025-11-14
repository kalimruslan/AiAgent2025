package ru.llm.agent.core.utils.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Value class для типобезопасного представления идентификатора диалога.
 * Гарантирует, что идентификатор не будет пустым.
 *
 * @property value Строковое значение идентификатора
 * @throws IllegalArgumentException если value пустая строка или содержит только пробелы
 */
@Serializable
@JvmInline
public value class ConversationId(public val value: String) {
    init {
        require(value.isNotBlank()) { "ConversationId не может быть пустым" }
    }

    override fun toString(): String = value

    public companion object {
        /**
         * Создаёт ConversationId из String, возвращая null если строка невалидна
         */
        public fun fromStringOrNull(value: String?): ConversationId? {
            return if (value?.isNotBlank() == true) {
                ConversationId(value)
            } else {
                null
            }
        }
    }
}