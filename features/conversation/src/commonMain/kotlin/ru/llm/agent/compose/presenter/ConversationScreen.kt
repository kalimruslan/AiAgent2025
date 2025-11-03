package ru.llm.agent.compose.presenter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.module.rememberKoinModules
import org.koin.compose.scope.KoinScope
import org.koin.compose.viewmodel.koinViewModel
import ru.llm.agent.compose.di.CONVERSATION_CHAT_SCOPE_ID
import ru.llm.agent.compose.di.conversationChatScopeQualifier
import ru.llm.agent.compose.di.conversationKoinModule
import ru.llm.agent.core.uikit.LlmAgentTheme
import ru.llm.agent.model.Role
import ru.llm.agent.model.conversation.ConversationMessage
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
                    title = { Text("AI Консультант") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    actions = {
                        TextButton(onClick = {
                            viewModel.setEvent(
                                ConversationUIState.Event.ResetConversation
                            )
                        }) {
                            Text("Начать заново")
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
                                        ConversationUIState.Event.ResetConversation
                                    )
                                }) {
                                    Text("Начать заново")
                                }
                            }
                        }
                    }
                    BottomBar(
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LlmAgentTheme.colors.background)
                    .padding(paddingValues),
                contentAlignment = Alignment.BottomCenter,
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


