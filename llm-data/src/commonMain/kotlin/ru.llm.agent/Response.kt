package ru.llm.agent

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.SerializationException
import ru.llm.agent.data.AdventApiError

/**
 * Represents the result of an API operation.
 */
public sealed class Response<out T> {
    /**
     * Represents a successful API operation with data.
     *
     * @param data The data returned from the API.
     */
    public data class Success<T>(
        val httpCode: Int,
        val data: T,
    ) : Response<T>()

    /**
     * Represents a failed API operation.
     *
     * @param code The error code.
     */
    public data class NetworkError(
        val httpCode: Int,
        val error: AdventApiError? = null,
    ) : Response<Nothing>()

    /**
     * Represents a failed API operation.
     *
     * @param code The error code.
     */
    public data class Error(
        val throwable: Throwable,
    ) : Response<Nothing>()
}

@Suppress("MagicNumber", "TooGenericExceptionCaught")
internal suspend inline fun <reified T> HttpResponse.asResponse(): Response<T> {
    return when (status.value) {
        in 200..299 -> Response.Success(
            httpCode = status.value,
            data = body<T>(),
        )

        else -> {
            val errorBody = try {
                body<AdventApiError>()
            } catch (_: Exception) {
                null
            } catch (_: SerializationException) {
                null
            }

            Response.NetworkError(
                httpCode = status.value,
                error = errorBody,
            )
        }
    }
}
