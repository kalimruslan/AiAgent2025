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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.module.rememberKoinModules
import org.koin.compose.scope.KoinScope
import org.koin.compose.viewmodel.koinViewModel
import ru.llm.agent.compose.di.Tokens_SCOPE_ID
import ru.llm.agent.compose.di.tokensKoinModule
import ru.llm.agent.compose.di.tokensScopeQualifier
import ru.llm.agent.core.uikit.LlmAgentTheme
import ru.llm.agent.model.MessageModel
import ru.llm.agent.model.Role
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokensScreen() {
    KoinScope(Tokens_SCOPE_ID, tokensScopeQualifier) {
        rememberKoinModules {
            listOf(tokensKoinModule())
        }
        val viewModel = koinViewModel() as TokensViewModel
        val state by viewModel.screenState.collectAsStateWithLifecycle()

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
                        isLoading = false,
                        onSendMessage = {
                            viewModel.sendMessage(it)
                        }
                    )
                }

            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LlmAgentTheme.colors.primary)
                    .padding(paddingValues),
            ) {
                MessagesContent(
                    modifier = Modifier.padding(top = 8.dp),
                    messages = state.messages,
                    maxTokens = viewModel.maxTokens
                )
            }
        }
    }
}

@Composable
private fun MessagesContent(
    modifier: Modifier = Modifier,
    messages: List<MessageModel>,
    maxTokens: Int,
) {
    val messagesListState = rememberLazyListState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp).padding(horizontal = 8.dp),
        state = messagesListState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {

        items(messages) {
            MessageItem(
                message = it,
                maxTokens = maxTokens
            )
        }
    }
}

@Composable
fun MessageItem(
    message: MessageModel,
    maxTokens: Int,
) {
    val isUser = message is MessageModel.UserMessage
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
                val tokenCount = when (message) {
                    is MessageModel.UserMessage -> message.inputTokens
                    is MessageModel.ResponseMessage -> message.tokenUsed.toInt()
                    else -> 0
                }
                TokenCounter(
                    tokenCount,
                    if (message is MessageModel.UserMessage) maxTokens else 1000
                )
                if (message is MessageModel.UserMessage) {
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = "Запрос сжат - ${message.isCompressed}",
                        fontWeight = FontWeight.Bold
                    )
                    if (message.isCompressed) {
                        Text(
                            modifier = Modifier.padding(top = 8.dp),
                            text = "Не в сжатом виде - ${message.notCompressedTokens} токенов",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isUser)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

// TokenCounterComposable.kt
@Composable
fun TokenCounter(tokenCount: Int, maxTokens: Int = 30) {
    val percentage = (tokenCount.toFloat() / maxTokens * 100).toInt()
    val color = when {
        percentage < 50 -> Color.Yellow
        percentage < 80 -> Color(0xFFFFA500) // Orange
        else -> Color.Red
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Токены: $tokenCount / $maxTokens",
            style = MaterialTheme.typography.bodySmall,
            color = color
        )

        LinearProgressIndicator(
            progress = percentage / 100f,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color
        )
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


