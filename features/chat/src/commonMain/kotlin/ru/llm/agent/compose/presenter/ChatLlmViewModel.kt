package ru.llm.agent.compose.presenter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.llm.agent.compose.presenter.model.MessageTypeUI
import ru.llm.agent.handleResult
import ru.llm.agent.model.MessageModel
import ru.llm.agent.model.PromtFormat
import ru.llm.agent.model.Role
import ru.llm.agent.usecase.ParseJsonFormatUseCase
import ru.llm.agent.usecase.SendMessageToYandexGptUseCase
import java.util.logging.Logger
import kotlin.text.set

class ChatLlmViewModel(
    private val sendMessageToYaGPT: SendMessageToYandexGptUseCase,
    private val parseJsonFormatUseCase: ParseJsonFormatUseCase
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
            is ChatLlmContract.Event.SelectOutputFormat -> selectOutputFormat(event.outputFormat)
            is ChatLlmContract.Event.OnParseClick -> doParsing(event.message)
        }
    }

    private fun doParsing(message: MessageModel.ResponseMessage) {
        val parsed = parseJsonFormatUseCase.invoke(message.content, message.textFormat)
        Logger.getLogger("ChatLlmViewModel").info("parsed: $parsed")

        val currentMessages = _screeState.value.messages
        val index = currentMessages.indexOfFirst { it is MessageTypeUI.TheirMessageUI && it.message.content == message.content }

        val updatedMessage = message.copy(
            parsedContent = parsed
        )

        if (index != -1) {
            val updatedMessages = currentMessages.toMutableList()
            updatedMessages[index] = MessageTypeUI.TheirMessageUI(updatedMessage)
            updateState(messages = updatedMessages)
        }
    }

    /**
     * Выбор формата вывода
     * @param outputFormat - формат вывода @see OutputFormat
     */
    private fun selectOutputFormat(outputFormat: PromtFormat) {
        updateState(outputFormat = outputFormat)
        Logger.getLogger("ChatLlmViewModel").info("outputFormat: ${_screeState.value.outputFormat}")
    }

    /**
     * Отправка сообщения на конкретную LLM
     */
    private fun sendMessageToAi(message: String) = viewModelScope.launch {
        Logger.getLogger("ChatLlmViewModel")
            .info("sendMessageToSingleLlm apiModel: ${_screeState.value.selectedAIType}")

        val userMessageModel = MessageModel.UserMessage(
            role = Role.USER,
            content = message
        )
        userMessageModel.addMessage(myMessage = true)
        val resultFlow = when (_screeState.value.selectedAIType) {
            is AiType.YaGptAI -> {
                sendMessageToYaGPT.invoke(
                    userMessage = userMessageModel,
                    outputFormat = _screeState.value.outputFormat,
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
        error: String? = null,
        outputFormat: PromtFormat? = null
    ) {
        _screeState.update {
            it.copy(
                messages = messages ?: it.messages,
                isLoading = isLoading ?: it.isLoading,
                error = error ?: it.error,
                outputFormat = outputFormat ?: it.outputFormat
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

    /**
     * Добавление сообщения в список для отображения на экране
     * @param myMessage - флаг, что сообщение отправлено мной
     */
    private fun MessageModel.addMessage(myMessage: Boolean) {
        val messageTypeUI = if (myMessage) {
            MessageTypeUI.MyMessageUI(this as MessageModel.UserMessage)
        } else {
            MessageTypeUI.TheirMessageUI(this as MessageModel.ResponseMessage)
        }
        _newMessages.value += messageTypeUI
    }
}