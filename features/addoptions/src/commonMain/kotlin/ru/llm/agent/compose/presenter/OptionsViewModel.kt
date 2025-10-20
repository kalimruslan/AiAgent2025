package ru.llm.agent.compose.presenter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.llm.agent.model.Settings
import ru.llm.agent.usecase.GetOptionsFromDbUseCase
import ru.llm.agent.usecase.SendOptionsToLocalDbUseCase
import java.util.logging.Logger

class OptionsViewModel(
    private val getOptionsFromDbUseCase: GetOptionsFromDbUseCase,
    private val sendOptionsToLocalDbUseCase: SendOptionsToLocalDbUseCase
) : ViewModel() {

    private val _screeState = MutableStateFlow(OptionsUIState.State.empty())
    internal val screeState = _screeState.asStateFlow()

    private val _events = MutableSharedFlow<OptionsUIState.Event>()

    init {
        viewModelScope.launch {
            _events.collect {
                handleEvent(it)
            }
        }
    }

    fun start() {
        viewModelScope.launch {
            val options = getOptionsFromDbUseCase.invoke()
            Logger.getLogger("Conversation").info("options = ${options?.systemPrompt}")
            _screeState.value = _screeState.value.copy(
                temperature = options?.temperature ?: _screeState.value.temperature,
                systemPrompt = options?.systemPrompt ?: _screeState.value.systemPrompt,
                maxTokens = options?.maxTokens ?: _screeState.value.maxTokens
            )
        }
    }

    internal fun setEvent(event: OptionsUIState.Event) {
        viewModelScope.launch { _events.emit(event) }
    }


    private fun handleEvent(event: OptionsUIState.Event) {
        when (event) {
            is OptionsUIState.Event.ApplyClick -> {
                val settings = Settings(
                    temperature = event.temperature.toDouble(),
                    systemPrompt = event.systemPrompt.orEmpty(),
                    maxTokens = event.maxTokens.toInt(),
                    timestamp = System.currentTimeMillis()
                )
                viewModelScope.launch {
                    sendOptionsToLocalDbUseCase.invoke(settings)
                    event.navigateAction()
                }
            }
        }
    }
}