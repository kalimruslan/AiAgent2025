# Реализация функционала "Мнения экспертов" (Committee Mode)

## Обзор

Документ описывает пошаговую реализацию режима "Комитет экспертов" (Committee Mode), где несколько AI-экспертов дают свои мнения по вопросу пользователя, после чего синтезируется финальный ответ.

## Архитектура решения

### Общая схема данных

```
User Message (MessageEntity)
    ↓
Expert 1 Opinion (ExpertOpinionEntity) ─┐
Expert 2 Opinion (ExpertOpinionEntity) ─┼→ Synthesis → Final Answer (MessageEntity)
Expert 3 Opinion (ExpertOpinionEntity) ─┘
```

### Слои приложения

1. **Domain Layer** (`llm-domain/`)
   - Модели данных
   - Интерфейсы репозиториев
   - Use Cases для бизнес-логики

2. **Data Layer** (`llm-data/`)
   - Реализация репозиториев
   - База данных (Room)
   - API клиенты

3. **Presentation Layer** (`features/conversation/`)
   - UI компоненты
   - ViewModel
   - State management

---

## Шаг 1: Подготовка Domain Layer

### 1.1 Создать модель Expert

**Файл:** `llm-domain/src/commonMain/kotlin/ru.llm.agent/model/Expert.kt`

```kotlin
public data class Expert(
    public val id: String,
    public val name: String,
    public val description: String,
    public val icon: String,
    public val systemPrompt: String
) {
    public companion object {
        public fun getPredefinedExperts(): List<Expert> = listOf(
            Expert(
                id = "security",
                name = "Безопасность",
                description = "Эксперт по безопасности",
                icon = "🔒",
                systemPrompt = "Ты — эксперт по безопасности Android..."
            ),
            Expert(
                id = "performance",
                name = "Производительность",
                description = "Эксперт по оптимизации",
                icon = "⚡",
                systemPrompt = "Ты — эксперт по производительности..."
            ),
            // Другие эксперты...
        )
    }
}
```

### 1.2 Создать модель ExpertOpinion

**Файл:** `llm-domain/src/commonMain/kotlin/ru.llm.agent/model/ExpertOpinion.kt`

```kotlin
public data class ExpertOpinion(
    public val id: Long = 0,
    public val expertId: String,
    public val expertName: String,
    public val expertIcon: String,
    public val messageId: Long,
    public val opinion: String,
    public val timestamp: Long,
    public val originalResponse: String? = null
)
```

### 1.3 Создать модель ConversationMode

**Файл:** `llm-domain/src/commonMain/kotlin/ru.llm.agent/model/ConversationMode.kt`

```kotlin
public enum class ConversationMode(
    public val displayName: String,
    public val description: String
) {
    SINGLE("Single AI", "Один AI-ассистент"),
    COMMITTEE("Committee", "Комитет экспертов");

    public companion object {
        public fun default(): ConversationMode = SINGLE
    }
}
```

### 1.4 Расширить модель ConversationMessage

**Файл:** `llm-domain/src/commonMain/kotlin/ru.llm.agent/model/conversation/ConversationMessage.kt`

Добавить поле:
```kotlin
/** Мнения экспертов, связанные с этим сообщением (для режима Committee) */
val expertOpinions: List<ExpertOpinion> = emptyList()
```

### 1.5 Создать интерфейс ExpertRepository

**Файл:** `llm-domain/src/commonMain/kotlin/ru.llm.agent/repository/ExpertRepository.kt`

```kotlin
public interface ExpertRepository {
    public suspend fun saveExpertOpinion(
        expertId: String,
        expertName: String,
        expertIcon: String,
        messageId: Long,
        conversationId: String,
        opinion: String,
        timestamp: Long,
        originalResponse: String?
    ): Long

    public fun getOpinionsForMessage(messageId: Long): Flow<List<ExpertOpinion>>
    public fun getOpinionsForConversation(conversationId: String): Flow<List<ExpertOpinion>>
    public suspend fun deleteOpinionsForConversation(conversationId: String)
    public suspend fun deleteOpinionsForMessage(messageId: Long)
    public suspend fun getOpinionsCountForMessage(messageId: Long): Int
}
```

### 1.6 Обновить интерфейс ConversationRepository

**Файл:** `llm-domain/src/commonMain/kotlin/ru.llm.agent/repository/ConversationRepository.kt`

Добавить метод:
```kotlin
/** Получить сообщения вместе с мнениями экспертов (для режима Committee) */
public suspend fun getMessagesWithExpertOpinions(conversationId: String): Flow<List<ConversationMessage>>
```

