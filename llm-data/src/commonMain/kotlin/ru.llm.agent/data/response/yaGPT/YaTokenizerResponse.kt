package ru.llm.agent.data.response.yaGPT

import kotlinx.serialization.Serializable

/**
 * {
 *     "tokens": [
 *         {
 *             "id": "string",    // внутренний идентификатор токена (int64)
 *             "text": "string",  // текстовое представление токена
 *             "special": "boolean"  // флаг, указывающий на специальный токен
 *         }
 *     ],
 *     "modelVersion": "string"  // версия модели (обновляется при новых релизах)
 * }
 */
@Serializable
public data class YaTokenizerResponse (
    internal val tokens: List<YaToken>? = null,
    val modelVersion: String? = null
)

@Serializable
public data class YaToken (
    val id: String? = null,
    val text: String? = null,
    val special: Boolean? = null
)
