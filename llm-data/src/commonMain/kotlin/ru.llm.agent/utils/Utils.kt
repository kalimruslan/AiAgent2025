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
import ru.llm.agent.error.DomainError

/**
 * Общий обработчик запросов, который все ответы превращает во Flow
 * @param execute - функция для выполнения запросов
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
                            error = DomainError.NetworkError(
                                code = result.status.value,
                                message = errorBody?.message ?: "Неизвестная ошибка сети",
                                exception = null
                            )
                        )
                    )
                }
            }
        } catch (e: SerializationException) {
            emit(
                NetworkResult.Error(
                    error = DomainError.ParseError(
                        rawData = e.message ?: "",
                        message = "Ошибка парсинга ответа от сервера",
                        exception = e
                    )
                )
            )
        } catch (e: Exception) {
            emit(
                NetworkResult.Error(
                    error = DomainError.NetworkError(
                        code = null,
                        message = e.message ?: "Ошибка при выполнении запроса",
                        exception = e
                    )
                )
            )
        }
    }.flowOn(Dispatchers.IO)