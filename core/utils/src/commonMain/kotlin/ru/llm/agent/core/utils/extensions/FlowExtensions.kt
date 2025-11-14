package ru.llm.agent.core.utils.extensions

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.CoroutineScope
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Повторяет Flow с экспоненциальной задержкой между попытками.
 *
 * @param maxAttempts Максимальное количество попыток (по умолчанию 3)
 * @param initialDelay Начальная задержка между попытками (по умолчанию 1 секунда)
 * @param maxDelay Максимальная задержка между попытками (по умолчанию 10 секунд)
 * @param factor Множитель для экспоненциального увеличения задержки (по умолчанию 2.0)
 * @param predicate Условие, определяющее нужно ли повторять при данной ошибке
 */
public fun <T> Flow<T>.retryWithExponentialBackoff(
    maxAttempts: Long = 3,
    initialDelay: Duration = 1.seconds,
    maxDelay: Duration = 10.seconds,
    factor: Double = 2.0,
    predicate: suspend (Throwable) -> Boolean = { true }
): Flow<T> = retry(maxAttempts) { cause ->
    if (!predicate(cause)) {
        return@retry false
    }

    // Вычисляем задержку с экспоненциальным увеличением
    val currentAttempt = maxAttempts - 1
    val calculatedDelay = (initialDelay.inWholeMilliseconds * factor.pow(currentAttempt.toDouble())).toLong()
    val delayDuration = minOf(calculatedDelay, maxDelay.inWholeMilliseconds).milliseconds

    delay(delayDuration)
    true
}

/**
 * Кэширует последнее значение Flow и делится им с новыми подписчиками.
 * Полезно для избежания множественных одновременных запросов к одному источнику данных.
 *
 * @param scope CoroutineScope, в котором будет работать shareIn
 * @param replayExpiration Время, в течение которого кэшированное значение остаётся актуальным
 */
public fun <T> Flow<T>.cacheLatest(
    scope: CoroutineScope,
    replayExpiration: Duration = 5.seconds
): Flow<T> = shareIn(
    scope = scope,
    started = SharingStarted.WhileSubscribed(
        stopTimeoutMillis = replayExpiration.inWholeMilliseconds,
        replayExpirationMillis = replayExpiration.inWholeMilliseconds
    ),
    replay = 1
)

/**
 * Применяет throttle к Flow - пропускает значения, если они поступают чаще чем заданный интервал.
 * Первое значение всегда пропускается.
 *
 * @param periodMillis Минимальный интервал между значениями в миллисекундах
 */
public fun <T> Flow<T>.throttleFirst(periodMillis: Long): Flow<T> = kotlinx.coroutines.flow.flow {
    var lastEmissionTime = 0L
    collect { value ->
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastEmissionTime > periodMillis) {
            lastEmissionTime = currentTime
            emit(value)
        }
    }
}

/**
 * Применяет debounce к Flow - ждёт паузу в поступлении значений перед эмиссией последнего.
 *
 * @param timeoutMillis Время ожидания паузы в миллисекундах
 */
public fun <T> Flow<T>.debounce(timeoutMillis: Long): Flow<T> = kotlinx.coroutines.flow.flow {
    var lastValue: T? = null
    var lastEmissionTime = 0L

    collect { value ->
        lastValue = value
        val currentTime = System.currentTimeMillis()
        lastEmissionTime = currentTime

        delay(timeoutMillis)

        if (System.currentTimeMillis() - lastEmissionTime >= timeoutMillis) {
            lastValue?.let { emit(it) }
        }
    }
}