### 1.7 Создать ExecuteCommitteeUseCase

**Файл:** `llm-domain/src/commonMain/kotlin/ru.llm.agent/usecase/ExecuteCommitteeUseCase.kt`

```kotlin
public class ExecuteCommitteeUseCase(
    private val conversationRepository: ConversationRepository,
    private val expertRepository: ExpertRepository
) {
    public suspend operator fun invoke(
        conversationId: String,
        userMessage: String,
        experts: List<Expert>,
        provider: LlmProvider,
        messageId: Long
    ): Flow<NetworkResult<CommitteeResult>>
}

public sealed class CommitteeResult {
    public data class ExpertOpinion(val opinion: ExpertOpinionResult) : CommitteeResult()
    public data class FinalSynthesis(val answer: String) : CommitteeResult()
}
```

Логика:
1. Для каждого эксперта:
   - Создать временный диалог с системным промптом эксперта
   - Отправить вопрос пользователя
   - Сохранить мнение в БД через `expertRepository.saveExpertOpinion()`
   - Эмитить `CommitteeResult.ExpertOpinion`

2. Синтез финального ответа:
   - Собрать все мнения
   - Создать промпт для синтеза
   - Отправить в LLM
   - Эмитить `CommitteeResult.FinalSynthesis`

---

## Шаг 2: Реализация Data Layer

### 2.1 Создать Entity для мнений экспертов

**Файл:** `llm-data/src/commonMain/kotlin/ru.llm.agent/database/expert/ExpertOpinionEntity.kt`

```kotlin
@Entity(tableName = "expert_opinions")
public data class ExpertOpinionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val expertId: String,
    val expertName: String,
    val expertIcon: String,
    val messageId: Long,
    val conversationId: String,
    val opinion: String,
    val timestamp: Long,
    val originalResponse: String? = null
)
```

### 2.2 Создать DAO для мнений экспертов

**Файл:** `llm-data/src/commonMain/kotlin/ru.llm.agent/database/expert/ExpertOpinionDao.kt`

```kotlin
@Dao
public interface ExpertOpinionDao {
    @Insert
    public suspend fun insertOpinion(opinion: ExpertOpinionEntity): Long

    @Query("SELECT * FROM expert_opinions WHERE messageId = :messageId ORDER BY timestamp ASC")
    public fun getOpinionsForMessage(messageId: Long): Flow<List<ExpertOpinionEntity>>

    @Query("SELECT * FROM expert_opinions WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    public fun getOpinionsForConversation(conversationId: String): Flow<List<ExpertOpinionEntity>>

    @Query("DELETE FROM expert_opinions WHERE conversationId = :conversationId")
    public suspend fun deleteOpinionsForConversation(conversationId: String)

    @Query("DELETE FROM expert_opinions WHERE messageId = :messageId")
    public suspend fun deleteOpinionsForMessage(messageId: Long)

    @Query("SELECT COUNT(*) FROM expert_opinions WHERE messageId = :messageId")
    public suspend fun getOpinionsCountForMessage(messageId: Long): Int
}
```

### 2.3 Обновить MessageDatabase

**Файл:** `llm-data/src/commonMain/kotlin/ru.llm.agent/database/MessageDatabase.kt`

```kotlin
@Database(
    entities = [
        MessageEntity::class,
        ContextEntity::class,
        ExpertOpinionEntity::class  // Добавить
    ],
    version = 3,  // Увеличить версию
    exportSchema = true
)
abstract class MessageDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun settingsDao(): ContextDao
    abstract fun expertOpinionDao(): ExpertOpinionDao  // Добавить
}
```

**Важно:** Создать файл миграции схемы:
`llm-data/schemas/ru.llm.agent.database.MessageDatabase/3.json`

### 2.4 Создать ExpertRepositoryImpl

**Файл:** `llm-data/src/commonMain/kotlin/ru.llm.agent/repository/ExpertRepositoryImpl.kt`

```kotlin
public class ExpertRepositoryImpl(
    private val expertOpinionDao: ExpertOpinionDao
) : ExpertRepository {

    override suspend fun saveExpertOpinion(...): Long {
        val entity = ExpertOpinionEntity(...)
        return expertOpinionDao.insertOpinion(entity)
    }

    override fun getOpinionsForMessage(messageId: Long): Flow<List<ExpertOpinion>> {
        return expertOpinionDao.getOpinionsForMessage(messageId).map { entities ->
            entities.map { it.toExpertOpinion() }
        }
    }

    // Остальные методы...

    private fun ExpertOpinionEntity.toExpertOpinion(): ExpertOpinion { ... }
}
```

