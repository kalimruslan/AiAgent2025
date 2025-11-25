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
import ru.llm.agent.presentation.ui.components.TokenUsageProgressBar
import ru.llm.agent.presentation.ui.components.BoardSummaryCard
import ru.llm.agent.mcp.presentation.ui.McpToolsPanel
import ru.llm.agent.mcp.presentation.viewmodel.McpViewModel
import ru.llm.agent.presentation.ui.components.SmartPromptsBar
import ru.llm.agent.presentation.ui.components.RagControlPanel
import ru.llm.agent.presentation.ui.components.KnowledgeBaseDialog
import ru.llm.agent.presentation.ui.dropdowns.ConversationModeDropdown
import ru.llm.agent.presentation.ui.dropdowns.LlmProviderDropdown
import ru.llm.agent.presentation.ui.experts.ExpertsSelectionPanel
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
        val mcpViewModel = koinViewModel<McpViewModel>()
        viewModel.start()
        val state by viewModel.screeState.collectAsStateWithLifecycle()
        val mcpState by mcpViewModel.state.collectAsStateWithLifecycle()

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
                    RagControlPanel(
                        isRagEnabled = state.isRagEnabled,
                        indexedCount = state.ragIndexedCount,
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
                        enabled = !state.isLoading
                    )

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
                // Progress bar для токенов
                TokenUsageProgressBar(
                    usedTokens = state.usedTokens,
                    maxTokens = state.maxTokens,
                    requestTokens = state.requestTokens,
                    summarizationInfo = state.summarizationInfo,
                    isSummarizing = state.isSummarizing
                )

                // Карточка с саммари доски Trello
                state.boardSummary?.let { summary ->
                    BoardSummaryCard(boardSummary = summary)
                }

                // Показываем выбор экспертов только в режиме Committee
                if (state.selectedMode == ConversationMode.COMMITTEE) {
                    ExpertsSelectionPanel(
                        selectedExperts = state.selectedExperts,
                        availableExperts = state.availableExperts,
                        onToggleExpert = { expert ->
                            viewModel.setEvent(
                                ConversationUIState.Event.ToggleExpert(expert)
                            )
                        },
                        enabled = !state.isLoading
                    )
                }

                // MCP панель управления инструментами (показываем только если есть инструменты или MCP включен)
                if (mcpState.availableTools.isNotEmpty() || mcpState.isEnabled) {
                    McpToolsPanel(
                        viewModel = mcpViewModel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
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