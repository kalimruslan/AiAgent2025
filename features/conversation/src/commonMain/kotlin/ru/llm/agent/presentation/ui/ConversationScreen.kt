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
import ru.llm.agent.presentation.ui.components.ToolExecutionIndicator
import ru.llm.agent.presentation.ui.components.BoardSummaryCard
import ru.llm.agent.presentation.ui.components.SmartPromptsBar
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
        viewModel.start()
        val state by viewModel.screeState.collectAsStateWithLifecycle()

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

                    McpToolsCheckbox(
                        isUsedMcpTools = state.isUsedMcpTools,
                        onToggle = { useTools ->
                            viewModel.setEvent(
                                ConversationUIState.Event.SwitchNeedMcpTools(useTools)
                            )
                        },
                        enabled = !state.isLoading,
                        boardId = state.trelloBoardId,
                        onSetBoardId = { newBoardId ->
                            viewModel.setEvent(
                                ConversationUIState.Event.SetTrelloBoardId(newBoardId)
                            )
                        }
                    )

                    // Умные промпты для Trello (показываем только когда MCP инструменты включены)
                    if (state.isUsedMcpTools) {
                        SmartPromptsBar(
                            onPromptClick = { prompt ->
                                viewModel.setEvent(
                                    ConversationUIState.Event.SendMessage(prompt)
                                )
                            },
                            enabled = !state.isLoading,
                            boardId = state.trelloBoardId,
                            text = text
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
                if (!state.isUsedMcpTools) {
                    TokenUsageProgressBar(
                        usedTokens = state.usedTokens,
                        maxTokens = state.maxTokens,
                        requestTokens = state.requestTokens,
                        summarizationInfo = state.summarizationInfo,
                        isSummarizing = state.isSummarizing
                    )
                }

                // Карточка с саммари доски Trello (если есть и MCP инструменты включены)
                if (state.isUsedMcpTools) {
                    state.boardSummary?.let { summary ->
                        BoardSummaryCard(boardSummary = summary)
                    }
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
                        currentToolExecution = state.currentToolExecution,
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
 * Чекбокс для переключения использования MCP инструментов
 */
@Composable
private fun McpToolsCheckbox(
    isUsedMcpTools: Boolean,
    onToggle: (Boolean) -> Unit,
    enabled: Boolean = true,
    boardId: String? = null,
    onSetBoardId: (String?) -> Unit = {},
) {
    var showBoardIdDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Использовать инструменты MCP",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Checkbox(
                    checked = isUsedMcpTools,
                    onCheckedChange = { if (enabled) onToggle(it) },
                    enabled = enabled
                )
            }

            // Кнопка для настройки Board ID (показываем только когда MCP включен)
            if (isUsedMcpTools) {
                TextButton(
                    onClick = { showBoardIdDialog = true },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = if (boardId != null) {
                            "Board ID: ${boardId.take(12)}..."
                        } else {
                            "⚙️ Настроить Board ID"
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }

    // Диалог для ввода Board ID
    if (showBoardIdDialog) {
        BoardIdDialog(
            currentBoardId = boardId,
            onDismiss = { showBoardIdDialog = false },
            onConfirm = { newBoardId ->
                onSetBoardId(newBoardId)
                showBoardIdDialog = false
            }
        )
    }
}

/**
 * Диалог для настройки Board ID Trello
 */
@Composable
private fun BoardIdDialog(
    currentBoardId: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
) {
    var boardId by remember { mutableStateOf(currentBoardId ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Настройка Trello Board ID") },
        text = {
            Column {
                Text(
                    text = "Введите ID вашей доски Trello для автоматической подстановки в промпты:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = boardId,
                    onValueChange = { boardId = it },
                    label = { Text("Board ID") },
                    placeholder = { Text("например: 5f8a1b2c3d4e5f6g7h8i9j0k") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Найти Board ID можно в URL доски Trello после /b/",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(boardId.ifBlank { null }) }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
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
    currentToolExecution: ConversationUIState.ToolExecutionStatus? = null,
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

        // Показываем индикатор выполнения tool (если есть)
        if (currentToolExecution != null) {
            item {
                ToolExecutionIndicator(
                    toolStatus = currentToolExecution,
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