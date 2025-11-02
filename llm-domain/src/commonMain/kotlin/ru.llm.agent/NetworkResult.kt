package ru.llm.agent

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.logging.Logger

/**
 * NetworkResult представляет собой запрос обновлениях данных,
 * который может происходить из нескольких источников
 */
public sealed interface NetworkResult<out T> {
    public class Success<out T>(public val data: T) : NetworkResult<T>

    public class Error<out T>(public val message: String?) : NetworkResult<T>

    public class Loading<out T> : NetworkResult<T>
}

/**
 * Трансформирует текущий NetworkResult в выходной NetworkResult
 * @param transform - функция трансформации данных, например маппинг в домейн модельки
 */
public inline fun <T, R> Flow<NetworkResult<T>>.mapNetworkResult(crossinline transform: suspend (T) -> R): Flow<NetworkResult<R>> =
    map { result ->
        when (result) {
            is NetworkResult.Success -> {
                Logger.getLogger("TOKENIZER").info("Success - ${result.data}")
                NetworkResult.Success(transform(result.data))
            }
            is NetworkResult.Error -> {
                Logger.getLogger("McpClient").info("Error - ${result.message}")
                NetworkResult.Error(result.message)
            }
            is NetworkResult.Loading -> {
                Logger.getLogger("TOKENIZER").info("Loading...")
                NetworkResult.Loading()
            }
        }
    }

/**
 * Обработка конечногно результата
 */
public fun <T> NetworkResult<T>.handleResult(onLoading: () -> Unit, onError: (String?) -> Unit, onSuccess: (data: T) -> Unit) {
    when (this) {
        is NetworkResult.Loading -> onLoading()
        is NetworkResult.Error -> onError(message)
        is NetworkResult.Success -> onSuccess(data)
    }
}

/**
 * Выполнение действия при успешном ответе, остальные действия не выполняются
 */
public fun <T> NetworkResult<T>.doActionIfSuccess(onSuccess: (data: T) -> Unit) {
    when (this) {
        is NetworkResult.Loading -> {}
        is NetworkResult.Error -> {}
        is NetworkResult.Success -> onSuccess(data)
    }
}

/**
 * Выполнение действия при ошибке, остальные действия не выполняются
 */
public fun <T> NetworkResult<T>.doActionIfError(onError: () -> Unit) {
    when (this) {
        is NetworkResult.Loading -> {}
        is NetworkResult.Error -> onError.invoke()
        is NetworkResult.Success -> {}
    }
}

/**
 * Выполнение действия при запрросе, остальные действия не выполняются
 */
public fun <T> NetworkResult<T>.doActionIfLoading(onLoading: () -> Unit) {
    when (this) {
        is NetworkResult.Loading -> onLoading.invoke()
        is NetworkResult.Error -> {}
        is NetworkResult.Success -> {}
    }
}

/**
 * Создаем цепочку вызовов, наприммер послу успешного запроса делаем еще запрос на основе предыдущего
 */
@OptIn(ExperimentalCoroutinesApi::class)
public inline fun <T, R> Flow<NetworkResult<T>>.andThen(
    crossinline nextAction: suspend (T) -> Flow<NetworkResult<R>>,
): Flow<NetworkResult<R>> = this.flatMapLatest { result ->
    when (result) {
        is NetworkResult.Loading -> flowOf(NetworkResult.Loading())
        is NetworkResult.Error -> flowOf(NetworkResult.Error(result.message))
        is NetworkResult.Success -> nextAction(result.data)
    }
}