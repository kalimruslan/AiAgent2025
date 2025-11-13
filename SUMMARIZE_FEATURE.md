# План реализации механизма сжатия истории диалога с суммаризацией

## 📋 Обзор задачи

Необходимо реализовать автоматическое сжатие истории диалога, когда количество использованных токенов приближается к лимиту. При превышении порога старые сообщения будут суммаризироваться через `summarizeYandexGPTText`, а информация о суммаризации отображаться в UI через `TokenUsageProgressBar`.

---

## 🏗️ Архитектурные слои изменений

### **1. Domain Layer (llm-domain)**

#### **1.1. Создать новый Use Case: `SummarizeHistoryUseCase`**
- **Файл**: `llm-domain/src/commonMain/kotlin/ru.llm.agent/usecase/SummarizeHistoryUseCase.kt`
- **Ответственность**:
  - Определить, нужна ли суммаризация (проверка порога токенов)
  - Выбрать сообщения для суммаризации (все кроме последних N)
  - Вызвать `LlmRepository.summarizeYandexGPTText()`
  - Создать новое суммаризированное сообщение с ролью `SYSTEM`
  - Удалить старые сообщения из БД
  - Сохранить суммаризированное сообщение

**Параметры конфигурации**:
- `tokenThreshold: Double = 0.75` (75% от maxTokens)
- `messagesKeepLast: Int = 3` (сколько последних сообщений сохранить без суммаризации)
- `summarizationMaxTokens: Int = 300` (макс. длина суммаризации)

#### **1.2. Расширить модель `ConversationMessage`**
- **Файл**: `llm-domain/src/commonMain/kotlin/ru.llm.agent/model/conversation/ConversationMessage.kt`
- Добавить поле `isSummarized: Boolean = false` для маркировки суммаризированных сообщений

#### **1.3. Создать модель `SummarizationInfo`**
- **Файл**: `llm-domain/src/commonMain/kotlin/ru.llm.agent/model/SummarizationInfo.kt`
- Данные:
  ```kotlin
  data class SummarizationInfo(
      val hasSummarizedMessages: Boolean,
      val summarizedMessagesCount: Int,
      val savedTokens: Int,
      val lastSummarizationTimestamp: Long?
  )
  ```

---

### **2. Data Layer (llm-data)**

#### **2.1. Расширить Room entities**
- **Файл**: `llm-data/src/commonMain/kotlin/ru.llm.agent/db/entity/MessageEntity.kt`
- Добавить поле `isSummarized: Boolean = false`
- Обновить миграцию схемы БД

#### **2.2. Расширить DAO**
- **Файл**: `llm-data/src/commonMain/kotlin/ru.llm.agent/db/dao/MessageDao.kt`
- Добавить методы:
  - `getMessagesByConversationOrderedByTime(conversationId: String): List<MessageEntity>`
  - `deleteMessagesByIds(ids: List<Long>)`
  - `getSummarizedMessagesInfo(conversationId: String): SummarizationInfo?`

#### **2.3. Реализовать `ConversationRepository` методы**
- **Файл**: `llm-data/src/commonMain/kotlin/ru.llm.agent/repository/ConversationRepositoryImpl.kt`
- Добавить:
  - `getSummarizationInfo(conversationId: String): Flow<SummarizationInfo>`
  - `deleteMessages(messageIds: List<Long>)`
  - `saveSystemMessage(conversationId: String, text: String, isSummarized: Boolean): Long`

---

### **3. Presentation Layer (features/conversation)**

#### **3.1. Расширить `ConversationUIState`**
- **Файл**: `features/conversation/src/commonMain/kotlin/ru/llm/agent/presentation/state/ConversationUIState.kt`
- Добавить в `State`:
  ```kotlin
  /** Информация о суммаризации истории */
  val summarizationInfo: SummarizationInfo? = null,
  /** Флаг процесса суммаризации */
  val isSummarizing: Boolean = false
  ```

#### **3.2. Обновить `TokenUsageProgressBar`**
- **Файл**: `features/conversation/src/commonMain/kotlin/ru/llm/agent/presentation/ui/ConversationScreen.kt`
- Добавить параметры:
  ```kotlin
  summarizationInfo: SummarizationInfo?,
  isSummarizing: Boolean
  ```
- Визуальные изменения:
  - Показать индикатор суммаризации (например, иконка 📝)
  - Отобразить количество суммаризированных сообщений
  - Показать сколько токенов сэкономлено
  - Анимация во время процесса суммаризации

#### **3.3. Обновить `ConversationViewModel`**
- **Файл**: `features/conversation/src/commonMain/kotlin/ru/llm/agent/presentation/viewmodel/ConversationViewModel.kt`
- В методе `start()` добавить подписку на `getSummarizationInfo()`
- Добавить метод `checkAndSummarizeIfNeeded()` который вызывается перед отправкой сообщения
- Обновить `sendMessageToSingleAi()`:
  ```kotlin
  // ПЕРЕД отправкой сообщения
  checkAndSummarizeIfNeeded()
  ```

