package ru.llm.agent.usecase.base

/**
 * Базовый интерфейс для асинхронных Use Case.
 * Используется для операций, требующих suspend функций (например, сетевые запросы, работа с БД).
 *
 * @param Input Тип входных параметров
 * @param Output Тип возвращаемого результата
 */
public interface SuspendUseCase<in Input, out Output> {
    /**
     * Выполняет асинхронную бизнес-логику Use Case
     *
     * @param input Входные параметры
     * @return Результат выполнения
     */
    public suspend operator fun invoke(input: Input): Output
}

/**
 * Асинхронный Use Case без входных параметров
 */
public interface NoInputSuspendUseCase<out Output> : SuspendUseCase<Unit, Output> {
    public suspend operator fun invoke(): Output

    override suspend fun invoke(input: Unit): Output = invoke()
}