package ru.llm.agent.utils

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.SerializationException
import ru.llm.agent.NetworkResult
import ru.llm.agent.data.LlmApiError

/**
 * Общий Обработчик запросов, который все ответы превращает во Flow
 * @param execute - функция для выпаолнения запросов
 */
public inline fun <reified T> handleApi(crossinline execute: suspend () -> HttpResponse): Flow<NetworkResult<T>> =
    flow {
        emit(NetworkResult.Loading())
        try {
            val result = execute.invoke()
            when (result.status.value) {
                in 200..299 -> emit(
                    NetworkResult.Success(
                        data = result.body<T>()
                    )
                )

                else -> {
                    val errorBody = try {
                        result.body<LlmApiError>()
                    } catch (_: Exception) {
                        null
                    } catch (_: SerializationException) {
                        null
                    }

                    emit(
                        NetworkResult.Error(
                            message = errorBody?.message ?: "unknown error"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            emit(
                NetworkResult.Error(
                    message = e.message
                )
            )
        }
    }.flowOn(Dispatchers.IO)