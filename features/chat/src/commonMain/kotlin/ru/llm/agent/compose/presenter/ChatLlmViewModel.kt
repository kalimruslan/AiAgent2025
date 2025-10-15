package ru.llm.agent.compose.presenter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.llm.agent.OutputFormat
import ru.llm.agent.RoleSender
import ru.llm.agent.handleResult
import ru.llm.agent.model.MessageModel
import ru.llm.agent.usecase.SendMessageToProxyUseCase
import ru.llm.agent.usecase.SendMessageToYandexGpt
import java.util.logging.Logger

class ChatLlmViewModel(
    private val sendMessageProxy: SendMessageToProxyUseCase,
    private val sendMessageToYaGPT: SendMessageToYandexGpt
) : ViewModel() {

    private val _screeState = MutableStateFlow(ChatLlmContract.State.empty())
    internal val screeState = _screeState.asStateFlow()

    private val _events = MutableSharedFlow<ChatLlmContract.Event>()

    init {
        viewModelScope.launch {
            _events.collect {
                handleEvent(it)
            }
        }
    }

    internal fun setEvent(event: ChatLlmContract.Event) {
        viewModelScope.launch { _events.emit(event) }
    }

    private fun handleEvent(event: ChatLlmContract.Event) {
        when (event) {
            is ChatLlmContract.Event.SendMessage -> sendMessageToAi(event.message)

            is ChatLlmContract.Event.SelectAIType -> selectAIType(event.aiType)
            is ChatLlmContract.Event.SelectProxyAiModel -> {
                (_screeState.value.selectedAIType as? AiType.ProxyAI)?.let { proxyAiType ->
                    _screeState.update {
                        it.copy(selectedAIType = proxyAiType.copy(selectedModel = event.model))
                    }
                }
            }
        }
    }

    /**
     * Отправка сообщения на конкретную LLM
     */
    private fun sendMessageToAi(message: String) = viewModelScope.launch {
        Logger.getLogger("ChatLlmViewModel")
            .info("sendMessageToSingleLlm apiModel: ${_screeState.value.selectedAIType}")
        val resultFlow = when (_screeState.value.selectedAIType) {
            is AiType.ProxyAI -> {
                sendMessageProxy.invoke(
                    message = message,
                    roleSender = RoleSender.USER,
                    outputFormat = OutputFormat.TEXT,
                    model = (_screeState.value.selectedAIType as AiType.ProxyAI).selectedModel.model
                )
            }

            is AiType.YaGptAI -> {
                sendMessageToYaGPT.invoke(
                    message = message,
                    roleSender = RoleSender.USER,
                    outputFormat = OutputFormat.TEXT,
                    model = (_screeState.value.selectedAIType as AiType.YaGptAI).selectedModel
                )
            }
        }
        resultFlow.collect {
            it.handleResult(
                onLoading = {
                    _screeState.update { state -> state.copy(isLoading = true, error = null) }
                },
                onSuccess = { message: MessageModel? ->
                    _screeState.update {
                        it.copy(
                            messages = _screeState.value.messages + listOf(
                                MessageModel("user", _screeState.value.userMessage),
                                message ?: MessageModel("user", "Пустой ответ")
                            ),
                            userMessage = "",
                            isLoading = false
                        )
                    }
                },
                onError = { errorMessage ->
                    _screeState.update {
                        it.copy(
                            error = errorMessage ?: "Неизвестная ошибка",
                            isLoading = false
                        )
                    }
                }
            )
        }
    }

    /**
     * Функция для выбора конкретной LLM
     */
    private fun selectAIType(aiType: AiType) {
        _screeState.value = _screeState.value.copy(selectedAIType = aiType)
        Logger.getLogger("ChatLlmViewModel").info("aiType: ${_screeState.value.selectedAIType}")
    }
}