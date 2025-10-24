package ru.llm.agent.compose.presenter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.llm.agent.doActionIfSuccess
import ru.llm.agent.model.MessageModel
import ru.llm.agent.model.PromtFormat
import ru.llm.agent.model.Role
import ru.llm.agent.repository.LlmRepository
import kotlin.collections.plus

class TokensViewModel(
    private val repository: LlmRepository,
) : ViewModel() {

    val maxTokens = 20

    private val _screenState = MutableStateFlow(TokensUIState.State.empty())
    val screenState: StateFlow<TokensUIState.State> = _screenState.asStateFlow()

    private val _newMessages = MutableStateFlow<List<MessageModel>>(emptyList())
    private val newMessages = _newMessages.asStateFlow()

    init {
        viewModelScope.launch {
            newMessages.collect { messages ->
                _screenState.update {
                    it.copy(messages = messages)
                }
            }
        }
    }

    /**
     * Получаемк количество токенов для запроса
     */
    private fun getInputTokensCount(userMessage: String, successAction: (Int) -> Unit) {
        viewModelScope.launch {
            repository.countYandexGPTTokens(
                text = userMessage,
                modelUri = "gpt://b1gonedr4v7ke927m32n/yandexgpt-lite"
            ).collect { result ->
                result.doActionIfSuccess { tokenCount ->
                    successAction(tokenCount)
                }
            }
        }
    }

    fun sendMessage(userMessage: String) {
        getInputTokensCount(userMessage) { tokens ->
            if (tokens < maxTokens) {
                val message = MessageModel.UserMessage(
                    role = Role.USER, content = userMessage, inputTokens = tokens
                )
                _newMessages.value += message

                sendMessageToYandexGPT(message)
            } else {
                compressContext(userMessage, tokens)
            }
        }
    }

    private fun compressContext(userMessage: String, notCompressedTokens: Int) {
        viewModelScope.launch {
            val compressedText = repository.summarizeYandexGPTText(
                text = userMessage,
                model = "gpt://b1gonedr4v7ke927m32n/yandexgpt-lite",
                maxTokens = maxTokens / 3
            )

            if (compressedText.isNotEmpty()) {
                getInputTokensCount(
                    userMessage = compressedText
                ){ tokenCount ->
                    val message = MessageModel.UserMessage(
                        role = Role.USER,
                        content = userMessage,
                        inputTokens = tokenCount,
                        isCompressed = true,
                        notCompressedTokens = notCompressedTokens
                    )
                    _newMessages.value += message

                    sendMessageToYandexGPT(message)
                }
            }
        }
    }

    private fun sendMessageToYandexGPT(message: MessageModel.UserMessage) {
        viewModelScope.launch {
            // Отправляем запрос
            repository.sendMessageToYandexGPT(
                promptMessage = null,
                userMessage = message,
                model = "gpt://b1gonedr4v7ke927m32n/yandexgpt-lite",
                outputFormat = PromtFormat.TEXT
            ).collect { result ->
                result.doActionIfSuccess { token: MessageModel? ->
                    val theirMessage = token as MessageModel.ResponseMessage
                    _newMessages.value += theirMessage
                }
            }
        }
    }
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
)