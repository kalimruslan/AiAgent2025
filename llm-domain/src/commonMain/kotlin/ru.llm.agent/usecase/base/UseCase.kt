package ru.llm.agent.usecase.base

/**
 * Базовый интерфейс для всех Use Case в приложении.
 * Определяет стандартный контракт для бизнес-логики.
 *
 * @param Input Тип входных параметров
 * @param Output Тип возвращаемого результата
 */
public interface UseCase<in Input, out Output> {
    /**
     * Выполняет бизнес-логику Use Case
     *
     * @param input Входные параметры
     * @return Результат выполнения
     */
    public operator fun invoke(input: Input): Output
}

/**
 * Use Case без входных параметров
 */
public interface NoInputUseCase<out Output> : UseCase<Unit, Output> {
    public operator fun invoke(): Output

    override fun invoke(input: Unit): Output = invoke()
}