package ru.llm.agent

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import ru.llm.agent.error.DomainError

/**
 * NetworkResult представляет собой запрос обновлениях данных,
 * который может происходить из нескольких источников.
 *
 * @param T тип данных при успешном выполнении
 */
public sealed interface NetworkResult<out T> {
    /**
     * Успешный результат с данными
     */
    public class Success<out T>(public val data: T) : NetworkResult<T>

    /**
     * Ошибка с типизированной информацией о причине
     */
    public class Error<out T>(public val error: DomainError) : NetworkResult<T> {
        /**
         * Конструктор для обратной совместимости со старым API
         * @deprecated Используйте конструктор с DomainError
         */
        @Deprecated("Используйте конструктор с DomainError", ReplaceWith("Error(DomainError.UnknownError(message ?: \"Неизвестная ошибка\"))"))
        public constructor(message: String?) : this(DomainError.UnknownError(message ?: "Неизвестная ошибка"))

        /**
         * Свойство для обратной совместимости
         * @deprecated Используйте error.toUserMessage()
         */
        @Deprecated("Используйте error.toUserMessage()", ReplaceWith("error.toUserMessage()"))
        public val message: String
            get() = error.toUserMessage()
    }

    /**
     * Состояние загрузки
     */
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
                NetworkResult.Success(transform(result.data))
            }
            is NetworkResult.Error -> {
                NetworkResult.Error(result.error)
            }
            is NetworkResult.Loading -> {
                NetworkResult.Loading()
            }
        }
    }

/**
 * Обработка конечного результата с типизированной ошибкой
 */
public fun <T> NetworkResult<T>.handleResult(
    onLoading: () -> Unit,
    onError: (DomainError) -> Unit,
    onSuccess: (data: T) -> Unit
) {
    when (this) {
        is NetworkResult.Loading -> onLoading()
        is NetworkResult.Error -> onError(error)
        is NetworkResult.Success -> onSuccess(data)
    }
}

/**
 * Обработка конечного результата (deprecated версия для обратной совместимости)
 * @deprecated Используйте версию с DomainError
 */
@Deprecated("Используйте версию с DomainError", ReplaceWith("handleResult(onLoading, { onError(it.toUserMessage()) }, onSuccess)"))
public fun <T> NetworkResult<T>.handleResultLegacy(
    onLoading: () -> Unit,
    onError: (String?) -> Unit,
    onSuccess: (data: T) -> Unit
) {
    when (this) {
        is NetworkResult.Loading -> onLoading()
        is NetworkResult.Error -> onError(error.toUserMessage())
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
 * Выполнение действия при ошибке с типизированной информацией
 */
public fun <T> NetworkResult<T>.doActionIfError(onError: (DomainError) -> Unit) {
    when (this) {
        is NetworkResult.Loading -> {}
        is NetworkResult.Error -> onError.invoke(error)
        is NetworkResult.Success -> {}
    }
}

/**
 * Выполнение действия при ошибке (deprecated версия для обратной совместимости)
 * @deprecated Используйте версию с DomainError
 */
@Deprecated("Используйте версию с DomainError", ReplaceWith("doActionIfError { onError() }"))
public fun <T> NetworkResult<T>.doActionIfErrorLegacy(onError: () -> Unit) {
    when (this) {
        is NetworkResult.Loading -> {}
        is NetworkResult.Error -> onError.invoke()
        is NetworkResult.Success -> {}
    }
}

/**
 * Выполнение действия при запросе, остальные действия не выполняются
 */
public fun <T> NetworkResult<T>.doActionIfLoading(onLoading: () -> Unit) {
    when (this) {
        is NetworkResult.Loading -> onLoading.invoke()
        is NetworkResult.Error -> {}
        is NetworkResult.Success -> {}
    }
}

/**
 * Создаем цепочку вызовов, например после успешного запроса делаем еще запрос на основе предыдущего
 */
@OptIn(ExperimentalCoroutinesApi::class)
public inline fun <T, R> Flow<NetworkResult<T>>.andThen(
    crossinline nextAction: suspend (T) -> Flow<NetworkResult<R>>,
): Flow<NetworkResult<R>> = this.flatMapLatest { result ->
    when (result) {
        is NetworkResult.Loading -> flowOf(NetworkResult.Loading())
        is NetworkResult.Error -> flowOf(NetworkResult.Error(result.error))
        is NetworkResult.Success -> nextAction(result.data)
    }
}

/**
 * Получить DomainError из NetworkResult.Error или null
 */
public fun <T> NetworkResult<T>.getErrorOrNull(): DomainError? = when (this) {
    is NetworkResult.Error -> error
    else -> null
}

/**
 * Получить данные из NetworkResult.Success или null
 */
public fun <T> NetworkResult<T>.getDataOrNull(): T? = when (this) {
    is NetworkResult.Success -> data
    else -> null
}

/**
 * Преобразовать NetworkResult в Result<T>
 */
public fun <T> NetworkResult<T>.toResult(): Result<T> = when (this) {
    is NetworkResult.Success -> Result.success(data)
    is NetworkResult.Error -> Result.failure(Exception(error.toUserMessage()))
    is NetworkResult.Loading -> Result.failure(IllegalStateException("Операция еще выполняется"))
}