# План реализации отображения токенов и времени ответа LLM

## Цель
Добавить возможность отображения статистики по каждому ответу LLM:
- Количество входных токенов
- Количество токенов ответа
- Общее количество токенов
- Время ответа

## Архитектурные изменения

### 1. Domain слой - ConversationMessage
**Файл:** `llm-domain/src/commonMain/kotlin/ru.llm.agent/model/conversation/ConversationMessage.kt`

**Изменения:**
```kotlin
public data class ConversationMessage(
    val id: Long = 0,
    val conversationId: String,
    val role: Role,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isContinue: Boolean = false,
    val isComplete: Boolean = false,
    val originalResponse: String? = null,
    val model: String,
    val expertOpinions: List<ExpertOpinion> = emptyList(),
    // НОВЫЕ ПОЛЯ:
    val inputTokens: Int? = null,
    val completionTokens: Int? = null,
    val totalTokens: Int? = null,
    val responseTimeMs: Long? = null
)
```

### 2. Data слой - MessageEntity + миграция БД
**Файл:** `llm-data/src/commonMain/kotlin/ru.llm.agent/database/messages/MessageEntity.kt`

**Изменения:**
```kotlin
@Entity(tableName = "messages")
public data class MessageEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: String,
    val role: String,
    val text: String,
    val timestamp: Long,
    val originalResponse: String? = null,
    val model: String,
    // НОВЫЕ ПОЛЯ:
    val inputTokens: Int? = null,
    val completionTokens: Int? = null,
    val totalTokens: Int? = null,
    val responseTimeMs: Long? = null
)
```

**Миграция БД:**
- Увеличить версию с 3 до 4 в `AgentDatabase`
- Создать миграцию MIGRATION_3_4
- Добавить колонки: `input_tokens`, `completion_tokens`, `total_tokens`, `response_time_ms`
- Все поля nullable для обратной совместимости

### 3. Service слой - MessageSendingResult
**Файл:** `llm-domain/src/commonMain/kotlin/ru.llm.agent/service/MessageSendingService.kt`

**Изменения:**
```kotlin
public data class MessageSendingResult(
    val conversationMessage: ConversationMessage,
    val rawResponse: String,
    // НОВЫЕ ПОЛЯ:
    val inputTokens: Int? = null,
    val completionTokens: Int? = null,
    val totalTokens: Int? = null,
    val responseTimeMs: Long
)
```

### 4. Service Implementation - MessageSendingServiceImpl
**Файл:** `llm-data/src/commonMain/kotlin/ru.llm.agent/service/MessageSendingServiceImpl.kt`

**Изменения:**

#### Для Yandex GPT (sendToYandex):
```kotlin
private suspend fun sendToYandex(...): Flow<NetworkResult<MessageSendingResult>> {
    val startTime = System.currentTimeMillis()

    val result = handleApi<YandexGPTResponse> {
        yandexApi.sendMessage(request)
    }

    return result.mapNetworkResult { response: YandexGPTResponse ->
        val responseTime = System.currentTimeMillis() - startTime
        val usage = response.result.usage

        val rawResponse = response.result.alternatives.firstOrNull()?.message?.text
            ?: throw Exception("Empty response from Yandex API")

        parseAndCreateMessage(
            conversationId = conversationId,
            rawResponse = rawResponse,
            provider = provider,
            providerName = "Yandex",
            inputTokens = usage?.inputTextTokens?.toIntOrNull(),
            completionTokens = usage?.completionTokens?.toIntOrNull(),
            totalTokens = usage?.totalTokens?.toIntOrNull(),
            responseTimeMs = responseTime
        )
    }
}
```

#### Для Proxy API (sendToProxy):
```kotlin
private suspend fun sendToProxy(...): Flow<NetworkResult<MessageSendingResult>> {
    val startTime = System.currentTimeMillis()

    val result = handleApi<ProxyApiResponse> {
        proxyApi.sendMessage(request)
    }

    return result.mapNetworkResult { response: ProxyApiResponse ->
        val responseTime = System.currentTimeMillis() - startTime
        val usage = response.usage

        val rawResponse = response.choices.firstOrNull()?.message?.content
            ?: throw Exception("Empty response from Proxy API")

        parseAndCreateMessage(
            conversationId = conversationId,
            rawResponse = rawResponse,
            provider = provider,
            providerName = "ProxyAPI",
            inputTokens = usage?.promptTokens,
            completionTokens = usage?.completionTokens,
            totalTokens = usage?.totalTokens,
            responseTimeMs = responseTime
        )
    }
}
```

#### Обновить parseAndCreateMessage:
```kotlin
private fun parseAndCreateMessage(
    conversationId: String,
    rawResponse: String,
    provider: LlmProvider,
    providerName: String,
    inputTokens: Int?,
    completionTokens: Int?,
    totalTokens: Int?,
    responseTimeMs: Long
): MessageSendingResult {
    val parseResult = parseAssistantResponseUseCase(rawResponse)
    val parsed = parseResult.getOrElse {
        logger.error("Ошибка парсинга ответа от $providerName: ${it.message}")
        throw it
    }

    val conversationMessage = ConversationMessage(
        id = 0L,
        conversationId = conversationId,
        role = Role.ASSISTANT,
        text = parsed.answer.orEmpty(),
        timestamp = System.currentTimeMillis(),
        isContinue = parsed.isCOntinue == true,
        isComplete = parsed.isComplete == true,
        originalResponse = rawResponse,
        model = provider.displayName,
        inputTokens = inputTokens,
        completionTokens = completionTokens,
        totalTokens = totalTokens,
        responseTimeMs = responseTimeMs
    )

    return MessageSendingResult(
        conversationMessage = conversationMessage,
        rawResponse = rawResponse,
        inputTokens = inputTokens,
        completionTokens = completionTokens,
        totalTokens = totalTokens,
        responseTimeMs = responseTimeMs
    )
}
```

