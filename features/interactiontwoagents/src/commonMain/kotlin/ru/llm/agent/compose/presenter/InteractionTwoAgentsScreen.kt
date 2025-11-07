package ru.llm.agent.compose.presenter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.module.rememberKoinModules
import org.koin.compose.scope.KoinScope
import org.koin.compose.viewmodel.koinViewModel
import ru.llm.agent.compose.di.INTERACTION_TWO_AGENTS_SCOPE_ID
import ru.llm.agent.compose.di.interactionTwoAgentsKoinModule
import ru.llm.agent.compose.di.interactionTwoAgentsScopeQualifier
import ru.llm.agent.core.uikit.LlmAgentTheme
import ru.llm.agent.model.MessageModel
import ru.llm.agent.model.Role
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractionTwoAgentsScreen() {
    KoinScope(INTERACTION_TWO_AGENTS_SCOPE_ID, interactionTwoAgentsScopeQualifier) {
        rememberKoinModules {
            listOf(interactionTwoAgentsKoinModule())
        }
        val viewModel = koinViewModel() as InteractionTwoAgentsViewModel
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
                )
            },
            bottomBar = {
                Column {
                    BottomBar(
                        isLoading = state.isLoading,
                        onSendMessage = {
                            viewModel.setEvent(InteractionTwoAgentsUIState.Event.SendMessage(it))
                        }
                    )
                }

            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LlmAgentTheme.colors.primary)
                    .padding(paddingValues),
                contentAlignment = Alignment.BottomCenter,
            ) {
                MessagesContent(
                    modifier = Modifier.padding(top = 8.dp),
                    firstMessage = state.agentFirstMessage,
                    secondMessage = state.agentSecondMessage,
                    error = state.error,
                    isLoading = state.isLoading,
                )
            }
        }
    }
}

@Composable
private fun BoxScope.MessagesContent(
    modifier: Modifier = Modifier,
    firstMessage: MessageModel.ResponseMessage?,
    secondMessage: MessageModel.ResponseMessage?,
    error: String,
    isLoading: Boolean
) {
    val messagesListState = rememberLazyListState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp).padding(horizontal = 8.dp),
        state = messagesListState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        item {
            firstMessage?.let {
                MessageItem(
                    model = "Первый агент: gpt-4o-mini",
                    message = it
                )
            }
        }

        item {
            secondMessage?.let {
                MessageItem(
                    model = "Вторрой агент: yandexgpt-lite",
                    message = it
                )
            }
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
}

@Composable
fun MessageItem(
    model: String,
    message: MessageModel.ResponseMessage
) {
    val isUser = message.role == Role.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 600.dp),
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
                    text = model,
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (isUser)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Результат:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun BottomBar(
    isLoading: Boolean,
    onSendMessage: (String) -> Unit,
) {
    Row(
        modifier = Modifier.padding(8.dp).height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var text by remember { mutableStateOf("") }
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
        Button(
            onClick = { onSendMessage.invoke(text) },
            modifier = Modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF222222), contentColor = Color.White
            ),
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(0.dp),
            enabled = !isLoading
        ) {
            Text(
                "-->", fontSize = 24.sp
            )
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


