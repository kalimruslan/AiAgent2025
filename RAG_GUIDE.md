# RAG (Retrieval-Augmented Generation) в LlmAgent2025

## Что такое RAG?

RAG (Retrieval-Augmented Generation) - это техника, которая улучшает ответы LLM, предоставляя дополнительный контекст из внешней базы знаний.

**Как это работает:**
1. Документы разбиваются на небольшие фрагменты (chunks)
2. Для каждого фрагмента создаётся векторное представление (embedding)
3. При запросе пользователя создаётся embedding запроса
4. Ищутся наиболее похожие фрагменты по косинусному сходству
5. Найденные фрагменты добавляются в контекст LLM
6. LLM генерирует ответ с учётом предоставленного контекста

## Архитектура реализации

Проект следует Clean Architecture с разделением на слои:

```
┌─────────────────────────────────────────────────────────────┐
│                      Presentation Layer                      │
│                    (ViewModel + UI - TODO)                   │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────┴────────────────────────────────┐
│                        Domain Layer                          │
│                                                              │
│  Use Cases:                                                  │
│  ├─ IndexTextUseCase           - индексация текста          │
│  ├─ SearchRagDocumentsUseCase  - поиск документов           │
│  ├─ GetRagIndexStatsUseCase    - статистика                 │
│  ├─ ClearRagIndexUseCase       - очистка индекса            │
│  └─ AskWithRagUseCase          - вопрос с RAG контекстом    │
│                                                              │
│  Models:                                                     │
│  ├─ RagDocument                - документ с similarity       │
│  └─ RagIndexResult             - результат индексации        │
│                                                              │
│  Repository Interface:                                       │
│  └─ RagRepository              - интерфейс репозитория       │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────┴────────────────────────────────┐
│                         Data Layer                           │
│                                                              │
│  API:                                                        │
│  └─ OllamaApi                  - клиент для Ollama           │
│                                                              │
│  Services:                                                   │
│  ├─ TextChunker                - разбиение на chunks         │
│  ├─ VectorStore                - хранение embeddings         │
│  └─ EmbeddingService           - генерация embeddings        │
│                                                              │
│  Repository Implementation:                                  │
│  └─ RagRepositoryImpl          - реализация репозитория      │
└──────────────────────────────────────────────────────────────┘
```

## Установка и настройка

### 1. Установка Ollama

**macOS:**
```bash
brew install ollama
```

**Linux:**
```bash
curl https://ollama.ai/install.sh | sh
```

**Windows:**
Скачайте с https://ollama.ai/download

### 2. Запуск Ollama

```bash
ollama serve
```

Ollama по умолчанию запускается на `http://localhost:11434`

### 3. Загрузка модели для эмбеддингов

```bash
ollama pull nomic-embed-text
```

**nomic-embed-text** - это модель для генерации embeddings, оптимизированная для семантического поиска.

Характеристики:
- Размер модели: ~274 MB
- Размерность вектора: 768
- Контекст: 8192 токенов

### 4. Проверка работы

```bash
curl http://localhost:11434/api/embed -d '{
  "model": "nomic-embed-text",
  "input": "Hello, world!"
}'
```

## Использование RAG в коде

### Пример 1: Индексация документа

```kotlin
class MyViewModel(
    private val indexTextUseCase: IndexTextUseCase
) : ViewModel() {

    suspend fun indexKnowledgeBase() {
        val text = """
            Kotlin Multiplatform позволяет создавать
            кроссплатформенные приложения с общей логикой.
        """

        val result = indexTextUseCase(
            text = text,
            sourceId = "kotlin-guide"
        )

        println("Проиндексировано чанков: ${result.chunksIndexed}")
    }
}
```

### Пример 2: Поиск релевантных документов

```kotlin
class SearchViewModel(
    private val searchUseCase: SearchRagDocumentsUseCase
) : ViewModel() {

    suspend fun search(query: String) {
        val documents = searchUseCase(
            query = query,
            topK = 5,           // топ-5 результатов
            threshold = 0.3      // минимальная схожесть 0.3
        )

        documents.forEach { doc ->
            println("Схожесть: ${doc.similarity}")
            println("Текст: ${doc.text}")
            println("---")
        }
    }
}
```

### Пример 3: Вопрос с RAG контекстом

```kotlin
class ChatViewModel(
    private val askWithRagUseCase: AskWithRagUseCase
) : ViewModel() {

    fun askQuestion(conversationId: String, question: String, provider: LlmProvider) {
        viewModelScope.launch {
            // Автоматически находит релевантные документы
            // и добавляет их в контекст перед отправкой в LLM
            askWithRagUseCase(
                conversationId = conversationId,
                userMessage = question,
                provider = provider,
                topK = 3,
                threshold = 0.3
            ).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        println("Ответ LLM: ${result.data.content}")
                    }
                    is NetworkResult.Error -> {
                        println("Ошибка: ${result.error}")
                    }
                    is NetworkResult.Loading -> {
                        println("Загрузка...")
                    }
                }
            }
        }
    }
}
```

