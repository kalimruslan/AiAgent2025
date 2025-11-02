package ru.llm.agent.compose.presenter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.module.rememberKoinModules
import org.koin.compose.scope.KoinScope
import org.koin.compose.viewmodel.koinViewModel
import ru.llm.agent.compose.di.MCP_CLIENT_SCOPE_ID
import ru.llm.agent.compose.di.mcpClientKoinModule
import ru.llm.agent.compose.di.mcpClientScopeQualifier
import ru.llm.agent.core.uikit.LlmAgentTheme
import ru.llm.agent.model.MessageModel
import ru.llm.agent.model.mcp.YaGptTool

/**
 * Отпроавить в LLM инструменты из MCP сервера
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McpClientScreen() {
    KoinScope(MCP_CLIENT_SCOPE_ID, mcpClientScopeQualifier) {
        rememberKoinModules {
            listOf(mcpClientKoinModule())
        }
        val viewModel = koinViewModel() as McpViewModel
        val uiState by viewModel.uiState.collectAsState()

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
                    messages = uiState.messages,
                    toolsCount = uiState.mcpTools
                )
            }
        }
    }
}

@Composable
private fun MessagesContent(
    modifier: Modifier = Modifier,
    messages: List<MessageModel>,
    toolsCount: List<YaGptTool>,
) {
    val messagesListState = rememberLazyListState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp).padding(horizontal = 8.dp),
        state = messagesListState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {

        item{
            Text("Tools: ${toolsCount.size}")
        }

        items(messages) {
            MessageItem(
                message = it,
            )
        }
    }
}

@Composable
fun MessageItem(
    message: MessageModel,
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


