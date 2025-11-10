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
import ru.llm.agent.model.ConversationMode
import ru.llm.agent.model.Expert
import ru.llm.agent.model.LlmProvider
import ru.llm.agent.model.Role
import ru.llm.agent.repository.ConversationRepository
import ru.llm.agent.usecase.CommitteeResult
import ru.llm.agent.usecase.ConversationUseCase
import ru.llm.agent.usecase.ExecuteCommitteeUseCase
import ru.llm.agent.usecase.SendConversationMessageUseCase
import java.util.logging.Logger

class ConversationViewModel(
    private val conversationUseCase: ConversationUseCase,
    private val sendConversationMessageUseCase: SendConversationMessageUseCase,
    private val getSelectedProviderUseCase: ru.llm.agent.usecase.GetSelectedProviderUseCase,
    private val saveSelectedProviderUseCase: ru.llm.agent.usecase.SaveSelectedProviderUseCase,
    private val conversationRepository: ConversationRepository,
    private val executeCommitteeUseCase: ExecuteCommitteeUseCase
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
            // Загружаем сохраненный провайдер
            val savedProvider = getSelectedProviderUseCase(conversationId)
            _screeState.update { it.copy(selectedProvider = savedProvider) }

            // Загружаем сообщения
            loadMessages()
        }
    }

    /**
     * Загрузить сообщения в зависимости от режима
     */
    private fun loadMessages() {
        viewModelScope.launch {
            val currentMode = _screeState.value.selectedMode

            when (currentMode) {
                ConversationMode.SINGLE -> {
                    // В режиме Single загружаем только обычные сообщения
                    conversationUseCase.invoke(conversationId).collect { messages ->
                        _screeState.update {
                            it.copy(
                                messages = messages.filter { msg -> msg.role != Role.SYSTEM }
                            )
                        }
                    }
                }

                ConversationMode.COMMITTEE -> {
                    // В режиме Committee загружаем сообщения вместе с мнениями экспертов
                    conversationRepository.getMessagesWithExpertOpinions(conversationId).collect { messages ->
                        Logger.getLogger("Committe").info("messages - $messages")

                        _screeState.update {
                            it.copy(
                                messages = messages.filter { msg -> msg.role != Role.SYSTEM }
                            )
                        }
                    }
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
            ConversationUIState.Event.ResetAll -> resetConversation()
            ConversationUIState.Event.ClearError -> clearError()
            ConversationUIState.Event.OpenSettings -> {}
            is ConversationUIState.Event.SelectProvider -> selectProvider(event.provider)
            is ConversationUIState.Event.SelectMode -> selectMode(event.mode)
            is ConversationUIState.Event.ToggleExpert -> toggleExpert(event.expert)
        }
    }

    /**
     * Отправка сообщения
     * Выбирает режим: Single AI или Committee
     */
    private fun sendMessageToAi(message: String) {
        if (message.isBlank() || _screeState.value.isLoading) return

        when (_screeState.value.selectedMode) {
            ConversationMode.SINGLE -> sendMessageToSingleAi(message)
            ConversationMode.COMMITTEE -> sendMessageToCommittee(message)
        }
    }

    /**
     * Отправка сообщения в режиме Single AI
     */
    private fun sendMessageToSingleAi(message: String) {
        viewModelScope.launch {
            sendConversationMessageUseCase.invoke(
                conversationId = conversationId,
                message = message,
                provider = _screeState.value.selectedProvider
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

    /**
     * Отправка сообщения в режиме Committee of Experts
     */
    private fun sendMessageToCommittee(message: String) {
        viewModelScope.launch {
            executeCommitteeUseCase.invoke(
                conversationId = conversationId,
                userMessage = message,
                experts = _screeState.value.selectedExperts,
                provider = _screeState.value.selectedProvider
            ).collect { result ->
                result.doActionIfLoading {
                    _screeState.update { it.copy(isLoading = true, error = "") }
                }
                result.doActionIfSuccess { committeeResult ->
                    when (committeeResult) {
                        is CommitteeResult.ExpertOpinion -> {
                            // Мнение эксперта получено
                            // UI обновится через Flow из БД
                            _screeState.update { it.copy(isLoading = true) }
                        }
                        is CommitteeResult.FinalSynthesis -> {
                            // Финальный ответ получен
                            _screeState.update { state ->
                                state.copy(
                                    isLoading = false,
                                    isConversationComplete = true
                                )
                            }
                        }
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

    /**
     * Выбор провайдера LLM
     */
    private fun selectProvider(provider: LlmProvider) {
        viewModelScope.launch {
            saveSelectedProviderUseCase(conversationId, provider)
            _screeState.update { it.copy(selectedProvider = provider) }
        }
    }

    /**
     * Выбор режима работы (Single AI / Committee)
     */
    private fun selectMode(mode: ConversationMode) {
        _screeState.update { it.copy(selectedMode = mode) }
        // Перезагружаем сообщения при смене режима
        loadMessages()
    }

    /**
     * Переключить эксперта (добавить/убрать из выбранных)
     */
    private fun toggleExpert(expert: Expert) {
        _screeState.update { state ->
            val currentExperts = state.selectedExperts
            val updatedExperts = if (currentExperts.contains(expert)) {
                // Убрать эксперта (но минимум 1 должен остаться)
                if (currentExperts.size > 1) {
                    currentExperts - expert
                } else {
                    currentExperts // Не даем удалить последнего
                }
            } else {
                // Добавить эксперта
                currentExperts + expert
            }
            state.copy(selectedExperts = updatedExperts)
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