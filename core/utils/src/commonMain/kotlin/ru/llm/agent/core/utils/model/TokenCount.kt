package ru.llm.agent.core.utils.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Value class для типобезопасного представления количества токенов.
 * Гарантирует, что количество токенов не будет отрицательным.
 *
 * @property value Количество токенов
 * @throws IllegalArgumentException если value отрицательное
 */
@Serializable
@JvmInline
public value class TokenCount(public val value: Int) {
    init {
        require(value >= 0) { "TokenCount не может быть отрицательным, получено: $value" }
    }

    /**
     * Складывает два количества токенов
     */
    public operator fun plus(other: TokenCount): TokenCount = TokenCount(value + other.value)

    /**
     * Вычитает одно количество токенов из другого
     * @throws IllegalArgumentException если результат отрицательный
     */
    public operator fun minus(other: TokenCount): TokenCount = TokenCount(value - other.value)

    /**
     * Сравнивает с другим количеством токенов
     */
    public operator fun compareTo(other: TokenCount): Int = value.compareTo(other.value)

    override fun toString(): String = value.toString()

    public companion object {
        /**
         * Нулевое количество токенов
         */
        public val ZERO: TokenCount = TokenCount(0)

        /**
         * Создаёт TokenCount из Int, возвращая null если значение невалидно
         */
        public fun fromIntOrNull(value: Int?): TokenCount? {
            return if (value != null && value >= 0) {
                TokenCount(value)
            } else {
                null
            }
        }

        /**
         * Суммирует коллекцию TokenCount
         */
        public fun sum(counts: Iterable<TokenCount>): TokenCount {
            return TokenCount(counts.sumOf { it.value })
        }
    }
}