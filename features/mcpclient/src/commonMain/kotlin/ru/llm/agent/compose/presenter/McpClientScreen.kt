package ru.llm.agent.compose.presenter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.koin.compose.module.rememberKoinModules
import org.koin.compose.scope.KoinScope
import org.koin.compose.viewmodel.koinViewModel
import ru.llm.agent.McpClient
import ru.llm.agent.compose.di.MCP_CLIENT_SCOPE_ID
import ru.llm.agent.compose.di.mcpClientKoinModule
import ru.llm.agent.compose.di.mcpClientScopeQualifier

/**
 * Создать простой MCP сервер и подключить его к Андроид клиенту
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
        var echoText by remember { mutableStateOf("") }
        var numberA by remember { mutableStateOf("5") }
        var numberB by remember { mutableStateOf("3") }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("MCP Client") }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Кнопка подключения
                Button(
                    onClick = { viewModel.connect() },
                    enabled = !uiState.isConnected && !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (uiState.isConnected) "Connected" else "Connect to Server")
                }

                // Статус
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                }

                // Результат
                if (uiState.result.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = uiState.result,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Ошибка
                if (uiState.error != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "Error: ${uiState.error}",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                // Инструменты
                if (uiState.isConnected) {
                    HorizontalDivider()

                    Text(
                        text = "Tools",
                        style = MaterialTheme.typography.titleLarge
                    )

                    // Echo Tool
                    OutlinedTextField(
                        value = echoText,
                        onValueChange = { echoText = it },
                        label = { Text("Echo text") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { viewModel.callEcho(echoText) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Call Echo")
                    }

                    // Add Tool
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = numberA,
                            onValueChange = { numberA = it },
                            label = { Text("Number A") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = numberB,
                            onValueChange = { numberB = it },
                            label = { Text("Number B") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Button(
                        onClick = {
                            val a = numberA.toDoubleOrNull() ?: 0.0
                            val b = numberB.toDoubleOrNull() ?: 0.0
                            viewModel.callAdd(a, b)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Numbers")
                    }

                    // Get Time Tool
                    Button(
                        onClick = { viewModel.getCurrentTime() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Get Current Time")
                    }

                    // Список доступных инструментов
                    if (uiState.tools.isNotEmpty()) {
                        HorizontalDivider()
                        Text(
                            text = "Available Tools:",
                            style = MaterialTheme.typography.titleMedium
                        )
                        uiState.tools.forEach { tool ->
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = tool.name,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = tool.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


