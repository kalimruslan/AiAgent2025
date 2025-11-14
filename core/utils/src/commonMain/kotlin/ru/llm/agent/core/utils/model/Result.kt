package ru.llm.agent.core.utils.model

/**
 * Sealed class для представления результата синхронной операции.
 * Альтернатива NetworkResult для синхронных операций.
 *
 * @param T Тип успешного результата
 */
public sealed class Result<out T> {
    /**
     * Успешный результат операции
     */
    public data class Success<out T>(public val data: T) : Result<T>()

    /**
     * Ошибка выполнения операции
     */
    public data class Error(
        public val message: String,
        public val cause: Throwable? = null
    ) : Result<Nothing>()

    /**
     * Проверяет, является ли результат успешным
     */
    public val isSuccess: Boolean
        get() = this is Success

    /**
     * Проверяет, является ли результат ошибкой
     */
    public val isError: Boolean
        get() = this is Error

    /**
     * Получает данные при успешном результате или null при ошибке
     */
    public fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    /**
     * Получает данные при успешном результате или значение по умолчанию при ошибке
     */
    public fun getOrDefault(defaultValue: @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Error -> defaultValue
    }

    /**
     * Получает данные при успешном результате или выбрасывает исключение при ошибке
     */
    public fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw cause ?: IllegalStateException(message)
    }

    /**
     * Выполняет действие при успешном результате
     */
    public inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) {
            action(data)
        }
        return this
    }

    /**
     * Выполняет действие при ошибке
     */
    public inline fun onError(action: (String, Throwable?) -> Unit): Result<T> {
        if (this is Error) {
            action(message, cause)
        }
        return this
    }

    /**
     * Преобразует успешный результат в другой тип
     */
    public inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    /**
     * Преобразует успешный результат в другой Result
     */
    public inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(data)
        is Error -> this
    }

    /**
     * Восстанавливается после ошибки, возвращая значение по умолчанию
     */
    public inline fun recover(recovery: (String, Throwable?) -> @UnsafeVariance T): Result<T> = when (this) {
        is Success -> this
        is Error -> Success(recovery(message, cause))
    }

    public companion object {
        /**
         * Создаёт Success результат
         */
        public fun <T> success(data: T): Result<T> = Success(data)

        /**
         * Создаёт Error результат
         */
        public fun error(message: String, cause: Throwable? = null): Result<Nothing> =
            Error(message, cause)

        /**
         * Выполняет блок кода и оборачивает результат в Result
         */
        public inline fun <T> runCatching(block: () -> T): Result<T> = try {
            Success(block())
        } catch (e: Exception) {
            Error(e.message ?: "Неизвестная ошибка", e)
        }

        /**
         * Преобразует kotlin.Result в наш Result
         */
        public fun <T> fromKotlinResult(result: kotlin.Result<T>): Result<T> =
            result.fold(
                onSuccess = { Success(it) },
                onFailure = { Error(it.message ?: "Ошибка операции", it) }
            )
    }
}

/**
 * Extension для упрощения работы с коллекцией Result
 * Возвращает Success со списком всех успешных результатов,
 * или первый Error если хотя бы один результат является ошибкой
 */
public fun <T> List<Result<T>>.combineResults(): Result<List<T>> {
    val errors = filterIsInstance<Result.Error>()
    if (errors.isNotEmpty()) {
        return errors.first()
    }

    val successes = filterIsInstance<Result.Success<T>>().map { it.data }
    return Result.Success(successes)
}