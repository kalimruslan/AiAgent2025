package ru.llm.agent.compose.presenter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.llm.agent.InteractYaGptWithMcpService
import ru.llm.agent.NetworkResult
import ru.llm.agent.handleResult
import ru.llm.agent.model.MessageModel
import ru.llm.agent.model.mcp.YaGptTool
import java.util.logging.Logger


class McpViewModel(
    private val interactYaGptWithMcpService: InteractYaGptWithMcpService,
) : ViewModel() {
    private val _uiState = MutableStateFlow(McpUiState())
    val uiState: StateFlow<McpUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            interactYaGptWithMcpService.getTools().collect { mcpTools: List<YaGptTool> ->
                _uiState.update {
                    it.copy(
                        isConnected = true,
                        mcpTools = mcpTools
                    )
                }
            }

        }
    }

    fun sendMessage(message: String) {
        viewModelScope.launch {
            interactYaGptWithMcpService.chat(message)
                .collect { result: NetworkResult<MessageModel> ->
                    result.handleResult(
                        onSuccess = {
                            if(it is MessageModel.NoneMessage){
                                Logger.getLogger("McpClient").info("NoneMessage - ${it.message}")
                            }
                            _uiState.update { state ->
                                state.copy(
                                    isLoading = false,
                                    messages = interactYaGptWithMcpService.getHistory()
                                )
                            }
                        },
                        onError = {
                            _uiState.update { state ->
                                state.copy(
                                    isLoading = false,
                                    error = it
                                )
                            }
                        },
                        onLoading = {
                            _uiState.update { state -> state.copy(isLoading = true) }
                        }
                    )

                }
        }
    }

    /*    private val client = McpClientOld()

        fun connect() {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                client.initialize()
                    .onSuccess { initResult ->
                        _uiState.value = _uiState.value.copy(
                            isConnected = true,
                            isLoading = false,
                            result = "Connected: ${initResult.serverInfo.name} v${initResult.serverInfo.version}"
                        )
                        loadTools()
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Connection failed: ${error.message}"
                        )
                    }
            }
        }

        private fun loadTools() {
            viewModelScope.launch {
                client.listTools()
                    .onSuccess { tools ->
                        _uiState.value = _uiState.value.copy(mcpTools = tools)
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to load tools: ${error.message}"
                        )
                    }
            }
        }

        fun callEcho(text: String) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                client.echo(text)
                    .onSuccess { result ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            result = result
                        )
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
            }
        }

        fun callAdd(a: Double, b: Double) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                client.add(a, b)
                    .onSuccess { result ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            result = result
                        )
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
            }
        }

        fun getCurrentTime() {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                client.getCurrentTime()
                    .onSuccess { result ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            result = result
                        )
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
            }
        }*/

}