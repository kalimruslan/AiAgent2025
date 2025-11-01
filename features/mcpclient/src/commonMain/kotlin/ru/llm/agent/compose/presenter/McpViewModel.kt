package ru.llm.agent.compose.presenter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.llm.agent.McpClient


class McpViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(McpUiState())
    val uiState: StateFlow<McpUiState> = _uiState.asStateFlow()

    private val client = McpClient()

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
                    _uiState.value = _uiState.value.copy(tools = tools)
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
    }

    override fun onCleared() {
        super.onCleared()
        client.close()
    }
}