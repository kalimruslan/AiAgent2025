package ru.llm.agent.compose.presenter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.llm.agent.doActionIfError
import ru.llm.agent.doActionIfLoading
import ru.llm.agent.doActionIfSuccess
import ru.llm.agent.model.Role
import ru.llm.agent.usecase.ConversationUseCase
import ru.llm.agent.usecase.SendConversationMessageUseCase

class ConversationViewModel(
    private val conversationUseCase: ConversationUseCase,
    private val sendConversationMessageUseCase: SendConversationMessageUseCase
) : ViewModel() {

    private val _screeState = MutableStateFlow(ConversationUIState.State.empty())
    internal val screeState = _screeState.asStateFlow()

    private val _events = MutableSharedFlow<ConversationUIState.Event>()
    val conversationId = "default_conversation"

    init {
        viewModelScope.launch {
            _events.collect {
                handleEvent(it)
            }
        }


    }

    fun start(){
        viewModelScope.launch {
            conversationUseCase.invoke(conversationId).collect { messages ->
                _screeState.update {
                    it.copy(
                        messages = messages.filter { msg -> msg.role != Role.SYSTEM }
                    )
                }
            }
        }
    }

    internal fun setEvent(event: ConversationUIState.Event) {
        viewModelScope.launch { _events.emit(event) }
    }

    private fun handleEvent(event: ConversationUIState.Event) {
        when (event) {
            is ConversationUIState.Event.SendMessage -> sendMessageToAi(event.message)
            ConversationUIState.Event.ResetConversation -> resetConversation()
            ConversationUIState.Event.ClearError -> clearError()
            ConversationUIState.Event.OpenSettings -> {}
        }
    }

    /**
     * Отправка сообщения на конкретную LLM
     */
    private fun sendMessageToAi(message: String) {
        if (message.isBlank() || _screeState.value.isLoading) return

        viewModelScope.launch {
            sendConversationMessageUseCase.invoke(
                conversationId = conversationId,
                message = message,
                model = "gpt://b1gonedr4v7ke927m32n/yandexgpt-lite"
            ).collect { result ->
                result.doActionIfLoading {
                    _screeState.update { it.copy(isLoading = true, error = "") }
                }
                result.doActionIfSuccess {
                    _screeState.update { state ->
                        state.copy(
                            isLoading = false,
                            isConversationComplete = it.isComplete
                        )
                    }
                }
                result.doActionIfError {
                    _screeState.update {
                        it.copy(
                            isLoading = false,
                            error = "Произошла ошибка"
                        )
                    }
                }
            }
        }
    }

    private fun resetConversation() {
        viewModelScope.launch {
            conversationUseCase.clearConversation(conversationId)
            _screeState.update { it.copy(
                isConversationComplete = false,
                error = ""
            )}
        }
    }

    private fun clearError() {
        _screeState.update { it.copy(error = "") }
    }
}