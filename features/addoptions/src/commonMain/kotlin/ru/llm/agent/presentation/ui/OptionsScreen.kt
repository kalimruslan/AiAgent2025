package ru.llm.agent.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.module.rememberKoinModules
import org.koin.compose.scope.KoinScope
import org.koin.compose.viewmodel.koinViewModel
import ru.llm.agent.model.mcp.McpToolInfo
import ru.llm.agent.presentation.di.OPTIONS_SCOPE_ID
import ru.llm.agent.presentation.di.optionsKoinModule
import ru.llm.agent.presentation.di.optionsScopeQualifier
import ru.llm.agent.core.uikit.LlmAgentTheme
import ru.llm.agent.presentation.viewmodel.OptionsViewModel
import ru.llm.agent.presentation.state.OptionsUIState
import kotlin.time.Instant

/**
 * День 4. Температура
 *
 * Запустите один и тот же запрос с температурой = 0, 0.7 и 1.2
 *
 * - Сравните результаты (точность, креативность, разнообразие)
 * - Сформулируйте, для каких задач лучше подходит каждая настройка
 *
 * Результат: Текст или код с примерами разных ответов
 * Формат: Видео + Код
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsScreen(
    onNavigateBack: () -> Unit,
    conversationId: String
) {
    KoinScope(OPTIONS_SCOPE_ID, optionsScopeQualifier) {
        rememberKoinModules {
            listOf(optionsKoinModule())
        }
        val viewModel = koinViewModel() as OptionsViewModel
        val state by viewModel.screeState.collectAsStateWithLifecycle()
        viewModel.start("default_conversation")

        // Диалог добавления сервера
        if (state.showAddServerDialog) {
            AddServerDialog(
                onDismiss = { viewModel.setEvent(OptionsUIState.Event.HideAddServerDialog) },
                onAdd = { name, url, description ->
                    viewModel.setEvent(OptionsUIState.Event.AddServer(name, url, description))
                }
            )
        }

        Scaffold(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = { Text("Настройки параметров") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    actions = {
                        Button(
                            onClick = {
                                viewModel.setEvent(OptionsUIState.Event.ResetOptions)
                            }
                        ) {
                            Text("Очистить")
                        }
                    }
                )
            },
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LlmAgentTheme.colors.primary)
                    .padding(paddingValues),
                contentAlignment = Alignment.BottomCenter,
            ) {
                OptionsScreenContent(
                    modifier = Modifier.padding(top = 8.dp),
                    systemPrompt = state.systemPrompt.orEmpty(),
                    temperature = state.temperature.toString(),
                    tokens = state.maxTokens.toString(),
                    mcpTools = state.mcpTools,
                    isToolsLoading = state.isToolsLoading,
                    toolsError = state.toolsError,
                    isToolsSectionExpanded = state.isToolsSectionExpanded,
                    mcpServers = state.mcpServers,
                    isServersSectionExpanded = state.isServersSectionExpanded,
                    onApplyClick = { systemPrompt, temperature, tokens ->
                        viewModel.setEvent(
                            OptionsUIState.Event.ApplyClick(
                                navigateAction = onNavigateBack,
                                systemPrompt = systemPrompt,
                                temperature = temperature,
                                maxTokens = tokens
                            )
                        )
                    },
                    onLoadToolsClick = {
                        viewModel.setEvent(OptionsUIState.Event.LoadMcpTools)
                    },
                    onToggleToolsSection = {
                        viewModel.setEvent(OptionsUIState.Event.ToggleToolsSection)
                    },
                    onToggleServersSection = {
                        viewModel.setEvent(OptionsUIState.Event.ToggleServersSection)
                    },
                    onAddServer = {
                        viewModel.setEvent(OptionsUIState.Event.ShowAddServerDialog)
                    },
                    onDeleteServer = { serverId ->
                        viewModel.setEvent(OptionsUIState.Event.DeleteServer(serverId))
                    },
                    onToggleServerActive = { serverId, isActive ->
                        viewModel.setEvent(OptionsUIState.Event.ToggleServerActive(serverId, isActive))
                    }
                )
            }
        }
    }
}

@Composable
private fun BoxScope.OptionsScreenContent(
    modifier: Modifier = Modifier,
    systemPrompt: String,
    temperature: String,
    tokens: String,
    mcpTools: List<McpToolInfo>,
    isToolsLoading: Boolean,
    toolsError: String?,
    isToolsSectionExpanded: Boolean,
    mcpServers: List<ru.llm.agent.model.mcp.McpServer>,
    isServersSectionExpanded: Boolean,
    onApplyClick: (String, String, String) -> Unit,
    onLoadToolsClick: () -> Unit,
    onToggleToolsSection: () -> Unit,
    onToggleServersSection: () -> Unit,
    onAddServer: () -> Unit,
    onDeleteServer: (Long) -> Unit,
    onToggleServerActive: (Long, Boolean) -> Unit
) {

    var systemPromptInput by remember { mutableStateOf(systemPrompt) }
    var temperatureInput by remember { mutableStateOf(temperature) }
    var tokensInput by remember { mutableStateOf(tokens) }

    LaunchedEffect(systemPrompt) {
        systemPromptInput = systemPrompt
    }
    LaunchedEffect(temperature) {
        temperatureInput = temperature
    }
    LaunchedEffect(tokens) {
        tokensInput = tokens
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp).padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 64.dp)
    ) {
        item {
            TextField(
                value = systemPromptInput,
                onValueChange = { systemPromptInput = it },
                label = { Text("Системный промпт") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .heightIn(min = 96.dp),
                shape = RoundedCornerShape(8.dp)
            )
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }

        item {
            TextField(
                value = tokensInput,
                onValueChange = { tokensInput = it },
                label = { Text("Максимальное количество токенов") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                singleLine = true
            )
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }

        item {
            TextField(
                value = temperatureInput,
                onValueChange = { temperatureInput = it },
                label = { Text("Температура") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                singleLine = true
            )
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }

        // Секция MCP Серверов
        item {
            McpServersSection(
                servers = mcpServers,
                isExpanded = isServersSectionExpanded,
                onToggleExpand = onToggleServersSection,
                onAddServer = onAddServer,
                onDeleteServer = onDeleteServer,
                onToggleActive = onToggleServerActive
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Секция MCP Tools
        item {
            McpToolsSection(
                mcpTools = mcpTools,
                isLoading = isToolsLoading,
                error = toolsError,
                isExpanded = isToolsSectionExpanded,
                onLoadClick = onLoadToolsClick,
                onToggleExpand = onToggleToolsSection
            )
        }
    }

    Button(
        modifier = Modifier.align(Alignment.BottomCenter),
        onClick = {
            onApplyClick.invoke(systemPromptInput, temperatureInput, tokensInput)
        },
    ) {
        Text("Применить")
    }
}

@Composable
private fun McpToolsSection(
    mcpTools: List<McpToolInfo>,
    isLoading: Boolean,
    error: String?,
    isExpanded: Boolean,
    onLoadClick: () -> Unit,
    onToggleExpand: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MCP Инструменты",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isExpanded) "▲" else "▼",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))

                if (mcpTools.isEmpty() && !isLoading && error == null) {
                    Button(
                        onClick = onLoadClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Загрузить инструменты")
                    }
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                error?.let {
                    Text(
                        text = "Ошибка: $it",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onLoadClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Повторить")
                    }
                }

                mcpTools.forEach { tool ->
                    McpToolItem(tool = tool)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun McpToolItem(tool: McpToolInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = tool.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodyMedium
            )

            if (tool.parameters.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Параметры:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                tool.parameters.forEach { (name, info) ->
                    val isRequired = name in tool.requiredParameters
                    Text(
                        text = "• $name (${info.type})${if (isRequired) " *" else ""}: ${info.description}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
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


