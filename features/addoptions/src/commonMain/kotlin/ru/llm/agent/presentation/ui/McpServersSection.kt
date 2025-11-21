package ru.llm.agent.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ru.llm.agent.model.mcp.McpServer
import ru.llm.agent.model.mcp.McpServerType

/**
 * Секция управления MCP серверами
 */
@Composable
internal fun McpServersSection(
    servers: List<McpServer>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddServer: () -> Unit,
    onDeleteServer: (Long) -> Unit,
    onToggleActive: (Long, Boolean) -> Unit
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
                    text = "MCP Серверы",
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAddServer,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+ Добавить сервер")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (servers.isEmpty()) {
                    Text(
                        text = "Нет добавленных серверов",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    servers.forEach { server ->
                        McpServerItem(
                            server = server,
                            onDelete = { onDeleteServer(server.id) },
                            onToggleActive = { onToggleActive(server.id, it) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

/**
 * Элемент списка MCP сервера
 */
@Composable
private fun McpServerItem(
    server: McpServer,
    onDelete: () -> Unit,
    onToggleActive: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (server.isActive)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = server.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (server.type == McpServerType.REMOTE) "[Remote]" else "[Local]",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (server.type == McpServerType.REMOTE)
                                MaterialTheme.colorScheme.secondary
                            else
                                MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    when (server.type) {
                        McpServerType.REMOTE -> {
                            Text(
                                text = server.url ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        McpServerType.LOCAL -> {
                            Text(
                                text = "${server.command} ${server.args?.joinToString(" ") ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    server.description?.let { desc ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Switch(
                    checked = server.isActive,
                    onCheckedChange = onToggleActive
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Удалить")
            }
        }
    }
}

/**
 * Диалог добавления нового MCP сервера
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddServerDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, type: McpServerType, url: String?, command: String?, args: List<String>?, description: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var serverType by remember { mutableStateOf(McpServerType.REMOTE) }
    var url by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }
    var argsText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp)
            ) {
                Text(
                    text = "Добавить MCP сервер",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Выбор типа сервера
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = serverType == McpServerType.REMOTE,
                        onClick = { serverType = McpServerType.REMOTE },
                        label = { Text("Remote (HTTP)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = serverType == McpServerType.LOCAL,
                        onClick = { serverType = McpServerType.LOCAL },
                        label = { Text("Local (stdio)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Поля в зависимости от типа сервера
                when (serverType) {
                    McpServerType.REMOTE -> {
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            label = { Text("URL") },
                            placeholder = { Text("http://localhost:3000") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    McpServerType.LOCAL -> {
                        OutlinedTextField(
                            value = command,
                            onValueChange = { command = it },
                            label = { Text("Команда") },
                            placeholder = { Text("node, python, npx, ...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = argsText,
                            onValueChange = { argsText = it },
                            label = { Text("Аргументы (через пробел)") },
                            placeholder = { Text("server.js или -m http.server") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание (опционально)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val isValid = when (serverType) {
                                McpServerType.REMOTE -> name.isNotBlank() && url.isNotBlank()
                                McpServerType.LOCAL -> name.isNotBlank() && command.isNotBlank()
                            }

                            if (isValid) {
                                val args = if (argsText.isNotBlank())
                                    argsText.trim().split(" ").filter { it.isNotBlank() }
                                else
                                    null

                                onAdd(
                                    name.trim(),
                                    serverType,
                                    url.trim().takeIf { it.isNotBlank() },
                                    command.trim().takeIf { it.isNotBlank() },
                                    args,
                                    description.trim()
                                )
                            }
                        },
                        enabled = when (serverType) {
                            McpServerType.REMOTE -> name.isNotBlank() && url.isNotBlank()
                            McpServerType.LOCAL -> name.isNotBlank() && command.isNotBlank()
                        }
                    ) {
                        Text("Добавить")
                    }
                }
            }
        }
    }
}