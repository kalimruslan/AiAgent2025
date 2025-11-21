# Подключение MCP сервера к Claude Desktop

Этот документ описывает, как подключить ваш MCP сервер к Claude Desktop и другим MCP-совместимым клиентам.

## Обзор

Проект содержит два режима работы MCP сервера:

1. **HTTP сервер** (`KtorServer.kt`) - для использования в вашем Android/Desktop приложении
2. **Stdio сервер** (`StdioServer.kt`) - для подключения к Claude Desktop через stdin/stdout

## Предварительные требования

1. Установленный [Claude Desktop](https://claude.ai/download)
2. API ключи для сервисов (опционально):
   - Trello API Key и Token (для Trello инструментов)
   - OpenWeatherMap API Key (для погоды)

## Шаг 1: Подготовка переменных окружения

Создайте или обновите файл `local.properties` в корне проекта:

```properties
TRELLO_API_KEY=your_trello_api_key
TRELLO_TOKEN=your_trello_token
OPENWEATHER_API_KEY=your_openweather_api_key
```

**Где взять ключи:**

### Trello
1. Получите API Key: https://trello.com/power-ups/admin
2. Сгенерируйте Token по ссылке на той же странице

### OpenWeatherMap
1. Зарегистрируйтесь на https://openweathermap.org/
2. Получите API ключ в разделе "API keys"

## Шаг 2: Тестирование локального запуска

Проверьте, что сервер работает корректно:

```bash
# Запустить stdio сервер
./gradlew :server:runStdio

# Сервер ждет JSON-RPC команды через stdin
# Попробуйте отправить команду инициализации:
{"jsonrpc":"2.0","id":"1","method":"initialize","params":{}}
```

Вы должны увидеть ответ с информацией о сервере.

Для выхода нажмите `Ctrl+C` или `Ctrl+D`.

## Шаг 3: Настройка Claude Desktop

### macOS

1. Откройте файл конфигурации Claude Desktop:
```bash
nano ~/Library/Application\ Support/Claude/claude_desktop_config.json
```

2. Добавьте конфигурацию вашего сервера:

```json
{
  "mcpServers": {
    "llm-agent-trello": {
      "command": "/bin/sh",
      "args": [
        "-c",
        "cd /Users/YOUR_USERNAME/path/to/LlmAgent2025 && ./gradlew :server:runStdio --quiet"
      ],
      "env": {
        "TRELLO_API_KEY": "your_trello_api_key_here",
        "TRELLO_TOKEN": "your_trello_token_here",
        "OPENWEATHER_API_KEY": "your_openweather_api_key_here"
      }
    }
  }
}
```

**Важно:** Замените `/Users/YOUR_USERNAME/path/to/LlmAgent2025` на полный путь к вашему проекту.

### Windows

1. Откройте файл конфигурации:
```
%APPDATA%\Claude\claude_desktop_config.json
```

2. Добавьте конфигурацию:

```json
{
  "mcpServers": {
    "llm-agent-trello": {
      "command": "cmd",
      "args": [
        "/c",
        "cd C:\\path\\to\\LlmAgent2025 && gradlew.bat :server:runStdio --quiet"
      ],
      "env": {
        "TRELLO_API_KEY": "your_trello_api_key_here",
        "TRELLO_TOKEN": "your_trello_token_here",
        "OPENWEATHER_API_KEY": "your_openweather_api_key_here"
      }
    }
  }
}
```

### Linux

1. Откройте файл конфигурации:
```bash
nano ~/.config/Claude/claude_desktop_config.json
```

2. Используйте ту же конфигурацию, что и для macOS (с корректировкой пути).

## Шаг 4: Перезапуск Claude Desktop

1. Полностью закройте Claude Desktop
2. Запустите снова
3. Откройте Developer Tools (Cmd+Shift+I на macOS, Ctrl+Shift+I на Windows/Linux)
4. В консоли проверьте, что MCP сервер подключился без ошибок

## Шаг 5: Использование инструментов

Теперь вы можете использовать инструменты в чате с Claude:

**Примеры команд:**

```
Получи погоду в Москве
```

```
Покажи карточки с моей Trello доски (boardId: YOUR_BOARD_ID)
```

```
Создай быструю задачу "Написать отчет" на сегодня в Trello (boardId: YOUR_BOARD_ID)
```

## Доступные инструменты

### Погода
- `getWeather` - получить погоду для города

### Trello - Базовые операции
- `trello_getCards` - получить список карточек
- `trello_createCard` - создать новую карточку
- `trello_getCard` - получить информацию о карточке
- `trello_getSummary` - получить статистику по доске

### Trello - Композитные операции
- `trello_quickTask` - быстро создать задачу с умным парсингом дедлайна
- `trello_moveCard` - переместить карточку между списками
- `trello_updateCard` - обновить карточку
- `trello_searchCards` - поиск карточек по критериям

### Trello - Массовые операции
- `trello_batchCreateCards` - создать несколько карточек за раз
- `trello_bulkUpdate` - обновить несколько карточек
- `trello_bulkMove` - переместить несколько карточек

## Отладка

### Логи сервера

Логи MCP сервера можно увидеть в Developer Tools Claude Desktop:

1. Откройте Developer Tools (Cmd+Shift+I / Ctrl+Shift+I)
2. Перейдите в Console
3. Ищите сообщения от вашего сервера

### Проблемы с подключением

Если сервер не подключается:

1. Проверьте, что путь в конфигурации правильный
2. Убедитесь, что `./gradlew :server:runStdio` работает из терминала
3. Проверьте, что переменные окружения установлены корректно
4. Посмотрите логи в Developer Tools

### Проверка JSON-RPC коммуникации

Вы можете вручную тестировать JSON-RPC протокол:

```bash
./gradlew :server:runStdio
```

Затем вводите команды:

```json
{"jsonrpc":"2.0","id":"1","method":"initialize","params":{}}
{"jsonrpc":"2.0","id":"2","method":"tools/list","params":{}}
{"jsonrpc":"2.0","id":"3","method":"tools/call","params":{"name":"getWeather","arguments":{"city":"Moscow"}}}
```

## Альтернативный способ: Использование JAR файла

Вы можете собрать standalone JAR файл для более быстрого запуска:

1. Соберите JAR:
```bash
./gradlew :server:shadowJar
```

2. Измените конфигурацию Claude Desktop:

```json
{
  "mcpServers": {
    "llm-agent-trello": {
      "command": "java",
      "args": [
        "-jar",
        "/full/path/to/LlmAgent2025/server/build/libs/mcp-server.jar"
      ],
      "env": {
        "TRELLO_API_KEY": "your_key",
        "TRELLO_TOKEN": "your_token",
        "OPENWEATHER_API_KEY": "your_key"
      }
    }
  }
}
```

**Примечание:** В данный момент основной класс в JAR - `KtorServerKt`. Для stdio режима нужно изменить `Main-Class` в `build.gradle.kts`:

```kotlin
tasks {
    shadowJar {
        manifest {
            attributes["Main-Class"] = "ru.llm.agent.StdioServerKt"  // Изменено
        }
        archiveFileName.set("mcp-server.jar")
        mergeServiceFiles()
    }
}
```

## Дополнительные ресурсы

- [MCP Documentation](https://modelcontextprotocol.io/)
- [Claude Desktop Documentation](https://claude.ai/docs)
- [Trello API Documentation](https://developer.atlassian.com/cloud/trello/)
- [OpenWeatherMap API Documentation](https://openweathermap.org/api)

## Безопасность

⚠️ **Важно:**
- Никогда не коммитьте `local.properties` с реальными API ключами
- В конфигурации Claude Desktop используйте переменные окружения вместо хардкода ключей
- Ограничьте доступ к Trello токену только необходимыми правами

## Поддержка

При возникновении проблем:
1. Проверьте логи в Developer Tools
2. Убедитесь, что все зависимости установлены
3. Попробуйте запустить сервер вручную для тестирования