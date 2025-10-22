package ru.llm.agent.compose.presenter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.module.rememberKoinModules
import org.koin.compose.scope.KoinScope
import org.koin.compose.viewmodel.koinViewModel
import ru.llm.agent.compose.di.OPTIONS_SCOPE_ID
import ru.llm.agent.compose.di.optionsKoinModule
import ru.llm.agent.compose.di.optionsScopeQualifier
import ru.llm.agent.core.uikit.LlmAgentTheme
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
    onNavigateBack: () -> Unit
) {
    KoinScope(OPTIONS_SCOPE_ID, optionsScopeQualifier) {
        rememberKoinModules {
            listOf(optionsKoinModule())
        }
        val viewModel = koinViewModel() as OptionsViewModel
        val state by viewModel.screeState.collectAsStateWithLifecycle()
        viewModel.start()

        Scaffold(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = { Text("Настройки") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
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
                OptionsScreen(
                    modifier = Modifier.padding(top = 8.dp),
                    systemPrompt = state.systemPrompt.orEmpty(),
                    temperature = state.temperature.toString(),
                    tokens = state.maxTokens.toString(),
                    onApplyClick = { systemPrompt, temperature, tokens ->
                        viewModel.setEvent(
                            OptionsUIState.Event.ApplyClick(
                                navigateAction = onNavigateBack,
                                systemPrompt = systemPrompt,
                                temperature = temperature,
                                maxTokens = tokens
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun BoxScope.OptionsScreen(
    modifier: Modifier = Modifier,
    systemPrompt: String,
    temperature: String,
    tokens: String,
    onApplyClick: (String, String, String) -> Unit
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

fun formatTimestamp(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dateTime.hour.toString().padStart(2, '0')}:${
        dateTime.minute.toString().padStart(2, '0')
    }"
}