#### **3.4. Визуализация суммаризированных сообщений**
- В `MessageItem` добавить визуальную индикацию для суммаризированных сообщений
- Например, фон с другим цветом или иконка 📝 рядом с текстом

---

## 🔄 Логика работы механизма

### Шаг 1: Проверка перед отправкой сообщения
```
User отправляет сообщение
  ↓
ViewModel → checkAndSummarizeIfNeeded()
  ↓
Подсчитать requestTokens для нового сообщения
  ↓
Вычислить: (usedTokens + requestTokens) / maxTokens
  ↓
Если > 0.75 → вызвать SummarizeHistoryUseCase
```

### Шаг 2: Процесс суммаризации
```
SummarizeHistoryUseCase
  ↓
1. Получить все сообщения диалога (кроме SYSTEM)
  ↓
2. Разделить на:
   - Старые (для суммаризации): все кроме последних 3
   - Новые (сохранить): последние 3 сообщения
  ↓
3. Объединить старые сообщения в один текст
  ↓
4. Вызвать summarizeYandexGPTText(text, model, maxTokens=300)
  ↓
5. Сохранить суммаризацию как SYSTEM сообщение с isSummarized=true
  ↓
6. Удалить старые сообщения из БД
  ↓
7. Обновить SummarizationInfo
```

### Шаг 3: Обновление UI
```
TokenUsageProgressBar показывает:
  - "📝 История сжата: 5 сообщений (сохранено ~1200 токенов)"
  - Анимация во время процесса
  - Зеленый цвет индикатора после суммаризации
```

---

## 🎨 UI/UX Детали

### TokenUsageProgressBar - новый дизайн
```
┌─────────────────────────────────────────────┐
│ Использование токенов           3200 / 8000 │
│                                              │
│ ████████████░░░░░░░░░░░░░░░░░░░░░░ 40%     │
│                                              │
│ 📝 История сжата: 5 сообщений               │
│ Сэкономлено: ~1200 токенов                  │
└─────────────────────────────────────────────┘
```

### Во время суммаризации
```
┌─────────────────────────────────────────────┐
│ Использование токенов           6400 / 8000 │
│                                              │
│ ████████████████████████░░░░░░░░ 80%        │
│                                              │
│ ⏳ Сжатие истории...                        │
└─────────────────────────────────────────────┘
```

### Суммаризированное сообщение в чате
```
┌───────────────────────────────────┐
│ 📝 [Краткое содержание диалога]   │
│                                   │
│ В предыдущих сообщениях обсужда-  │
│ лось: архитектура KMP проекта...  │
│                                   │
│ Сжато: 5 сообщений                │
│ 15:30                             │
└───────────────────────────────────┘
```

---

## 📦 Порядок реализации

### 1. Domain Layer
- Создать `SummarizationInfo` модель
- Добавить `isSummarized` в `ConversationMessage`
- Создать `SummarizeHistoryUseCase`
- Добавить методы в `ConversationRepository` интерфейс

### 2. Data Layer
- Обновить Room entity с миграцией
- Расширить DAO методы
- Реализовать новые методы в `ConversationRepositoryImpl`

### 3. Presentation Layer
- Обновить `ConversationUIState`
- Расширить `TokenUsageProgressBar` компонент
- Добавить логику в `ConversationViewModel`
- Обновить визуализацию в `MessageItem`

### 4. Интеграция
- Подключить use case в Koin DI
- Протестировать работу механизма
- Настроить пороги и параметры

### 5. Тестирование
- Проверить суммаризацию на длинных диалогах
- Убедиться, что не теряется контекст
- Проверить корректность подсчета токенов

---

## ⚙️ Конфигурация

Параметры можно вынести в `ConversationContext` или настройки:

```kotlin
data class SummarizationConfig(
    /** Порог использования токенов для запуска суммаризации (0.0 - 1.0) */
    val tokenThreshold: Double = 0.75,

    /** Сколько последних сообщений НЕ суммаризировать */
    val keepLastMessages: Int = 3,

    /** Максимальное количество токенов для суммаризации */
    val summarizationMaxTokens: Int = 300,

    /** Минимальное количество сообщений для суммаризации */
    val minMessagesToSummarize: Int = 5
)
```

---

## 🔍 Дополнительные улучшения (опционально)

1. **Кнопка ручной суммаризации** в UI для явного управления
2. **История суммаризаций** - логировать каждую операцию
3. **Умная суммаризация** - не суммаризировать важные сообщения (с метками)
4. **Прогрессивная суммаризация** - суммаризировать уже суммаризированное
5. **Уведомления** - показать toast при автоматической суммаризации

---

## ✅ Преимущества решения

- ✅ Автоматическое управление токенами
- ✅ Сохранение контекста диалога
- ✅ Прозрачность процесса для пользователя
- ✅ Минимальное влияние на существующий код
- ✅ Следование Clean Architecture
- ✅ Кроссплатформенность (Android + Desktop)