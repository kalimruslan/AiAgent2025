package ru.llm.agent.data.jsonrpc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class JsonRpcResponse(
    @SerialName("jsonrpc")
    val jsonrpc: String = "2.0",
    val id: String,
    val result: JsonElement? = null,
    val error: JsonRpcError? = null
){
    @Serializable
    data class JsonRpcError(
        val code: Int,
        val message: String,
        val data: JsonElement? = null
    )
}