### 2.5 Обновить ConversationRepositoryImpl

**Файл:** `llm-data/src/commonMain/kotlin/ru.llm.agent/repository/ConversationRepositoryImpl.kt`

Добавить зависимость:
```kotlin
public class ConversationRepositoryImpl(
    // ...
    private val expertRepository: ExpertRepository,
) : ConversationRepository
```

Реализовать метод:
```kotlin
override suspend fun getMessagesWithExpertOpinions(
    conversationId: String
): Flow<List<ConversationMessage>> {
    return expertRepository.getOpinionsForConversation(conversationId).map { allOpinions ->
        // Группируем мнения по messageId
        val opinionsByMessageId = allOpinions.groupBy { it.messageId }

        // Загружаем сообщения синхронно
        val messages = messageDao.getMessagesByConversationSync(conversationId)
            .map { it.toModel() }

        // Для каждого сообщения пользователя добавляем мнения экспертов
        messages.map { message ->
            if (message.role == Role.USER) {
                val opinions = opinionsByMessageId[message.id] ?: emptyList()
                message.copy(expertOpinions = opinions)
            } else {
                message
            }
        }
    }
}
```

### 2.6 Обновить Koin DI

**Файл:** `llm-data/src/commonMain/kotlin/ru.llm.agent/di/DataLayerKoinModule.kt`

```kotlin
single<ExpertRepository> {
    ExpertRepositoryImpl(
        expertOpinionDao = get<MessageDatabase>().expertOpinionDao()
    )
}

single<ConversationRepository> {
    ConversationRepositoryImpl(
        // ...
        expertRepository = get()  // Добавить
    )
}
```

---

## Шаг 3: Реализация Presentation Layer

### 3.1 Обновить ConversationUIState

**Файл:** `features/conversation/.../ConversationUIState.kt`

```kotlin
data class State(
    // ...
    val selectedMode: ConversationMode = ConversationMode.default(),
    val selectedExperts: List<Expert> = Expert.getPredefinedExperts().take(3),
    val availableExperts: List<Expert> = Expert.getPredefinedExperts(),
)

sealed interface Event {
    // ...
    data class SelectMode(val mode: ConversationMode) : Event
    data class ToggleExpert(val expert: Expert) : Event
}
```

### 3.2 Обновить ConversationViewModel

**Файл:** `features/conversation/.../ConversationViewModel.kt`

```kotlin
class ConversationViewModel(
    // ...
    private val executeCommitteeUseCase: ExecuteCommitteeUseCase
) : ViewModel() {

    // Загрузка сообщений в зависимости от режима
    private fun loadMessages() {
        viewModelScope.launch {
            when (_screeState.value.selectedMode) {
                ConversationMode.SINGLE -> {
                    conversationUseCase.invoke(conversationId).collect { messages ->
                        _screeState.update {
                            it.copy(messages = messages.filter { msg -> msg.role != Role.SYSTEM })
                        }
                    }
                }
                ConversationMode.COMMITTEE -> {
                    conversationRepository.getMessagesWithExpertOpinions(conversationId)
                        .collect { messages ->
                            _screeState.update {
                                it.copy(messages = messages.filter { msg -> msg.role != Role.SYSTEM })
                            }
                        }
                }
            }
        }
    }

    // Отправка сообщения
    private fun sendMessageToAi(message: String) {
        when (_screeState.value.selectedMode) {
            ConversationMode.SINGLE -> sendMessageToSingleAi(message)
            ConversationMode.COMMITTEE -> sendMessageToCommittee(message)
        }
    }

    // Отправка в режиме Committee
    private fun sendMessageToCommittee(message: String) {
        viewModelScope.launch {
            val messageId = /* ID последнего сообщения пользователя */

            executeCommitteeUseCase.invoke(
                conversationId = conversationId,
                userMessage = message,
                experts = _screeState.value.selectedExperts,
                provider = _screeState.value.selectedProvider,
                messageId = messageId
            ).collect { result ->
                result.doActionIfLoading {
                    _screeState.update { it.copy(isLoading = true) }
                }
                result.doActionIfSuccess { committeeResult ->
                    when (committeeResult) {
                        is CommitteeResult.ExpertOpinion -> {
                            // UI обновится автоматически через Flow из БД
                        }
                        is CommitteeResult.FinalSynthesis -> {
                            _screeState.update { it.copy(isLoading = false) }
                        }
                    }
                }
            }
        }
    }

    // Переключение режима
    private fun selectMode(mode: ConversationMode) {
        _screeState.update { it.copy(selectedMode = mode) }
        loadMessages()  // Перезагружаем сообщения
    }

    // Переключение экспертов
    private fun toggleExpert(expert: Expert) {
        _screeState.update { state ->
            val currentExperts = state.selectedExperts
            val updatedExperts = if (currentExperts.contains(expert)) {
                if (currentExperts.size > 1) currentExperts - expert
                else currentExperts  // Минимум 1 эксперт
            } else {
                currentExperts + expert
            }
            state.copy(selectedExperts = updatedExperts)
        }
    }
}
```

