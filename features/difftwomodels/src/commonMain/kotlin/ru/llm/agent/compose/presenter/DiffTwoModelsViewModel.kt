package ru.llm.agent.compose.presenter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.llm.agent.RoleSender
import ru.llm.agent.doActionIfError
import ru.llm.agent.doActionIfLoading
import ru.llm.agent.doActionIfSuccess
import ru.llm.agent.handleResult
import ru.llm.agent.model.MessageModel
import ru.llm.agent.model.PromtFormat
import ru.llm.agent.model.Role
import ru.llm.agent.usecase.ConversationUseCase
import ru.llm.agent.usecase.SendConversationMessageUseCase
import ru.llm.agent.usecase.old.SendMessageToProxyUseCase
import ru.llm.agent.usecase.old.SendMessageToYandexGptUseCase
import kotlin.invoke

class DiffTwoModelsViewModel(
    private val sendMessageToYandexGptUseCase: SendMessageToYandexGptUseCase,
    private val sendMessageProxy: SendMessageToProxyUseCase,
) : ViewModel() {

    private val _screeState = MutableStateFlow(DiffTwoModelsUIState.State.empty())
    internal val screeState = _screeState.asStateFlow()

    private val _events = MutableSharedFlow<DiffTwoModelsUIState.Event>()

    init {
        viewModelScope.launch {
            _events.collect {
                handleEvent(it)
            }
        }

    }

    internal fun setEvent(event: DiffTwoModelsUIState.Event) {
        viewModelScope.launch { _events.emit(event) }
    }

    private fun handleEvent(event: DiffTwoModelsUIState.Event) {
        when (event) {
            is DiffTwoModelsUIState.Event.SendMessage -> sendMessageToAi(event.message)
        }
    }

    /**
     * Отправка сообщения на конкретную LLM
     */
    private fun sendMessageToAi(message: String) {
        if (message.isBlank() || _screeState.value.isLoading) return

        val userMessageModel = MessageModel.UserMessage(
            role = Role.USER,
            content = message
        )

        var messageFirst: MessageModel.ResponseMessage? = null
        var messageSecond: MessageModel.ResponseMessage? = null

        viewModelScope.launch {
            val deffered1 = async {
                val startTime = System.currentTimeMillis()
                sendMessageToYandexGptUseCase.invoke(
                    userMessage = userMessageModel,
                    outputFormat = PromtFormat.TEXT,
                    model = "gpt://b1gonedr4v7ke927m32n/yandexgpt-lite"
                ).collect {
                    it.doActionIfSuccess { message: MessageModel? ->
                        val duration = System.currentTimeMillis() - startTime
                        messageFirst = (message as MessageModel.ResponseMessage).copy(
                            duration = formatDuration(duration)
                        )
                        messageFirst
                    }
                }
            }

            val deffered2 = async {
                val startTime = System.currentTimeMillis()
                sendMessageProxy.invoke(
                    message = message,
                    roleSender = RoleSender.USER,
                    outputFormat = PromtFormat.TEXT,
                    model = "gpt-4o-mini"
                ).collect {
                    it.doActionIfSuccess { message: MessageModel? ->
                        val duration = System.currentTimeMillis() - startTime
                        messageSecond = (message as MessageModel.ResponseMessage).copy(
                            duration = formatDuration(duration)
                        )
                        messageSecond
                    }
                }
            }

            deffered1.await()
            deffered2.await()

            if(messageFirst != null && messageSecond != null){
                _screeState.update {
                    it.copy(
                        messageFirst = messageFirst,
                        messageTwo = messageSecond,
                        isLoading = false,
                        error = ""
                    )
                }
            }
        }
    }

    fun formatDuration(durationMs: Long): String {
        val minutes = durationMs / 60_000
        val seconds = (durationMs % 60_000) / 1_000
        val milliseconds = durationMs % 1_000

        return buildString {
            if (minutes > 0) {
                append("$minutes мин ")
            }
            append("$seconds с ")
            append("$milliseconds мс")
        }
    }

}