## Структура данных

### Document (Data Layer)

```kotlin
data class Document(
    val id: String,                    // уникальный ID
    val text: String,                  // текст чанка
    val embedding: List<Double>,       // вектор эмбеддинга
    val metadata: Map<String, String>  // метаданные
)
```

### RagDocument (Domain Layer)

```kotlin
data class RagDocument(
    val text: String,                  // текст документа
    val similarity: Double,            // оценка схожести (0.0 - 1.0)
    val metadata: Map<String, String>  // метаданные
)
```

## Компоненты системы

### 1. OllamaApi

HTTP клиент для взаимодействия с Ollama API.

**Методы:**
- `getEmbedding(text: String, model: String): OllamaEmbeddingResponse`

**Конфигурация:**
```kotlin
OllamaApi(
    client = httpClient,
    baseUrl = "http://localhost:11434"  // можно изменить
)
```

### 2. TextChunker

Разбивает текст на смысловые фрагменты с перекрытием.

**Параметры:**
- `chunkSize: Int = 500` - размер чанка в символах
- `overlap: Int = 50` - перекрытие между чанками

**Алгоритм:**
1. Разбивает по параграфам (`\n\n`)
2. Для больших параграфов разбивает по предложениям
3. Добавляет overlap для сохранения контекста

### 3. VectorStore

**Персистентное хранилище** для векторов с поиском по косинусной схожести. Сохраняет документы в Room базу данных.

**Методы:**
- `addDocument(document, sourceId, chunkIndex, model)` - добавить документ
- `addDocuments(docs, sourceId, model)` - батч-вставка документов
- `search(queryEmbedding, topK, threshold)` - поиск похожих
- `size()` - количество документов
- `clear()` - очистить хранилище
- `getAllDocuments()` - получить все документы

**База данных:**
- Таблица: `rag_documents`
- Поля: id, text, embedding (JSON), sourceId, chunkIndex, model, timestamp
- Embeddings сохраняются как JSON строки для кроссплатформенности

**Cosine Similarity:**
```
similarity = (A · B) / (||A|| * ||B||)
```
Результат от -1 до 1, где 1 = полное совпадение.

### 4. EmbeddingService

Высокоуровневый сервис, объединяющий работу всех компонентов.

**Методы:**
- `indexText(text, sourceId)` - индексировать текст
- `search(query, topK, threshold)` - поиск
- `getIndexedDocumentsCount()` - статистика
- `clearIndex()` - очистка

## Настройка параметров

### Размер чанка (chunkSize)

**Рекомендации:**
- 200-300 символов - для коротких фактов
- 500-700 символов - универсальный вариант (по умолчанию)
- 1000+ символов - для больших связных текстов

### Перекрытие (overlap)

**Рекомендации:**
- 10-20% от chunkSize
- По умолчанию: 50 символов для chunkSize=500

### Порог схожести (threshold)

**Рекомендации:**
- 0.0 - вернуть всё
- 0.3 - слабое сходство (по умолчанию)
- 0.5 - среднее сходство
- 0.7+ - высокое сходство

### Количество результатов (topK)

**Рекомендации:**
- 3-5 - для коротких ответов (по умолчанию)
- 5-10 - для подробных ответов
- Слишком много результатов могут переполнить контекст LLM

## Тестирование

### Запуск тестового файла

```bash
# Убедитесь, что Ollama запущена
ollama serve

# Проверьте модель
ollama list | grep nomic-embed-text

# Запустите тест (если настроен)
./gradlew :test
```

### Ручное тестирование через Koin

См. файл `RagTest.kt` в корне проекта.

## Реализованные функции

1. ✅ **Персистентное хранилище** - данные сохраняются в Room базе данных
2. ✅ **UI для добавления знаний** - диалог с двумя режимами (текст и файл)
3. ✅ **Загрузка из файлов** - поддержка загрузки текстовых документов
4. ✅ **Батч-индексация** - эффективная вставка множества документов
5. ✅ **Статистика индекса** - отображение количества проиндексированных документов

## Использование UI

### Добавление знаний через интерфейс

1. Включите переключатель "RAG" в панели управления
2. Нажмите кнопку "Добавить знания"
3. Выберите режим ввода:
   - **Ввести текст** - для коротких текстов или вставки из буфера
   - **Загрузить файл** - для длинных документов

#### Режим "Ввести текст":
- Введите название источника (например, "kotlin-guide")
- Вставьте текст в поле (до 300dp высоты, неограниченное количество строк)
- Нажмите "Добавить"