### 3.3 Создать UI компоненты

**Файл:** `features/conversation/.../ConversationScreen.kt`

#### 3.3.1 Dropdown для выбора режима

```kotlin
@Composable
fun ConversationModeDropdown(
    selectedMode: ConversationMode,
    onModeSelected: (ConversationMode) -> Unit,
    enabled: Boolean = true
) {
    // ExposedDropdownMenuBox с режимами SINGLE / COMMITTEE
}
```

#### 3.3.2 Панель выбора экспертов

```kotlin
@Composable
fun ExpertsSelectionPanel(
    selectedExperts: List<Expert>,
    availableExperts: List<Expert>,
    onToggleExpert: (Expert) -> Unit,
    enabled: Boolean = true
) {
    Card {
        Text("Выбранные эксперты (${selectedExperts.size}):")
        LazyRow {
            items(availableExperts) { expert ->
                ExpertChip(
                    expert = expert,
                    isSelected = selectedExperts.contains(expert),
                    onClick = { onToggleExpert(expert) }
                )
            }
        }
    }
}
```

#### 3.3.3 Карточка мнения эксперта

```kotlin
@Composable
fun ExpertOpinionCard(opinion: ExpertOpinion) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column {
            // Заголовок с иконкой и именем
            Row {
                Text(text = opinion.expertIcon)
                Text(text = opinion.expertName)
            }

            // Текст мнения
            Text(text = opinion.opinion)

            // Кнопка показа оригинального JSON
            if (opinion.originalResponse != null) {
                TextButton(onClick = { /* toggle */ }) {
                    Text("Показать JSON")
                }
            }

            // Временная метка
            Text(formatTimestamp(opinion.timestamp))
        }
    }
}
```

#### 3.3.4 Обновить MessageItem

```kotlin
@Composable
fun MessageItem(message: ConversationMessage) {
    Column {
        // Основная карточка сообщения
        Card { /* текст сообщения */ }

        // Отображаем мнения экспертов (если есть)
        if (message.role == Role.USER && message.expertOpinions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                message.expertOpinions.forEach { opinion ->
                    ExpertOpinionCard(opinion)
                }
            }
        }
    }
}
```

#### 3.3.5 Интеграция в ConversationScreen

```kotlin
@Composable
fun ConversationScreen() {
    Scaffold(
        topBar = {
            TopAppBar {
                // Dropdown выбора режима
                ConversationModeDropdown(
                    selectedMode = state.selectedMode,
                    onModeSelected = { viewModel.setEvent(Event.SelectMode(it)) }
                )
            }
        }
    ) {
        Column {
            // Панель выбора экспертов (только в режиме Committee)
            if (state.selectedMode == ConversationMode.COMMITTEE) {
                ExpertsSelectionPanel(
                    selectedExperts = state.selectedExperts,
                    availableExperts = state.availableExperts,
                    onToggleExpert = { viewModel.setEvent(Event.ToggleExpert(it)) }
                )
            }

            // Список сообщений
            MessagesContent(messages = state.messages)
        }
    }
}
```

### 3.4 Обновить Koin DI для ViewModel

**Файл:** `features/conversation/.../di/ConversationKoin.kt`

```kotlin
viewModel {
    ConversationViewModel(
        conversationUseCase = get(),
        sendConversationMessageUseCase = get(),
        conversationRepository = get(),
        executeCommitteeUseCase = get()  // Добавить
    )
}
```

**Файл:** `llm-domain/.../di/DomainLayerKoinModule.kt`

```kotlin
factory {
    ExecuteCommitteeUseCase(
        conversationRepository = get(),
        expertRepository = get()
    )
}
```

---

## Шаг 4: Тестирование

### 4.1 Unit-тесты

