# MCP Feature Module

## Описание

Изолированный feature модуль для работы с MCP (Model Context Protocol) инструментами. Следует принципам Clean Architecture и может быть легко интегрирован в любой экран приложения.

## Структура модуля

```
features/mcp/
├── src/commonMain/kotlin/ru/llm/agent/mcp/
│   ├── model/                           # Модели данных
│   │   └── McpToolExecutionStatus.kt    # Статус выполнения инструмента
│   ├── presentation/
│   │   ├── state/                       # State management
│   │   │   ├── McpState.kt              # UI State
│   │   │   └── McpEvent.kt              # UI Events
│   │   ├── viewmodel/
│   │   │   └── McpViewModel.kt          # ViewModel для бизнес-логики
│   │   └── ui/                          # UI компоненты
│   │       ├── McpToolsPanel.kt         # Главная панель управления
│   │       ├── McpToolCard.kt           # Карточка инструмента
│   │       └── McpToolExecutionStatus.kt # Индикатор статуса
│   └── di/
│       └── McpKoin.kt                   # Koin DI модуль
└── build.gradle.kts
```

## Зависимости

Модуль зависит только от:
- `llm-domain` - для доступа к use cases и репозиториям
- `core/utils` - для утилит
- Jetpack Compose & Material 3 - для UI
- Koin - для dependency injection

## Установка

### 1. Модуль уже добавлен в проект

Модуль уже настроен в `settings.gradle.kts`:
```kotlin
include(":features:mcp")
```

### 2. Koin модуль уже зарегистрирован

В `llm-app/src/commonMain/kotlin/ru/llm/agent/common/app/InitKoin.kt`:
```kotlin
import ru.llm.agent.mcp.di.mcpModule

fun KoinApplication.initKoinApp(...) {
    modules(
        // ... другие модули
        mcpModule,
    )
}
```

## Использование

### Базовое использование в Composable экране

```kotlin
import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel
import ru.llm.agent.mcp.presentation.ui.McpToolsPanel
import ru.llm.agent.mcp.presentation.viewmodel.McpViewModel

@Composable
fun YourScreen() {
    // Получаем ViewModel через Koin
    val mcpViewModel: McpViewModel = koinViewModel()

    // Встраиваем панель MCP в ваш экран
    Column {
        // ... ваш контент

        // MCP панель
        McpToolsPanel(
            viewModel = mcpViewModel,
            modifier = Modifier.fillMaxWidth()
        )

        // ... остальной контент
    }
}
```

### Интеграция в ConversationScreen

Пример интеграции в существующий ConversationScreen:

```kotlin
@Composable
fun ConversationScreen(
    onNavigateToOptions: (String) -> Unit,
) {
    // ... существующий код

    val mcpViewModel: McpViewModel = koinViewModel()

    Scaffold(
        topBar = { /* ... */ },
        bottomBar = { /* ... */ }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // MCP панель (опционально сворачиваемая)
            McpToolsPanel(
                viewModel = mcpViewModel,
                modifier = Modifier.fillMaxWidth()
            )

            // Список сообщений
            LazyColumn(modifier = Modifier.weight(1f)) {
                // ... сообщения
            }

            // Поле ввода
            InputBar(/* ... */)
        }
    }
}
```

## API

### McpViewModel

**State:**
- `availableTools: List<McpToolInfo>` - список доступных MCP инструментов
- `isEnabled: Boolean` - включены ли MCP инструменты
- `isLoadingTools: Boolean` - загружаются ли инструменты
- `currentExecution: McpToolExecutionStatus?` - текущий выполняемый инструмент
- `executionHistory: List<McpToolExecutionStatus>` - история выполнений
- `error: String?` - ошибка загрузки
- `isPanelExpanded: Boolean` - развёрнута ли панель

**Events:**
- `LoadTools` - загрузить список инструментов
- `ToggleEnabled(enabled: Boolean)` - переключить использование MCP
- `ExecuteTool(toolName, arguments)` - выполнить инструмент
- `CancelExecution` - отменить выполнение
- `ClearHistory` - очистить историю
- `TogglePanel` - свернуть/развернуть панель
- `ClearError` - очистить ошибку

### McpToolsPanel

Главный UI компонент модуля.

**Параметры:**
- `viewModel: McpViewModel` - ViewModel для управления состоянием
- `modifier: Modifier` - модификатор для кастомизации

**Возможности:**
- Переключатель включения/выключения MCP инструментов
- Список доступных инструментов с описанием
- Индикатор текущего выполнения
- История последних 5 выполнений
- Сворачивание/разворачивание панели

### McpToolCard

Карточка для отображения одного MCP инструмента.

**Параметры:**
- `tool: McpToolInfo` - информация об инструменте
- `onExecute: (String) -> Unit` - callback при нажатии "Выполнить"
- `modifier: Modifier` - модификатор

## Архитектура

Модуль следует MVVM паттерну и Clean Architecture:

```
UI Layer (McpToolsPanel)
    ↓
ViewModel (McpViewModel)
    ↓
Domain Layer (GetMcpToolsUseCase)
    ↓
Repository (McpRepository)
    ↓
Data Layer (McpClient)
```

## Преимущества

1. **Изоляция** - модуль полностью самодостаточен
2. **Переиспользуемость** - можно встроить в любой экран
3. **Clean Architecture** - зависит только от domain layer
4. **Тестируемость** - легко тестировать в изоляции
5. **Масштабируемость** - легко расширять функциональность

## Roadmap

- [ ] Добавить возможность ввода аргументов для инструментов через UI
- [ ] Реализовать фактический вызов инструментов через use case
- [ ] Добавить поддержку отмены выполнения инструмента
- [ ] Добавить кэширование результатов выполнения
- [ ] Создать unit тесты для ViewModel
- [ ] Добавить UI тесты для компонентов