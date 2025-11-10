# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Kotlin Multiplatform (KMP)** project implementing an LLM Agent system with tool-calling capabilities. The project follows a daily challenge format, progressively building from basic chat to sophisticated multi-agent systems.

Target platforms: **Android** and **Desktop (JVM)**

## Code Style and Conventions

### Language for Comments and Documentation

**IMPORTANT**: All code comments, KDoc documentation, and commit messages **MUST be written in Russian**.

This includes:
- KDoc comments (`/** */`) for classes, functions, and properties
- Single-line comments (`//`)
- Multi-line comments (`/* */`)
- Git commit messages
- TODO comments
- Code review comments

**Example:**
```kotlin
/**
 * Кроссплатформенный интерфейс логгера для соблюдения Clean Architecture.
 * Позволяет логировать без platform-specific зависимостей в domain layer.
 */
public interface Logger {
    /**
     * Логирование информационного сообщения
     */
    public fun info(message: String)
}
```

**Exception**: Technical identifiers (class names, function names, variable names) remain in English as per Kotlin conventions.

## Common Commands

### Build & Run
```bash
./gradlew build              # Build entire project
./gradlew clean              # Clean build directories
./gradlew desktopJar         # Build desktop JAR
./gradlew run                # Run desktop application
./gradlew assembleDebug      # Build Android debug APK
./gradlew assembleRelease    # Build Android release APK
```

### Testing
```bash
./gradlew test               # Run all unit tests
./gradlew allTests           # Run tests for all targets with aggregated report
./gradlew desktopTest        # Run desktop-specific tests
./gradlew testDebugUnitTest  # Run Android debug unit tests
./gradlew connectedAndroidTest  # Run instrumented tests on device
```

### Quality Checks
```bash
./gradlew check              # Run all checks
./gradlew lint               # Run lint
./gradlew lintDebug          # Run lint on debug variant
./gradlew lintFix            # Apply lint fixes
```

## Architecture Overview

### Module Structure

The project follows **Clean Architecture** with clear layer separation:

```
├── llm-app/              # Main application (Android + Desktop entry points)
├── llm-domain/           # Domain layer: use cases, repository interfaces, models
├── llm-data/             # Data layer: repository implementations, API clients, Room DB
├── features/             # Feature modules (UI + ViewModels)
│   ├── conversation/     # Main chat interface with AI
│   └── addoptions/       # Settings and configuration
├── core/
│   ├── uikit/            # Shared UI components & theme
│   └── utils/            # Platform abstractions and utilities
├── server/               # Ktor server (MCP server implementation)
└── build-logic/          # Custom Gradle convention plugins
```

### Dependency Flow
- **llm-app** → features/* + llm-data + llm-domain
- **features/** → llm-domain only
- **llm-data** → llm-domain (implements interfaces)
- All modules can depend on **core/utils**

### Key Architectural Patterns

**Clean Architecture Layers:**
1. **Domain** (`llm-domain/`): Pure business logic, no framework dependencies
   - Explicit API mode enabled (all public APIs must be declared)
   - Repository interfaces only
   - Use cases for each business operation

2. **Data** (`llm-data/`): Infrastructure implementations
   - Repository implementations
   - Ktor HTTP clients for API calls
   - Room database for local persistence
   - MCP client for tool integration

3. **Presentation** (`features/*/`, `llm-app/`): UI and state management
   - Jetpack Compose UI
   - MVVM pattern with ViewModels
   - StateFlow for reactive state
   - Koin for dependency injection

## LLM/AI Integration

### Agent System Architecture

The core agent functionality is in `InteractYaGptWithMcpService`:

1. **Fetches tools** from MCP server
2. **Sends message** to YandexGPT with tool definitions
3. **Handles tool calls** returned by LLM
4. **Executes tools** via MCP protocol
5. **Iterates** up to 3 times for complex tool-based tasks

### Data Flow
```
User Input
  → ViewModel (features/conversation)
    → Use Case (ConversationUseCase in llm-domain)
      → Repository (ConversationRepository implementation in llm-data)
        → YandexApi + Room Database
          → LLM Response
            → Tool call detection
              → MCP execution
                → Return to LLM
                  → Final response to UI
```

### API Configuration

The project uses multiple LLM providers. API keys must be set in `local.properties`:

```properties
YANDEX_API_KEY=...
OPENROUTER_API_KEY=...
PROXY_API_KEY=...
```

**Note**: `local.properties` is gitignored. Never commit API keys.

### Conversation Management

- Conversations stored in Room database (`MessageEntity`, `ContextEntity`)
- System prompts define agent behavior and expected JSON response format
- Token tracking per message via YandexGPT tokenizer
- Temperature and max token limits configurable per conversation
- Context window management via text summarization

### Multi-Agent Support

- `ExecuteChainTwoAgentsUseCase`: Chain multiple agents together
- Agent interaction patterns: one agent checks another's work
- Each agent has independent conversation context

## Technology Stack

### Core
- **Kotlin 2.2.0** with Gradle 8.14.3
- **Compose Multiplatform 1.8.2** for shared UI
- **Coroutines 1.10.2** + Flows for async operations
- **Kotlinx Serialization** for JSON handling

### Networking & Storage
- **Ktor 3.2.1** (client & server)
- **Room 2.7.2** (KMP-compatible database)
- **SQLite Bundled** driver

### Dependency Injection & Architecture
- **Koin 4.1.0** for DI across all modules
- **Lifecycle ViewModel 2.9.1** for state management
- **Navigation Compose 2.9.1** for navigation

### APIs & Services
- **YandexGPT**: Primary LLM provider (`gpt://b1gonedr4v7ke927m32n/yandexgpt-lite`)
- **ProxyAPI**: Alternative LLM access
- **MCP (Model Context Protocol)**: Tool/function calling integration

