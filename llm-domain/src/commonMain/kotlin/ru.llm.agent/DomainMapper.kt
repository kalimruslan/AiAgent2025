package ru.llm.agent

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import ru.llm.agent.model.mcp.McpToolInfo
import ru.llm.agent.model.mcp.YaGptFunction
import ru.llm.agent.model.mcp.YaGptTool
import kotlin.collections.component1
import kotlin.collections.component2

public fun McpToolInfo.toYaGptTool(): YaGptTool {
    return YaGptTool(
        function = YaGptFunction(
            name = this.name,
            description = this.description,
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                put("properties", buildJsonObject {
                    this@toYaGptTool.parameters.forEach { (paramName, paramInfo) ->
                        put(paramName, buildJsonObject {
                            put("type", JsonPrimitive(paramInfo.type))
                            put("description", JsonPrimitive(paramInfo.description))
                        })
                    }
                })
                if (this@toYaGptTool.requiredParameters.isNotEmpty()) {
                    put("required", buildJsonArray {
                        this@toYaGptTool.requiredParameters.forEach { paramName ->
                            add(JsonPrimitive(paramName))
                        }
                    })
                }
            }
        )
    )
}