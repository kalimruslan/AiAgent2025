package ru.llm.agent.rag.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.llm.agent.core.utils.*
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
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var inputMode by remember { mutableStateOf(InputMode.TEXT) }
    var fileError by remember { mutableStateOf<String?>(null) }
    var isLoadingFile by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val fileManager = remember { getFileManager() }
    val documentParser = remember { getDocumentParser() }

    // Поддерживаемые расширения файлов
    val supportedTextExtensions = setOf("txt", "md", "json", "xml", "log", "csv", "kt", "java", "py", "js", "ts", "html", "css")
    val supportedDocExtensions = setOf("pdf", "doc", "docx")
    val allSupportedExtensions = supportedTextExtensions + supportedDocExtensions

    // Функция для выбора файла
    val pickFile = {
        scope.launch {
            isLoadingFile = true
            fileError = null

            when (val result = fileManager.pickFile(allSupportedExtensions.toList())) {
                is FilePickResult.Success -> {
                    selectedFileName = result.fileName
                    val extension = result.fileName.substringAfterLast('.', "").lowercase()

                    // Если это документ, парсим его
                    if (extension in supportedDocExtensions) {
                        when (val parseResult = documentParser.extractText(result.content, result.fileName)) {
                            is DocumentParseResult.Success -> {
                                text = parseResult.text
                                if (sourceId.isBlank()) {
                                    sourceId = result.fileName.substringBeforeLast('.')
                                }
                                fileError = null
                            }
                            is DocumentParseResult.Error -> {
                                fileError = parseResult.message
                                text = ""
                            }
                            is DocumentParseResult.UnsupportedFormat -> {
                                fileError = "Неподдерживаемый формат: ${parseResult.extension}"
                                text = ""
                            }
                        }
                    } else {
                        // Текстовый файл - читаем напрямую
                        try {
                            text = result.content.decodeToString()
                            if (sourceId.isBlank()) {
                                sourceId = result.fileName.substringBeforeLast('.')
                            }
                        } catch (e: Exception) {
                            fileError = "Ошибка чтения файла: ${e.message}"
                            text = ""
                        }
                    }
                }
                is FilePickResult.Cancelled -> {
                    // Пользователь отменил выбор
                }
                is FilePickResult.Error -> {
                    fileError = result.message
                    text = ""
                }
            }

            isLoadingFile = false
        }
    }

    // Загружаем текст из файла при его выборе (для ручного ввода пути - Desktop legacy)
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
            if (extension !in allSupportedExtensions) {
                fileError = "Неподдерживаемый формат файла: .$extension"
                text = ""
                return@LaunchedEffect
            }

            try {
                val content = file.readBytes()
                selectedFileName = file.name

                // Если это документ, парсим его
                if (extension in supportedDocExtensions) {
                    when (val parseResult = documentParser.extractText(content, file.name)) {
                        is DocumentParseResult.Success -> {
                            text = parseResult.text
                            if (sourceId.isBlank()) {
                                sourceId = file.nameWithoutExtension
                            }
                        }
                        is DocumentParseResult.Error -> {
                            fileError = parseResult.message
                            text = ""
                        }
                        is DocumentParseResult.UnsupportedFormat -> {
                            fileError = "Неподдерживаемый формат: ${parseResult.extension}"
                            text = ""
                        }
                    }
                } else {
                    text = file.readText()
                    if (sourceId.isBlank()) {
                        sourceId = file.nameWithoutExtension
                    }
                }
            } catch (e: Exception) {
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
                        // Кнопка выбора файла
                        OutlinedButton(
                            onClick = { pickFile() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoadingFile
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Выбрать файл",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            if (isLoadingFile) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Загрузка...")
                            } else {
                                Text(selectedFileName ?: "Выбрать файл")
                            }
                        }

                        // Показываем ошибку если есть
                        fileError?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        // Альтернативно: ввод пути вручную (для Desktop)
                        OutlinedTextField(
                            value = selectedFile?.absolutePath ?: "",
                            onValueChange = { path ->
                                if (path.isNotBlank()) {
                                    selectedFile = File(path)
                                }
                            },
                            label = { Text("Или укажите путь к файлу") },
                            placeholder = { Text("/path/to/document.pdf") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isLoadingFile
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
                        InputMode.FILE -> "Поддерживаемые форматы:\n" +
                                "• Документы: PDF, DOC, DOCX\n" +
                                "• Текстовые: TXT, MD, JSON, XML, LOG, CSV\n" +
                                "• Код: KT, JAVA, PY, JS, TS, HTML, CSS"
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