### 5. Repository слой
**Файлы для проверки/обновления:**
- `llm-domain/src/commonMain/kotlin/ru.llm.agent/repository/ConversationRepository.kt`
- `llm-data/src/commonMain/kotlin/ru.llm.agent/repository/ConversationRepositoryImpl.kt`

**Изменения:** Убедиться, что маппинг между Entity и Model корректно передает новые поля.

### 6. UI слой - ConversationScreen
**Файл:** `features/conversation/src/commonMain/kotlin/ru/llm/agent/presentation/ui/ConversationScreen.kt`

**Изменения в MessageItem:**

```kotlin
@Composable
fun MessageItem(message: ConversationMessage) {
    val isUser = message.role == Role.USER
    var showOriginalJson by remember { mutableStateOf(false) }
    var showMetadata by remember { mutableStateOf(false) }  // НОВОЕ СОСТОЯНИЕ

    Column(modifier = Modifier.fillMaxWidth()) {
        if(!isUser) {
            Text("Model: ${message.model}")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            Card(
                // ... существующая конфигурация карточки
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.text,
                        // ... существующая конфигурация
                    )

                    // НОВЫЙ БЛОК: Показ метаданных (только для AI сообщений с данными)
                    if (!isUser && hasMetadata(message)) {
                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { showMetadata = !showMetadata },
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Text(
                                text = if (showMetadata) "Скрыть статистику" else "Показать статистику",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        if (showMetadata) {
                            Spacer(modifier = Modifier.height(4.dp))
                            MetadataCard(message)
                        }
                    }

                    // Существующий блок с JSON
                    if (!isUser && message.originalResponse != null) {
                        // ... существующий код
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = formatTimestamp(message.timestamp),
                        // ... существующая конфигурация
                    )
                }
            }
        }

        // Существующий блок с экспертами
        if (isUser && message.expertOpinions.isNotEmpty()) {
            // ... существующий код
        }
    }
}

/**
 * Проверка наличия метаданных у сообщения
 */
fun hasMetadata(message: ConversationMessage): Boolean {
    return message.totalTokens != null ||
           message.inputTokens != null ||
           message.completionTokens != null ||
           message.responseTimeMs != null
}

/**
 * Карточка с метаданными (токены и время)
 */
@Composable
fun MetadataCard(message: ConversationMessage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "📊 Статистика ответа",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            if (message.inputTokens != null) {
                MetadataRow(
                    label = "Входные токены:",
                    value = "${message.inputTokens}"
                )
            }

            if (message.completionTokens != null) {
                MetadataRow(
                    label = "Токены ответа:",
                    value = "${message.completionTokens}"
                )
            }

            if (message.totalTokens != null) {
                MetadataRow(
                    label = "Всего токенов:",
                    value = "${message.totalTokens}",
                    isBold = true
                )
            }

            if (message.responseTimeMs != null) {
                MetadataRow(
                    label = "Время ответа:",
                    value = formatResponseTime(message.responseTimeMs)
                )
            }
        }
    }
}

/**
 * Строка с меткой и значением
 */
@Composable
fun MetadataRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Форматирование времени ответа
 */
fun formatResponseTime(milliseconds: Long): String {
    return when {
        milliseconds < 1000 -> "${milliseconds}мс"
        milliseconds < 60000 -> "${milliseconds / 1000.0}сек"
        else -> "${milliseconds / 60000}мин ${(milliseconds % 60000) / 1000}сек"
    }
}
```

## Дополнительные задачи

### 7. Обновить ExecuteCommitteeUseCase
**Проверить:** Нужно ли передавать метаданные при использовании режима Committee.

### 8. Тестирование
- Протестировать с Yandex GPT
- Протестировать с Proxy API (OpenAI, Mistral)
- Проверить миграцию БД со старыми данными
- Убедиться в корректном отображении UI

## Важные замечания

1. **Nullable поля:** Все новые поля должны быть nullable для обратной совместимости
2. **Миграция БД:** Обязательно создать миграцию, чтобы не потерять существующие данные
3. **UI только для AI:** Метаданные показываются только для сообщений от ассистента
4. **Разные форматы токенов:** Yandex возвращает String, Proxy API - Int, нужна конвертация
5. **Комментарии на русском:** Все новые комментарии и KDoc должны быть на русском языке

## Порядок реализации

1. ✅ Domain модели (ConversationMessage)
2. ✅ Data модели (MessageEntity) + миграция
3. ✅ Service (MessageSendingResult, MessageSendingServiceImpl)
4. ✅ Repository (если требуется)
5. ✅ UI (ConversationScreen, MessageItem, MetadataCard)
6. ✅ Тестирование
