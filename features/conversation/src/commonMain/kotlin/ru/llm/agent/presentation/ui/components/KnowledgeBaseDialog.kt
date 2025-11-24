package ru.llm.agent.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

/**
 * Диалог для добавления текста в базу знаний RAG
 */
@Composable
fun KnowledgeBaseDialog(
    onDismiss: () -> Unit,
    onAddKnowledge: (text: String, sourceId: String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var sourceId by remember { mutableStateOf("") }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var inputMode by remember { mutableStateOf(InputMode.TEXT) }

    var fileError by remember { mutableStateOf<String?>(null) }

    // Поддерживаемые расширения текстовых файлов
    val supportedExtensions = setOf("txt", "md", "json", "xml", "log", "csv", "kt", "java", "py", "js", "ts", "html", "css")

    // Загружаем текст из файла при его выборе
    LaunchedEffect(selectedFile) {
        selectedFile?.let { file ->
            fileError = null

            if (!file.exists()) {
                fileError = "Файл не найден"
                text = ""
                return@LaunchedEffect
            }

            if (!file.canRead()) {
                fileError = "Нет доступа к файлу"
                text = ""
                return@LaunchedEffect
            }

            // Проверяем расширение файла
            val extension = file.extension.lowercase()
            if (extension !in supportedExtensions) {
                fileError = "Неподдерживаемый формат файла: .$extension\nПоддерживаются: ${supportedExtensions.joinToString(", ")}"
                text = ""
                return@LaunchedEffect
            }

            try {
                text = file.readText()
                if (sourceId.isBlank()) {
                    sourceId = file.nameWithoutExtension
                }
            } catch (e: Exception) {
                // Ошибка чтения файла
                fileError = "Ошибка чтения файла: ${e.message}"
                text = ""
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить знания") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Добавьте текст в базу знаний для использования в RAG",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Переключатель режима ввода
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = inputMode == InputMode.TEXT,
                        onClick = { inputMode = InputMode.TEXT },
                        label = { Text("Ввести текст") }
                    )
                    FilterChip(
                        selected = inputMode == InputMode.FILE,
                        onClick = { inputMode = InputMode.FILE },
                        label = { Text("Загрузить файл") }
                    )
                }

                // Поле для названия источника
                OutlinedTextField(
                    value = sourceId,
                    onValueChange = { sourceId = it },
                    label = { Text("Название источника") },
                    placeholder = { Text("Например: kotlin-guide") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Выбор режима: текст или файл
                when (inputMode) {
                    InputMode.TEXT -> {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            label = { Text("Текст знаний") },
                            placeholder = {
                                Text("Вставьте текст, который будет использоваться для ответов...")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            maxLines = Int.MAX_VALUE
                        )
                    }
                    InputMode.FILE -> {
                        // Поле для пути к файлу
                        OutlinedTextField(
                            value = selectedFile?.absolutePath ?: "",
                            onValueChange = { path ->
                                if (path.isNotBlank()) {
                                    selectedFile = File(path)
                                }
                            },
                            label = { Text("Путь к файлу") },
                            placeholder = { Text("/path/to/document.txt") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = fileError != null,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Upload,
                                    contentDescription = "Выбрать файл"
                                )
                            },
                            supportingText = fileError?.let {
                                { Text(it, color = MaterialTheme.colorScheme.error) }
                            }
                        )

                        // Превью текста из файла
                        if (text.isNotBlank() && fileError == null) {
                            Text(
                                text = "Превью (${text.length} символов):",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = text.take(500) + if (text.length > 500) "..." else "",
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .verticalScroll(rememberScrollState()),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                Text(
                    text = when (inputMode) {
                        InputMode.TEXT -> "Текст будет разбит на фрагменты и проиндексирован"
                        InputMode.FILE -> "Поддерживаемые форматы: txt, md, json, xml, log, csv, kt, java, py, js, ts, html, css"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (text.isNotBlank() && sourceId.isNotBlank() && fileError == null) {
                        onAddKnowledge(text, sourceId)
                    }
                },
                enabled = text.isNotBlank() && sourceId.isNotBlank() && fileError == null
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

/**
 * Режим ввода данных в базу знаний
 */
private enum class InputMode {
    TEXT,  // Ввод текста вручную
    FILE   // Загрузка из файла
}