#### Режим "Загрузить файл":
- Введите название источника (или оставьте пустым - заполнится автоматически)
- Введите путь к файлу (например, `/Users/user/documents/guide.txt`)
- Файл автоматически загрузится и отобразится превью (первые 500 символов)
- Нажмите "Добавить"

### Просмотр статистики

- Количество проиндексированных документов отображается в панели RAG
- После перезапуска приложения индекс сохраняется

### Очистка индекса

- Нажмите кнопку "Очистить" для полного удаления всех документов из базы знаний

## Ограничения текущей реализации

1. ~~**In-memory хранилище**~~ ✅ Решено - используется Room
2. ~~**Нет персистентности**~~ ✅ Решено - данные сохраняются в БД
3. **Простой chunking** - не учитывает семантику на уровне NLP
4. **Нет управления источниками** - нельзя удалить отдельный документ
5. **Только текстовые файлы** - нет поддержки PDF, DOCX

## Улучшения (TODO)

### Краткосрочные:
- [x] Добавить сохранение индекса в Room
- [x] Добавить UI для загрузки файлов
- [ ] Реализовать удаление документов по источнику
- [ ] Поддержка PDF, DOCX форматов
- [ ] Прогресс индексации
- [ ] Drag & Drop для файлов

### Долгосрочные:
- [ ] Использовать векторную БД (Chroma, Qdrant)
- [ ] Semantic chunking (на основе BERT)
- [ ] Гибридный поиск (keyword + vector)
- [ ] Re-ranking результатов
- [ ] Мульти-язычность

## Альтернативы Ollama

Если Ollama недоступна, можно использовать:

1. **OpenAI Embeddings API**
   ```kotlin
   POST https://api.openai.com/v1/embeddings
   model: "text-embedding-3-small"
   ```

2. **Sentence Transformers (через API)**
   - Hugging Face Inference API
   - Локальный FastAPI сервер

3. **Google Vertex AI Embeddings**

4. **Cohere Embed API**

## Производительность

### Время генерации embedding:
- ~50-100ms для текста в 500 символов (локальная Ollama)
- ~200-500ms через API (зависит от сети)

### Время поиска:
- ~1-5ms для 1000 документов (in-memory)
- Линейная сложность O(n) для cosine similarity

### Рекомендации:
- Для >10k документов используйте векторную БД
- Кэшируйте embeddings
- Используйте батчинг для индексации

## Troubleshooting

### Ошибка: "Connection refused localhost:11434"

**Решение:**
```bash
# Проверьте, запущена ли Ollama
ps aux | grep ollama

# Запустите Ollama
ollama serve
```

### Ошибка: "Model not found: nomic-embed-text"

**Решение:**
```bash
# Загрузите модель
ollama pull nomic-embed-text

# Проверьте список моделей
ollama list
```

### Низкая схожесть (similarity) результатов

**Возможные причины:**
1. Слишком маленький chunkSize - увеличьте до 500-700
2. Неподходящая модель - попробуйте другую
3. Разные языки - убедитесь, что модель поддерживает язык
4. Слишком узкий запрос - перефразируйте

### Out of Memory при большом количестве документов

**Решение:**
1. Увеличьте chunkSize - меньше чанков
2. Используйте более низкую размерность embedding
3. Переключитесь на персистентное хранилище

## Полезные ссылки

- [Ollama Documentation](https://github.com/ollama/ollama)
- [Nomic Embed Text Model](https://ollama.com/library/nomic-embed-text)
- [RAG Best Practices](https://www.pinecone.io/learn/retrieval-augmented-generation/)
- [Cosine Similarity](https://en.wikipedia.org/wiki/Cosine_similarity)

## Примеры использования

### Создание базы знаний о проекте

```kotlin
suspend fun buildProjectKnowledgeBase() {
    val files = listOf(
        "README.md",
        "CLAUDE.md",
        "docs/architecture.md"
    )

    files.forEach { filePath ->
        val content = File(filePath).readText()
        indexTextUseCase(
            text = content,
            sourceId = filePath
        )
    }
}
```

### RAG-powered чат-бот

```kotlin
suspend fun chatWithRAG(message: String, provider: LlmProvider) {
    askWithRagUseCase(
        conversationId = currentConversationId,
        userMessage = message,
        provider = provider,
        topK = 3
    ).collect { result ->
        when (result) {
            is NetworkResult.Success -> {
                // Используем ответ
                processResponse(result.data.content)
            }
            is NetworkResult.Error -> {
                handleError(result.error)
            }
            is NetworkResult.Loading -> {
                showLoading()
            }
        }
    }
}
```

## Заключение

RAG система в LlmAgent2025 предоставляет основу для работы с внешними знаниями. Текущая реализация является MVP и подходит для:
- Обучения концепции RAG
- Прототипирования
- Небольших баз знаний (<1000 документов)

Для production использования рекомендуется:
- Персистентное хранилище
- Векторная база данных
- Мониторинг качества поиска
- A/B тестирование параметров