1. **ExecuteCommitteeUseCase** - проверка логики сбора мнений и синтеза
2. **ExpertRepositoryImpl** - CRUD операции с мнениями
3. **ConversationRepositoryImpl.getMessagesWithExpertOpinions** - корректное объединение данных

### 4.2 UI-тесты

1. Переключение между режимами Single/Committee
2. Выбор/снятие экспертов
3. Отображение мнений экспертов под сообщениями пользователя
4. Корректная прокрутка списка сообщений

### 4.3 Интеграционные тесты

1. Полный флоу: отправка сообщения → получение мнений → синтез → отображение
2. Сохранение и загрузка мнений из БД
3. Работа с несколькими экспертами одновременно

---

## Шаг 5: Оптимизация и улучшения

### 5.1 Производительность

- **Кэширование мнений** - избежать повторных запросов к БД
- **Pagination для мнений** - если экспертов много
- **Lazy loading** - загружать мнения только при развороте сообщения

### 5.2 UX улучшения

- **Прогресс-бар** - показывать, сколько экспертов уже ответило
- **Анимации** - плавное появление карточек мнений
- **Возможность сворачивать/разворачивать мнения** - для экономии места
- **Фильтрация экспертов** - по категориям/специализациям

### 5.3 Обработка ошибок

- Что делать, если один из экспертов не ответил?
- Тайм-ауты для запросов к LLM
- Повторные попытки при ошибках сети
- Graceful degradation - показать хотя бы часть мнений

---

## Возможные проблемы и решения

### Проблема 1: Блокировка UI при загрузке мнений

**Причина:** Вызов `collect` внутри `map` блокирует Flow

**Решение:**
```kotlin
// ❌ Неправильно
messageDao.getMessages().map { messages ->
    messages.map { message ->
        expertRepository.getOpinions(message.id).collect { ... }  // Блокирует!
    }
}

// ✅ Правильно
expertRepository.getAllOpinions().map { allOpinions ->
    val opinionsByMessageId = allOpinions.groupBy { it.messageId }
    messageDao.getMessagesSync().map { message ->
        message.copy(expertOpinions = opinionsByMessageId[message.id] ?: emptyList())
    }
}
```

### Проблема 2: Мнения не обновляются в реальном времени

**Причина:** UI не подписан на изменения в таблице `expert_opinions`

**Решение:** Использовать `Flow` из Room DAO, который автоматически эмитит новые значения при изменении данных

### Проблема 3: Дублирование запросов к LLM

**Причина:** UseCase вызывается несколько раз

**Решение:** Добавить флаг `isLoading` и игнорировать повторные вызовы

---

## Диаграмма последовательности (Sequence Diagram)

```
User → UI: Отправить сообщение в режиме Committee
UI → ViewModel: Event.SendMessage
ViewModel → ExecuteCommitteeUseCase: invoke()
ExecuteCommitteeUseCase → ConversationRepository: saveUserMessage()
ConversationRepository → MessageDao: insertMessage()

loop Для каждого эксперта
    ExecuteCommitteeUseCase → ConversationRepository: sendMessage(expertPrompt)
    ConversationRepository → LLM API: POST /chat
    LLM API → ConversationRepository: Ответ эксперта
    ConversationRepository → ExecuteCommitteeUseCase: ExpertResponse
    ExecuteCommitteeUseCase → ExpertRepository: saveExpertOpinion()
    ExpertRepository → ExpertOpinionDao: insertOpinion()
    ExpertOpinionDao → UI: Flow emit (новое мнение)
    UI → User: Показать мнение эксперта
end

ExecuteCommitteeUseCase → ConversationRepository: synthesizeFinalAnswer(allOpinions)
ConversationRepository → LLM API: POST /chat (синтез)
LLM API → ConversationRepository: Финальный ответ
ConversationRepository → MessageDao: insertMessage(finalAnswer)
MessageDao → UI: Flow emit (финальный ответ)
UI → User: Показать финальный ответ
```

---

## Заключение

Реализация функционала "Мнения экспертов" требует координации между всеми слоями архитектуры:

1. **Domain** - определяет бизнес-логику и контракты
2. **Data** - реализует хранение и получение данных
3. **Presentation** - отображает данные и обрабатывает взаимодействие с пользователем

Ключевые моменты:
- Использование Flow для реактивности
- Правильная группировка данных для избежания N+1 запросов
- Разделение ответственности между слоями
- Обработка ошибок на каждом уровне

Следуя этому плану, можно реализовать масштабируемый и поддерживаемый функционал режима комитета экспертов.