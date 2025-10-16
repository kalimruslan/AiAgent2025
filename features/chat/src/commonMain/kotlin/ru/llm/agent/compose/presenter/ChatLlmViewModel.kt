package ru.llm.agent.compose.presenter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.llm.agent.OutputFormat
import ru.llm.agent.RoleSender
import ru.llm.agent.compose.presenter.model.MessageTypeUI
import ru.llm.agent.handleResult
import ru.llm.agent.model.MessageModel
import ru.llm.agent.usecase.SendMessageToYandexGpt
import java.lang.Error
import java.util.logging.Logger

class ChatLlmViewModel(
    private val sendMessageToYaGPT: SendMessageToYandexGpt
) : ViewModel() {

    private val _screeState = MutableStateFlow(ChatLlmContract.State.empty())
    internal val screeState = _screeState.asStateFlow()

    private val _newMessages = MutableStateFlow<List<MessageTypeUI>>(emptyList())
    private val newMessages = _newMessages.asStateFlow()

    private val _events = MutableSharedFlow<ChatLlmContract.Event>()

    init {
        viewModelScope.launch {
            _events.collect {
                handleEvent(it)
            }
        }

        viewModelScope.launch {
            newMessages.collect { userMessage ->
                updateState(messages = userMessage)
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
        }
    }

    /**
     * Отправка сообщения на конкретную LLM
     */
    private fun sendMessageToAi(message: String) = viewModelScope.launch {
        Logger.getLogger("ChatLlmViewModel")
            .info("sendMessageToSingleLlm apiModel: ${_screeState.value.selectedAIType}")
        val userMessageModel = MessageModel("user", message)
        userMessageModel.addMessage(myMessage = true)
        val resultFlow = when (_screeState.value.selectedAIType) {
            is AiType.YaGptAI -> {
                sendMessageToYaGPT.invoke(
                    messageModel = userMessageModel,
                    outputFormat = OutputFormat.TEXT,
                    model = (_screeState.value.selectedAIType as AiType.YaGptAI).selectedModel
                )
            }
        }
        resultFlow.collect {
            it.handleResult(
                onLoading = {
                    updateState(isLoading = true, error = "")
                },
                onSuccess = { message: MessageModel? ->
                    message?.addMessage(myMessage = false)
                    updateState(isLoading = false)
                },
                onError = { errorMessage ->
                    updateState(error = errorMessage ?: "Неизвестная ошибка", isLoading = false)
                }
            )
        }
    }

    private fun updateState(
        messages: List<MessageTypeUI>? = null,
        isLoading: Boolean? = null,
        error: String? = null
    ) {
        _screeState.update {
            it.copy(
                messages = messages ?: it.messages,
                isLoading = isLoading ?: it.isLoading,
                error = error ?: it.error
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

    private fun MessageModel.addMessage(myMessage: Boolean) {
        val messageTypeUI = if (myMessage) {
            MessageTypeUI.MyMessageUI(this)
        } else {
            MessageTypeUI.TheirMessageUI(this)
        }
        _newMessages.value += messageTypeUI
    }
}