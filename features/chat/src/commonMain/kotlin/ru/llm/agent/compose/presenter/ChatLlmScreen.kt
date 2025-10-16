package ru.llm.agent.compose.presenter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.module.rememberKoinModules
import org.koin.compose.scope.KoinScope
import org.koin.compose.viewmodel.koinViewModel
import ru.llm.agent.compose.di.LLM_CHAT_SCOPE_ID
import ru.llm.agent.compose.di.llmChatScopeQualifier
import ru.llm.agent.compose.di.llmKoinModule
import ru.llm.agent.compose.presenter.model.MessageTypeUI
import ru.llm.agent.core.uikit.LlmAgentTheme
import ru.llm.agent.model.MessageModel

@Composable
fun ChatScreen() {
    KoinScope(LLM_CHAT_SCOPE_ID, llmChatScopeQualifier) {
        rememberKoinModules {
            listOf(llmKoinModule())
        }
        val viewModel = koinViewModel() as ChatLlmViewModel
        val state by viewModel.screeState.collectAsStateWithLifecycle()

        Scaffold(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopBar(
                    selectedAIType = state.selectedAIType,
                    onAiTypeSelected = { viewModel.setEvent(ChatLlmContract.Event.SelectAIType(it)) },
                )
            },
            bottomBar = {
                BottomBar(
                    isLoading = state.isLoading,
                    onSendMessage = { viewModel.setEvent(ChatLlmContract.Event.SendMessage(it)) })
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
                    messages = state.messages,
                    error = state.error,
                    isLoading = state.isLoading
                )
            }
        }
    }
}

@Composable
private fun MessagesContent(
    modifier: Modifier = Modifier, messages: List<MessageTypeUI>, error: String, isLoading: Boolean
) {
    val messagesListState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            delay(100)
            messagesListState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier.then(
            Modifier.fillMaxSize().padding(horizontal = 16.dp)
        )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = messagesListState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(items = messages) { messageTypeUi ->
                when (messageTypeUi) {
                    is MessageTypeUI.MyMessageUI -> {
                        MyMessageBubble(messageTypeUi.message)
                    }

                    is MessageTypeUI.TheirMessageUI -> {
                        AiMessageBubble(messageTypeUi.message)
                    }

                    is MessageTypeUI.DateSeparatorUI -> Unit
                }
            }
        }

        if (error.isNotEmpty()) {
            Text(
                modifier = Modifier.padding(bottom = 4.dp),
                text = "Ошибка: $error",
                color = MaterialTheme.colorScheme.error
            )
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier
                .align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun AiMessageBubble(message: MessageModel) = Box(
    modifier = Modifier
        .border(
            width = 2.dp,
            color = Color(0xFFEDEDED),
            shape = RoundedCornerShape(24.dp)
        )
        .background(Color.White, shape = RoundedCornerShape(24.dp))
        .padding(vertical = 24.dp, horizontal = 20.dp), contentAlignment = Alignment.TopEnd
) {
    Text(
        text = message.content,
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF222222),
    )
}

@Composable
private fun MyMessageBubble(message: MessageModel) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(
            vertical = 8.dp,
            horizontal = 12.dp
        ),
    horizontalArrangement = Arrangement.End
) {
    Box(
        modifier = Modifier.border(
            width = 2.dp,
            color = LlmAgentTheme.colors.outline,
            shape = RoundedCornerShape(24.dp)
        ).background(LlmAgentTheme.colors.secondaryContainer, shape = RoundedCornerShape(24.dp))
            .padding(vertical = 8.dp, horizontal = 12.dp),
    ) {
        Text(
            text = message.content,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF222222),
        )
    }
}

@Composable
private fun TopBar(
    selectedAIType: AiType,
    onAiTypeSelected: (AiType) -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    horizontalArrangement = Arrangement.Center,
) {
    SelectLlmDropDown(selectedAiType = selectedAIType) {
        if (it != selectedAIType) {
            onAiTypeSelected(it)
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
            modifier = Modifier.size(80.dp),
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

@Composable
fun SelectLlmDropDown(
    selectedAiType: AiType, onApiSelected: (AiType) -> Unit
) {
    val expanded = remember { mutableStateOf(false) }

    Box {
        Text(
            modifier = Modifier.padding(top = 8.dp).clickable { expanded.value = true },
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            text = selectedAiType.displayName
        )

        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false },
            modifier = Modifier.background(Color(0xFF232533), shape = RoundedCornerShape(16.dp))
                .shadow(8.dp, RoundedCornerShape(20.dp))
        ) {
            AiType.values().forEach { aiType ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = aiType.displayName,
                            color = Color.Green,
                            modifier = Modifier.padding(4.dp)
                        )
                    }, onClick = {
                        onApiSelected(aiType)
                        expanded.value = false
                    }, modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Preview()
@Composable
private fun PreviewScreen() {
    val state = ChatLlmContract.State.empty().copy(
        workMode = WorkMode.SINGLE, messages = listOf(
            MessageTypeUI.MyMessageUI(MessageModel("Hello", "Привет", "12:00")),
            MessageTypeUI.TheirMessageUI(
                MessageModel(
                    "Hello",
                    "Я ассистент, Чем могу помочьfghfghjfgjfgjfghjfghjfghjfghbjdghjyghjkrtuy7k6uyk?",
                    "12:00"
                )
            ),
        )
    )
    Scaffold(topBar = {
        TopBar(
            selectedAIType = state.selectedAIType,
            onAiTypeSelected = { },
        )
    }, bottomBar = {
        BottomBar(
            isLoading = state.isLoading, onSendMessage = {})
    }) { paddingValues ->
        MessagesContent(
            modifier = Modifier.padding(paddingValues),
            messages = state.messages,
            error = state.error,
            isLoading = state.isLoading
        )
    }
}

