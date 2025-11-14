package ru.llm.agent.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
import ru.llm.agent.model.Expert
import ru.llm.agent.model.LlmProvider
import ru.llm.agent.model.Role
import ru.llm.agent.model.conversation.ConversationMessage
import ru.llm.agent.presentation.ui.components.InputBar
import ru.llm.agent.presentation.ui.components.MessageItem
import ru.llm.agent.presentation.ui.components.TokenUsageProgressBar
import kotlin.time.Instant

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
                        TextButton(onClick = {
                            viewModel.setEvent(
                                ConversationUIState.Event.ResetAll
                            )
                        }) {
                            Text("Очистить все")
                        }
                    }
                )
            },
            bottomBar = {
                Column {
                    if (state.isConversationComplete) {
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
                                TextButton(onClick = {
                                    viewModel.setEvent(
                                        ConversationUIState.Event.ResetAll
                                    )
                                }) {
                                    Text("Начать заново")
                                }
                            }
                        }
                    }
                    InputBar(
                        isLoading = state.isLoading,
                        onSendMessage = {
                            viewModel.setEvent(
                                ConversationUIState.Event.SendMessage(
                                    it
                                )
                            )
                        },
                        onSettingsClick = { onNavigateToOptions.invoke(viewModel.conversationId) }
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

        items(items = messages, key = { it.id }) { message ->
            MessageItem(message)
        }

        if (isLoading) {
            item {
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
        }
    }

    if (error.isNotEmpty()) {
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
}

@Composable
fun MessageItem(message: ConversationMessage) {
    val isUser = message.role == Role.USER
    var showOriginalJson by remember { mutableStateOf(false) }
    var showMetadata by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        if(!isUser) {
            Text("Model: ${message.model}")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            Card(
                modifier = Modifier.widthIn(max = 400.dp),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.secondaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isUser)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    // Показать метаданные (токены и время) если есть
                    if (!isUser && hasMetadata(message)) {
                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { showMetadata = !showMetadata },
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Text(
                                text = if (showMetadata) "Скрыть статистику" else "Показать статистику",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        if (showMetadata) {
                            Spacer(modifier = Modifier.height(4.dp))
                            MetadataCard(message)
                        }
                    }

                    // Show original JSON response if available (for assistant messages)
                    if (!isUser && message.originalResponse != null) {
                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { showOriginalJson = !showOriginalJson },
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Text(
                                text = if (showOriginalJson) "Скрыть JSON" else "Показать оригинальный JSON",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        if (showOriginalJson) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Text(
                                    text = message.originalResponse.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(8.dp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser)
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Отображаем мнения экспертов (если есть)
        if (isUser && message.expertOpinions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                message.expertOpinions.forEach { opinion ->
                    ExpertOpinionCard(opinion)
                }
            }
        }
    }
}

/**
 * Карточка с мнением эксперта
 */
@Composable
fun ExpertOpinionCard(opinion: ru.llm.agent.model.ExpertOpinion) {
    var showOriginalJson by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Spacer(modifier = Modifier.width(24.dp)) // Отступ слева для визуального отличия
        Card(
            modifier = Modifier.widthIn(max = 380.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Заголовок с иконкой и именем эксперта
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = opinion.expertIcon,
                        fontSize = 16.sp
                    )
                    Text(
                        text = opinion.expertName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Текст мнения
                Text(
                    text = opinion.opinion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontSize = 14.sp
                )

                // Показать оригинальный JSON (если есть)
                if (opinion.originalResponse != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = { showOriginalJson = !showOriginalJson },
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Text(
                            text = if (showOriginalJson) "Скрыть JSON" else "Показать JSON",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }

                    if (showOriginalJson) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(
                                text = opinion.originalResponse.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(6.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Временная метка
                Text(
                    text = formatTimestamp(opinion.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun BottomBar(
    isLoading: Boolean,
    onSendMessage: (String) -> Unit,
    onSettingsClick: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(8.dp).height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var text by remember { mutableStateOf("") }
        IconButton(
            onClick = onSettingsClick,
            enabled = !isLoading
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = LlmAgentTheme.colors.onBackground)
        }
        TextField(
            value = text,
            onValueChange = {
                text = it
            },
            placeholder = {
                Text(
                    "Ваше сообщение", color = LlmAgentTheme.colors.onSurface, fontSize = 16.sp
                )
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.weight(1f).fillMaxHeight().border(
                width = 2.dp, color = Color(0xFFE0E0E0), shape = RoundedCornerShape(16.dp)
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp)
        )
        Spacer(Modifier.width(8.dp))

        IconButton(
            onClick = {
                onSendMessage.invoke(text)
            },
            enabled = !isLoading
        ) {
            Icon(modifier = Modifier.size(48.dp), imageVector = Icons.Filled.Send, contentDescription = null, tint = LlmAgentTheme.colors.onBackground)
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dateTime.hour.toString().padStart(2, '0')}:${
        dateTime.minute.toString().padStart(2, '0')
    }"
}

/**
 * Dropdown для выбора LLM провайдера
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LlmProviderDropdown(
    selectedProvider: LlmProvider,
    onProviderSelected: (LlmProvider) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.wrapContentSize()) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            Surface(
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled)
                    .wrapContentSize()
                    .padding(start = 12.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                onClick = { if (enabled) expanded = !expanded }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = selectedProvider.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 12.sp
                    )
                    Icon(
                        imageVector = if (expanded)
                            Icons.Default.KeyboardArrowUp
                        else
                            Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                LlmProvider.entries.forEach { provider ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                provider.displayName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = {
                            onProviderSelected(provider)
                            expanded = false
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Dropdown для выбора режима работы (Single AI / Committee)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationModeDropdown(
    selectedMode: ConversationMode,
    onModeSelected: (ConversationMode) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.wrapContentSize()) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            Surface(
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled)
                    .wrapContentSize(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                onClick = { if (enabled) expanded = !expanded }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = selectedMode.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontSize = 12.sp
                    )
                    Icon(
                        imageVector = if (expanded)
                            Icons.Default.KeyboardArrowUp
                        else
                            Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                ConversationMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    mode.displayName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    mode.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            onModeSelected(mode)
                            expanded = false
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Панель выбора экспертов для режима Committee
 */
@Composable
fun ExpertsSelectionPanel(
    selectedExperts: List<Expert>,
    availableExperts: List<Expert>,
    onToggleExpert: (Expert) -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Выбранные эксперты (${selectedExperts.size}):",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableExperts) { expert ->
                    val isSelected = selectedExperts.contains(expert)
                    ExpertChip(
                        expert = expert,
                        isSelected = isSelected,
                        onClick = { onToggleExpert(expert) },
                        enabled = enabled
                    )
                }
            }
        }
    }
}

/**
 * Chip для отображения эксперта
 */
@Composable
fun ExpertChip(
    expert: Expert,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Surface(
        onClick = { if (enabled) onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surface,
        border = if (isSelected)
            null
        else
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.wrapContentSize()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = expert.icon,
                fontSize = 18.sp
            )
            Column {
                Text(
                    text = expert.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp
                )
                Text(
                    text = expert.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Проверка наличия метаданных у сообщения
 */
fun hasMetadata(message: ConversationMessage): Boolean {
    return message.totalTokens != null ||
           message.inputTokens != null ||
           message.completionTokens != null ||
           message.responseTimeMs != null
}

/**
 * Карточка с метаданными (токены и время)
 */
@Composable
fun MetadataCard(message: ConversationMessage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "📊 Статистика ответа",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            if (message.inputTokens != null) {
                MetadataRow(
                    label = "Входные токены:",
                    value = "${message.inputTokens}"
                )
            }

            if (message.completionTokens != null) {
                MetadataRow(
                    label = "Токены ответа:",
                    value = "${message.completionTokens}"
                )
            }

            if (message.totalTokens != null) {
                MetadataRow(
                    label = "Всего токенов:",
                    value = "${message.totalTokens}",
                    isBold = true
                )
            }

            message.responseTimeMs?.let { responseTime ->
                MetadataRow(
                    label = "Время ответа:",
                    value = formatResponseTime(responseTime)
                )
            }
        }
    }
}

/**
 * Строка с меткой и значением
 */
@Composable
fun MetadataRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isBold) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
        )
    }
}

/**
 * Форматирование времени ответа
 */
fun formatResponseTime(milliseconds: Long): String {
    return when {
        milliseconds < 1000 -> "${milliseconds} мс"
        milliseconds < 60000 -> String.format("%.1f сек", milliseconds / 1000.0)
        else -> {
            val minutes = milliseconds / 60000
            val seconds = (milliseconds % 60000) / 1000
            "${minutes} мин ${seconds} сек"
        }
    }
}

/**
 * ProgressBar для отображения использования токенов
 */
@Composable
fun TokenUsageProgressBar(
    usedTokens: Int,
    maxTokens: Int,
    requestTokens: Int?,
    summarizationInfo: ru.llm.agent.model.SummarizationInfo?,
    isSummarizing: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = if (maxTokens > 0) usedTokens.toFloat() / maxTokens.toFloat() else 0f
    val progressClamped = progress.coerceIn(0f, 1f)

    // Определяем цвет в зависимости от использования
    val progressColor = when {
        progressClamped < 0.5f -> MaterialTheme.colorScheme.primary
        progressClamped < 0.8f -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Использование токенов",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$usedTokens / $maxTokens",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }

            // Отображаем токены текущего запроса, если они подсчитаны
            if (requestTokens != null && requestTokens > 0) {
                Text(
                    text = "Текущий запрос: ~$requestTokens токенов",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
            }

            LinearProgressIndicator(
                progress = { progressClamped },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            // Индикатор процесса суммаризации
            if (isSummarizing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "⏳ Сжатие истории...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                }
            }

            // Информация о суммаризации
            if (!isSummarizing && summarizationInfo != null && summarizationInfo.hasSummarizedMessages) {
                Text(
                    text = "📝 История сжата: ${summarizationInfo.summarizedMessagesCount} сообщений (сохранено ~${summarizationInfo.savedTokens} токенов)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CAF50), // Зеленый цвет
                    fontSize = 11.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
            }

            // Предупреждение, если токены заканчиваются
            if (!isSummarizing && progressClamped > 0.8f) {
                Text(
                    text = "⚠️ Токены заканчиваются",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp
                )
            }
        }
    }
}


