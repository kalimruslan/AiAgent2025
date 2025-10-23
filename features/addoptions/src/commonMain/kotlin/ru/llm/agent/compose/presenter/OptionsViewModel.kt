package ru.llm.agent.compose.presenter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.llm.agent.model.ConversationContext
import ru.llm.agent.usecase.context.GetLocalContextUseCase
import ru.llm.agent.usecase.context.RemoveLocalContextUseCase
import ru.llm.agent.usecase.context.SaveLocalContextUseCase

class OptionsViewModel(
    private val getLocalContextUseCase: GetLocalContextUseCase,
    private val saveLocalContextUseCase: SaveLocalContextUseCase,
    private val removeLocalContextUseCase: RemoveLocalContextUseCase
) : ViewModel() {

    private val _screeState = MutableStateFlow(
        OptionsUIState.State.default(
            ""
        )
    )
    internal val screeState = _screeState.asStateFlow()

    private val _events = MutableSharedFlow<OptionsUIState.Event>()

    init {
        viewModelScope.launch {
            _events.collect {
                handleEvent(it)
            }
        }
    }

    fun start(conversationId: String) {
        viewModelScope.launch {
            val context = getLocalContextUseCase.invoke(conversationId)
            _screeState.value = _screeState.value.copy(
                conversationId = conversationId,
                temperature = context?.temperature ?: _screeState.value.temperature,
                systemPrompt = context?.systemPrompt ?: _screeState.value.systemPrompt,
                maxTokens = context?.maxTokens ?: _screeState.value.maxTokens
            )
        }
    }

    internal fun setEvent(event: OptionsUIState.Event) {
        viewModelScope.launch { _events.emit(event) }
    }


    private fun handleEvent(event: OptionsUIState.Event) {
        when (event) {
            is OptionsUIState.Event.ApplyClick -> {
                val conversationContext = ConversationContext(
                    temperature = event.temperature.toDouble(),
                    systemPrompt = event.systemPrompt.orEmpty(),
                    maxTokens = event.maxTokens.toInt(),
                    timestamp = System.currentTimeMillis()
                )
                viewModelScope.launch {
                    saveLocalContextUseCase.invoke(
                        conversationId = _screeState.value.conversationId,
                        context = conversationContext
                    )
                    event.navigateAction()
                }
            }

            OptionsUIState.Event.ResetOptions -> viewModelScope.launch {
                removeLocalContextUseCase.invoke(_screeState.value.conversationId)
                _screeState.value = OptionsUIState.State.default(_screeState.value.conversationId)
            }
        }
    }
}