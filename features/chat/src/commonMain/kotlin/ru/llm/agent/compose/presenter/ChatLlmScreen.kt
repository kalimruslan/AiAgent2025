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
import ru.llm.agent.core.uikit.AdventAITheme
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
            topBar = {
                TopBar(
                    selectedAIType = state.selectedAIType,
                    onAiTypeSelected = { viewModel.setEvent(ChatLlmContract.Event.SelectAIType(it)) },
                    onProxyModelSelect = {
                        viewModel.setEvent(
                            ChatLlmContract.Event.SelectProxyAiModel(
                                it
                            )
                        )
                    }
                )
            },
            bottomBar = {
                BottomBar(
                    isLoading = state.isLoading,
                    onSendMessage = { viewModel.setEvent(ChatLlmContract.Event.SendMessage(it)) }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding() // <- Здесь обрабатываем imePadding
            ) {
                MessagesContent(
                    modifier = Modifier.padding(paddingValues),
                    state = state,
                )
            }
        }
    }
}

@Composable
private fun MessagesContent(
    modifier: Modifier = Modifier,
    state: ChatLlmContract.State,
) = with(state) {
    val messagesListState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            // Добавляем небольшую задержку, чтобы убедиться, что список уже отрисован
            delay(100)
            // Прокручиваем к последнему сообщению
            messagesListState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier.then(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        )
    ) {

        // История сообщений
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = messagesListState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(
                items = messages,
                contentType = { "message" }
            ) { message ->
                MessageItem(message = message)
            }
        }

        // Ошибки
        error?.let {
            Text(
                modifier = Modifier.padding(bottom = 4.dp),
                text = "Ошибка: $it",
                color = MaterialTheme.colorScheme.error
            )
        }

        // Индикатор загрузки
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun TopBar(
    selectedAIType: AiType,
    onAiTypeSelected: (AiType) -> Unit,
    onProxyModelSelect: (AiType.ProxyAI.ProxyAIModel) -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    horizontalArrangement = Arrangement.SpaceBetween
) {
    SelectLlmDropDown(selectedAiType = selectedAIType) {
        if (it != selectedAIType) {
            onAiTypeSelected(it)
        }
    }

    if (selectedAIType is AiType.ProxyAI) {
        SelectProxyModelsDropdown(
            allModels = AiType.ProxyAI.ProxyAIModel.entries,
            selectedModel = selectedAIType.selectedModel,
        ) {
            if (it != selectedAIType.selectedModel) {
                onProxyModelSelect(it)
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
        modifier = Modifier
            .padding(8.dp)
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var text by remember { mutableStateOf("") }

        TextField(
            value = text,
            onValueChange = {
                text = it
            },
            label = {
                Text(
                    "Ваше сообщение",
                    color = AdventAITheme.colors.onSecondary,
                    fontSize = 16.sp
                )
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF7F7F7),
                unfocusedContainerColor = Color(0xFFF7F7F7),
                focusedBorderColor = Color(0xFFE0E0E0),
                unfocusedBorderColor = Color(0xFFE0E0E0),
                cursorColor = Color.Red,
                focusedTextColor = Color(0xFF333333),
                unfocusedTextColor = Color(0xFF333333),
            ),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .border(
                    width = 2.dp,
                    color = Color(0xFFE0E0E0),
                    shape = RoundedCornerShape(16.dp)
                ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp)
        )
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = { onSendMessage.invoke(text) },
            modifier = Modifier.size(80.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF222222),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(0.dp),
            enabled = !isLoading
        ) {
            Text(
                "-->",
                fontSize = 24.sp
            )
        }
    }
}

@Composable
fun SelectProxyModelsDropdown(
    allModels: List<AiType.ProxyAI.ProxyAIModel>,
    selectedModel: AiType.ProxyAI.ProxyAIModel,
    onModelSelect: (AiType.ProxyAI.ProxyAIModel) -> Unit
) {
    val expanded = remember { mutableStateOf(false) }

    Box {
        Text(
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable { expanded.value = true },
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            text = selectedModel.model
        )

        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false },
            modifier = Modifier
                .background(Color(0xFF232533), shape = RoundedCornerShape(16.dp))
                .shadow(8.dp, RoundedCornerShape(20.dp))
        ) {
            allModels.forEach { model ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = model.model,
                            color = Color.Green,
                            modifier = Modifier.padding(4.dp)
                        )
                    },
                    onClick = {
                        onModelSelect(model)
                        expanded.value = false
                    },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SelectLlmDropDown(
    selectedAiType: AiType,
    onApiSelected: (AiType) -> Unit
) {
    val expanded = remember { mutableStateOf(false) }

    Box {
        Text(
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable { expanded.value = true },
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            text = selectedAiType.displayName
        )

        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false },
            modifier = Modifier
                .background(Color(0xFF232533), shape = RoundedCornerShape(16.dp))
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
                    },
                    onClick = {
                        onApiSelected(aiType)
                        expanded.value = false
                    },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun MessageItem(message: MessageModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (message.role) {
                "user" -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Заголовок с переключателем
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.role.uppercase(),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val displayText = message.content
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Preview()
@Composable
private fun PreviewScreen() {
    val state = ChatLlmContract.State.empty().copy(workMode = WorkMode.SINGLE)
    Scaffold(
        topBar = {
            TopBar(
                selectedAIType = state.selectedAIType,
                onAiTypeSelected = { },
                onProxyModelSelect = {}
            )
        },
        bottomBar = {
            BottomBar(
                isLoading = state.isLoading,
                onSendMessage = {}
            )
        }
    ) { paddingValues ->
        MessagesContent(
            modifier = Modifier.padding(paddingValues),
            state = state,
        )
    }
}

