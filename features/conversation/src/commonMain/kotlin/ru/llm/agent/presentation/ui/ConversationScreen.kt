package ru.llm.agent.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.koin.compose.module.rememberKoinModules
import org.koin.compose.scope.KoinScope
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject
import ru.llm.agent.presentation.di.CONVERSATION_CHAT_SCOPE_ID
import ru.llm.agent.presentation.di.conversationChatScopeQualifier
import ru.llm.agent.presentation.viewmodel.ConversationViewModel
import ru.llm.agent.presentation.state.ConversationUIState
import ru.llm.agent.presentation.di.conversationKoinModule
import ru.llm.agent.core.uikit.LlmAgentTheme
import ru.llm.agent.model.ConversationMode
import ru.llm.agent.model.ExportFormat
import ru.llm.agent.model.conversation.ConversationMessage
import ru.llm.agent.presentation.ui.components.InputBar
import ru.llm.agent.presentation.ui.components.MessageItem
import ru.llm.agent.presentation.ui.components.TokenUsageChip
import ru.llm.agent.presentation.ui.components.BoardSummaryCard
import ru.llm.agent.mcp.presentation.ui.McpToolsPanel
import ru.llm.agent.mcp.presentation.viewmodel.McpViewModel
import ru.llm.agent.presentation.ui.components.SmartPromptsBar
import ru.llm.agent.presentation.ui.components.RagControlPanel
import ru.llm.agent.presentation.ui.components.RagSettings
import ru.llm.agent.presentation.ui.components.KnowledgeBaseDialog
import ru.llm.agent.presentation.ui.dropdowns.ConversationModeDropdown
import ru.llm.agent.presentation.ui.dropdowns.LlmProviderDropdown
import ru.llm.agent.committee.presentation.ui.ExpertsSelectionPanel
import ru.llm.agent.committee.presentation.viewmodel.CommitteeViewModel
import ru.llm.agent.committee.presentation.state.CommitteeEvent
import ru.llm.agent.presentation.ui.menu.TopBarMenu

