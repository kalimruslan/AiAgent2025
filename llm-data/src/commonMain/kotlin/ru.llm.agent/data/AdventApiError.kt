package ru.llm.agent.data

import kotlinx.serialization.Serializable

@Serializable
public data class AdventApiError(
    public val code: Int,
    public val message: String? = null,
    public val errors: List<Error>? = null,
) {
    @Serializable
    public class Error internal constructor(
        public val code: Int,
        public val status: Int,
        public val title: String? = null,
        public val detail: String? = null,
    )
}