package ru.llm.agent.presentation.ui.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

/**
 * Меню в TopBar с иконкой трех точек
 */
@Composable
fun TopBarMenu(
    onClearAll: () -> Unit,
    onExportJson: () -> Unit,
    onExportPdf: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Меню",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Очистить все") },
                onClick = {
                    expanded = false
                    onClearAll()
                }
            )
            DropdownMenuItem(
                text = { Text("Выгрузить диалог (JSON)") },
                onClick = {
                    expanded = false
                    onExportJson()
                }
            )
            DropdownMenuItem(
                text = { Text("Выгрузить диалог (PDF)") },
                onClick = {
                    expanded = false
                    onExportPdf()
                }
            )
        }
    }
}