/**
 * Главный экран диалога с AI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    onNavigateToOptions: (String) -> Unit,
) {
    KoinScope(CONVERSATION_CHAT_SCOPE_ID, conversationChatScopeQualifier) {
        rememberKoinModules {
            listOf(conversationKoinModule())
        }
        val viewModel = koinViewModel() as ConversationViewModel
        // Используем koinInject вместо koinViewModel для singleton
        val mcpViewModel = koinInject<McpViewModel>()
        val committeeViewModel = koinInject<CommitteeViewModel>()
        viewModel.start()
        val state by viewModel.screeState.collectAsStateWithLifecycle()
        val mcpState by mcpViewModel.state.collectAsStateWithLifecycle()
        val committeeState by committeeViewModel.state.collectAsStateWithLifecycle()

        Scaffold(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("AI Консультант")
                            ConversationModeDropdown(
                                selectedMode = state.selectedMode,
                                onModeSelected = { mode ->
                                    viewModel.setEvent(
                                        ConversationUIState.Event.SelectMode(mode)
                                    )
                                },
                                enabled = !state.isLoading
                            )
                            LlmProviderDropdown(
                                selectedProvider = state.selectedProvider,
                                onProviderSelected = { provider ->
                                    viewModel.setEvent(
                                        ConversationUIState.Event.SelectProvider(provider)
                                    )
                                },
                                enabled = !state.isLoading
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    actions = {
                        // Компактный чип использования токенов
                        TokenUsageChip(
                            usedTokens = state.usedTokens,
                            maxTokens = state.maxTokens,
                            requestTokens = state.requestTokens,
                            summarizationInfo = state.summarizationInfo,
                            isSummarizing = state.isSummarizing,
                            modifier = Modifier.padding(end = 8.dp)
                        )

                        TopBarMenu(
                            onClearAll = {
                                viewModel.setEvent(
                                    ConversationUIState.Event.ResetAll
                                )
                            },
                            onExportJson = {
                                viewModel.setEvent(
                                    ConversationUIState.Event.ExportConversation(ExportFormat.JSON)
                                )
                            },
                            onExportPdf = {
                                viewModel.setEvent(
                                    ConversationUIState.Event.ExportConversation(ExportFormat.PDF)
                                )
                            }
                        )
                    }
                )
            },
            bottomBar = {
                Column {
                    var text by remember { mutableStateOf("") }

                    if (state.isConversationComplete) {
                        ConversationCompleteCard(
                            onRestart = {
                                viewModel.setEvent(
                                    ConversationUIState.Event.ResetAll
                                )
                            }
                        )
                    }

                    // RAG панель управления
                    // Показываем только если НЕ в режиме Committee
                    if (state.selectedMode != ConversationMode.COMMITTEE) {
                        RagControlPanel(
                            isRagEnabled = state.isRagEnabled,
                            indexedCount = state.ragIndexedCount,
                            settings = RagSettings(
                                threshold = state.ragThreshold,
                                topK = state.ragTopK,
                                useMmr = state.ragUseMmr,
                                mmrLambda = state.ragMmrLambda
                            ),
                            onToggleRag = { enabled ->
                                viewModel.setEvent(
                                    ConversationUIState.Event.ToggleRag(enabled)
                                )
                            },
                            onAddKnowledge = {
                                viewModel.setEvent(
                                    ConversationUIState.Event.ShowKnowledgeBaseDialog
                                )
                            },
                            onClearKnowledge = {
                                viewModel.setEvent(
                                    ConversationUIState.Event.ClearKnowledgeBase
                                )
                            },
                            onThresholdChange = { threshold ->
                                viewModel.setEvent(
                                    ConversationUIState.Event.SetRagThreshold(threshold)
                                )
                            },
                            onTopKChange = { topK ->
                                viewModel.setEvent(
                                    ConversationUIState.Event.SetRagTopK(topK)
                                )
                            },
                            onToggleMmr = { enabled ->
                                viewModel.setEvent(
                                    ConversationUIState.Event.ToggleRagMmr(enabled)
                                )
                            },
                            onMmrLambdaChange = { lambda ->
                                viewModel.setEvent(
                                    ConversationUIState.Event.SetRagMmrLambda(lambda)
                                )
                            },
                            enabled = !state.isLoading
                        )
                    }

                    // Диалог добавления знаний
                    if (state.showKnowledgeBaseDialog) {
                        KnowledgeBaseDialog(
                            onDismiss = {
                                viewModel.setEvent(
                                    ConversationUIState.Event.HideKnowledgeBaseDialog
                                )
                            },
                            onAddKnowledge = { text, sourceId ->
                                viewModel.setEvent(
                                    ConversationUIState.Event.AddToKnowledgeBase(text, sourceId)
                                )
                            }
                        )
                    }

                    InputBar(
                        isLoading = state.isLoading,
                        onSendMessage = {
                            viewModel.setEvent(
                                ConversationUIState.Event.SendMessage(it)
                            )
                        },
                        onSettingsClick = { onNavigateToOptions.invoke(viewModel.conversationId) },
                        text = text,
                        onTextChange = { text = it }
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LlmAgentTheme.colors.background)
                    .padding(paddingValues)
            ) {

                // Показываем выбор экспертов только в режиме Committee
                if (state.selectedMode == ConversationMode.COMMITTEE) {
                    ExpertsSelectionPanel(
                        selectedExperts = committeeState.selectedExperts,
                        availableExperts = committeeState.availableExperts,
                        onToggleExpert = { expert ->
                            committeeViewModel.onEvent(
                                CommitteeEvent.ToggleExpert(expert)
                            )
                        },
                        enabled = !state.isLoading
                    )
                }

                // MCP панель управления инструментами
                // Показываем только если есть инструменты или MCP включен
                // И НЕ в режиме Committee (в Committee режиме MCP не используется)
                if ((mcpState.availableTools.isNotEmpty() || mcpState.isEnabled)
                    && state.selectedMode != ConversationMode.COMMITTEE) {
                    McpToolsPanel(
                        viewModel = mcpViewModel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        onToolExecute = { toolName, description ->
                            // Вызываем выполнение инструмента через LLM
                            viewModel.executeToolWithLlm(toolName, description)
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    MessagesContent(
                        modifier = Modifier.padding(top = 8.dp),
                        messages = state.messages,
                        error = state.error,
                        isLoading = state.isLoading,
                        mcpToolExecution = mcpState.currentExecution,
                        onClearError = {
                            viewModel.setEvent(
                                ConversationUIState.Event.ClearError
                            )
                        }
                    )
                }
            }
        }
    }
}

/**
 * Карточка завершения диалога
 */
@Composable
private fun ConversationCompleteCard(onRestart: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "✅ Диалог завершен!",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            TextButton(onClick = onRestart) {
                Text("Начать заново")
            }
        }
    }
}

/**
 * Контент со списком сообщений
 */
@Composable
private fun BoxScope.MessagesContent(
    modifier: Modifier = Modifier,
    messages: List<ConversationMessage>,
    onClearError: () -> Unit,
    error: String,
    isLoading: Boolean,
    mcpToolExecution: ru.llm.agent.mcp.model.McpToolExecutionStatus? = null
) {
    val messagesListState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            delay(100)
            messagesListState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp).padding(horizontal = 8.dp),
        state = messagesListState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(
            count = messages.size,
            key = { index -> messages[index].id }
        ) { index ->
            MessageItem(messages[index])
        }

        // Индикатор выполнения MCP инструмента
        if (mcpToolExecution != null) {
            item(key = "mcp_execution_${mcpToolExecution.toolName}") {
                ru.llm.agent.mcp.presentation.ui.McpToolExecutionStatusIndicator(
                    status = mcpToolExecution,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        if (isLoading) {
            item {
                LoadingIndicator()
            }
        }
    }

    if (error.isNotEmpty()) {
        ErrorSnackbar(
            error = error,
            onClearError = onClearError
        )
    }
}

/**
 * Индикатор загрузки
 */
@Composable
private fun LoadingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Text("Думаю...")
            }
        }
    }
}

/**
 * Snackbar для отображения ошибок
 */
@Composable
private fun BoxScope.ErrorSnackbar(
    error: String,
    onClearError: () -> Unit,
) {
    Snackbar(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp),
        action = {
            TextButton(onClick = onClearError) {
                Text("OK")
            }
        },
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Text(error)
    }
}