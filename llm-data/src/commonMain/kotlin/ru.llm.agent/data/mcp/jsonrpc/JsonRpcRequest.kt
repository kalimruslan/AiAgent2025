package ru.llm.agent.data.mcp.jsonrpc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
public data class JsonRpcRequest(
    @SerialName("jsonrpc")
    val jsonrpc: String = "2.0",
    val id: String,
    val method: String,
    @SerialName("params")
    val params: JsonObject? = null
)