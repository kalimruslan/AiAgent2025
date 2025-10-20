package ru.llm.agent.usecase

import ru.llm.agent.model.Settings
import ru.llm.agent.repository.LocalDbRepository

public class GetOptionsFromDbUseCase(
    public val repository: LocalDbRepository
) {
    public suspend operator fun invoke(): Settings? {
        return repository.getSettings()
    }
}