## Important Conventions

### Package Structure
All code uses `ru.llm.agent.*` base package.

### State Management Pattern
ViewModels follow this pattern:
```kotlin
private val _state = MutableStateFlow(State.empty())
val state = _state.asStateFlow()  // Expose as read-only

sealed interface Event { ... }    // User actions
```

### API Response Handling
Use `NetworkResult<T>` sealed class:
```kotlin
sealed class NetworkResult<out T> {
    object Loading : NetworkResult<Nothing>()
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String) : NetworkResult<Nothing>()
}
```

Extension functions: `doActionIfLoading { }`, `doActionIfSuccess { }`, `doActionIfError { }`

### Platform-Specific Code
Use expect/actual pattern:
```kotlin
// commonMain
expect fun platformSpecificFunction(): String

// androidMain
actual fun platformSpecificFunction(): String = "Android"

// desktopMain
actual fun platformSpecificFunction(): String = "Desktop"
```

### Database Schema
- Schema files in `llm-data/schemas/`
- Room uses KMP-compatible APIs
- Entities: `MessageEntity`, `ContextEntity`
- DAOs: `MessageDao`, `ContextDao`

### Navigation
Jetpack Navigation Compose with sealed class routes:
```kotlin
sealed class Screen(val route: String) {
    object Conversations : Screen("conversations")
    object Options : Screen("options/{conversationId}")
}
```

## Custom Gradle Plugins

Located in `build-logic/kmp-library-base/`:
- `frameio-kmplib`: Base KMP library configuration
- `frameio-compose`: Compose Multiplatform setup
- `frameio-android-library`: Android library conventions

Apply in feature modules:
```kotlin
plugins {
    alias(libs.plugins.frameio.kmplib)
    alias(libs.plugins.frameio.compose)
}
```

## Key Files & Entry Points

### Application Entry Points
- **Desktop**: `llm-app/src/desktopMain/kotlin/ru/llm/agent/AgentDesktop.kt`
- **Android**: `llm-app/src/androidMain/kotlin/ru/llm/agent/AgentApplication.kt`

### DI Configuration
- Main Koin modules: `llm-app/src/commonMain/kotlin/ru/llm/agent/di/`
- Feature-specific modules in each `features/*/di/` directory

### Database
- Database class: `llm-data/src/commonMain/kotlin/ru/llm/agent/db/AgentDatabase.kt`
- DAOs in `llm-data/src/commonMain/kotlin/ru/llm/agent/db/dao/`

### API Clients
- `llm-data/src/commonMain/kotlin/ru/llm/agent/api/yandex/YandexApi.kt`
- `llm-data/src/commonMain/kotlin/ru/llm/agent/api/proxy/ProxyApi.kt`
- `llm-data/src/commonMain/kotlin/ru.llm.agent/McpClient.kt`

### MCP Server
- Ktor server implementation: `server/src/main/kotlin/`
- Defines tools available to AI agents

## Project Context

This is a **learning project** following a progressive curriculum (see README.md):
- **Days 1-3**: Basic agent → Structured output → Goal-oriented conversations
- **Days 4-6**: Temperature tuning → Model comparison → Chain of Thought
- **Days 7-8**: Multi-agent interaction → Token management

The codebase demonstrates evolution from simple chat to sophisticated agentic systems. Some modules may represent experimental features from specific days.

## Development Notes

### Adding a New Feature Module

1. Create module in `features/`
2. Apply convention plugins in `build.gradle.kts`
3. Depend only on `llm-domain`, never `llm-data`
4. Create Koin module in feature's `di/` package
5. Register in main app's DI configuration

### Adding a New Use Case

1. Define interface/class in `llm-domain/src/commonMain/kotlin/ru.llm.agent/usecase/`
2. Mark public APIs explicitly (explicit API mode enabled)
3. Depend only on repository interfaces from `llm-domain`
4. Implement repository methods in `llm-data` if needed
5. Register in Koin module

### Working with Room Database

1. Define entities in `llm-data/.../db/entity/`
2. Create/update DAO in `llm-data/.../db/dao/`
3. Add DAO to `AgentDatabase.kt`
4. Update schema version if needed
5. Run build to generate Room implementations

### Adding MCP Tools

1. Define tool in `server/` Ktor application
2. Follow MCP protocol specification
3. Update `McpClient` if needed to support new tool types
4. Test with `InteractYaGptWithMcpService`

## Troubleshooting

### Build Issues
- Clean build: `./gradlew clean build`
- Invalidate caches and restart IDE
- Check `local.properties` has required API keys

### API Key Issues
- Ensure `local.properties` exists in project root
- Keys format: `KEY_NAME=value` (no quotes)
- File is gitignored - never commit it

### Database Migration Issues
- Schema files in `llm-data/schemas/`
- Increment version in `AgentDatabase`
- Provide migration path or use `fallbackToDestructiveMigration()`

### MCP Connection Issues
- Check server is running: `./gradlew :server:run`
- Verify MCP client configuration in use cases
- Check network connectivity and ports
