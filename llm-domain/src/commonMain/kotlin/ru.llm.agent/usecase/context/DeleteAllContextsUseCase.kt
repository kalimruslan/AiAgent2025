package ru.llm.agent.usecase.context

import ru.llm.agent.repository.LocalDbRepository

public class DeleteAllContextsUseCase(
    public val repository: LocalDbRepository
) {
    public suspend operator fun invoke() {
        repository.deleteAllContexts()
    }
}