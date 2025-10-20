package ru.llm.agent.usecase

import kotlinx.serialization.json.Json
import ru.llm.agent.model.ParseFromJsonModel
import ru.llm.agent.model.PromtFormat

/**
 * Use Case парсит Json в модель
 */
public class ParseJsonFormatUseCase {
    public operator fun  invoke(
        originalText: String,
        originalFormat: PromtFormat
    ): ParseFromJsonModel? {
        return if (originalFormat == PromtFormat.JSON) {
            Json.decodeFromString<ParseFromJsonModel>(originalText)
        } else null
    }
}