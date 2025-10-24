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
import ru.llm.agent.usecase.ExecuteChainTwoAgentsUseCase
import ru.llm.agent.usecase.SendConversationMessageUseCase
import ru.llm.agent.usecase.old.SendMessageToProxyUseCase
import ru.llm.agent.usecase.old.SendMessageToYandexGptUseCase
import kotlin.invoke

class InteractionTwoAgentsViewModel(
    private val executeChainTwoAgentsUseCase: ExecuteChainTwoAgentsUseCase
) : ViewModel() {

    private val _screeState = MutableStateFlow(InteractionTwoAgentsUIState.State.empty())
    internal val screeState = _screeState.asStateFlow()

    private val _events = MutableSharedFlow<InteractionTwoAgentsUIState.Event>()

    init {
        viewModelScope.launch {
            _events.collect {
                handleEvent(it)
            }
        }

    }

    internal fun setEvent(event: InteractionTwoAgentsUIState.Event) {
        viewModelScope.launch { _events.emit(event) }
    }

    private fun handleEvent(event: InteractionTwoAgentsUIState.Event) {
        when (event) {
            is InteractionTwoAgentsUIState.Event.SendMessage -> sendMessageToAi(event.message)
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

        viewModelScope.launch {
            executeChainTwoAgentsUseCase.invoke(
                initialTask = userMessageModel
            ).collect {
                it.handleResult(
                    onLoading = {
                        _screeState.update { it.copy(isLoading = true) }
                    },
                    onSuccess = { result ->
                        _screeState.update {
                            it.copy(
                                isLoading = false,
                                error = "",
                                agentFirstMessage = MessageModel.ResponseMessage(
                                    role = Role.ASSISTANT.title,
                                    content = result.firstAgentMessage,
                                    textFormat = PromtFormat.TEXT,
                                    duration = "",
                                ),
                                agentSecondMessage = MessageModel.ResponseMessage(
                                    role = Role.ASSISTANT.title,
                                    content = result.secondAgentMessage,
                                    textFormat = PromtFormat.TEXT,
                                    duration = "",
                                )
                            )
                        }
                    },
                    onError = { error ->
                        _screeState.update {
                            it.copy(
                                isLoading = false,
                                error = error.orEmpty()
                            )
                        }
                    }
                )
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