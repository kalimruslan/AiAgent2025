package ru.llm.agent.usecase.base

import kotlinx.coroutines.flow.Flow

/**
 * Базовый интерфейс для Use Case, возвращающих Flow.
 * Используется для реактивных операций с множественными эмиссиями данных.
 *
 * @param Input Тип входных параметров
 * @param Output Тип элементов в потоке
 */
public interface FlowUseCase<in Input, out Output> {
    /**
     * Выполняет бизнес-логику и возвращает Flow с результатами
     *
     * @param input Входные параметры
     * @return Flow с результатами
     */
    public operator fun invoke(input: Input): Flow<Output>
}

/**
 * Flow Use Case без входных параметров
 */
public interface NoInputFlowUseCase<out Output> : FlowUseCase<Unit, Output> {
    public operator fun invoke(): Flow<Output>

    override fun invoke(input: Unit): Flow<Output> = invoke()
}