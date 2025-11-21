# MCP Server - Changelog

## 2025-11-20 - Добавлена поддержка Claude Desktop

### Новые файлы

**Основной функционал:**
- `StdioServer.kt` - Stdio транспорт для подключения к Claude Desktop через stdin/stdout
- `run-mcp-server.sh` - Скрипт-хелпер для запуска MCP сервера

**Документация:**
- `README_CLAUDE_INTEGRATION.md` - Полное руководство по интеграции с Claude Desktop
- `QUICKSTART.md` - Быстрый старт за 3 шага
- `CHANGELOG_MCP.md` - Этот файл

**Примеры и тестирование:**
- `claude-desktop-config.json` - Пример конфигурации для Claude Desktop
- `test-mcp-server.sh` - Скрипт для тестирования сервера
- `test-commands.jsonl` - Примеры JSON-RPC команд для ручного тестирования

### Изменения в существующих файлах

**`server/build.gradle.kts`:**
- Добавлена новая Gradle задача `runStdio` для запуска сервера в stdio режиме
- Автоматическая передача переменных окружения из `local.properties`

**`README.md`** (корневой):
- Добавлена секция "MCP Server - Интеграция с Claude Desktop"
- Описание доступных инструментов
- Ссылки на документацию

### Функциональность

MCP сервер теперь поддерживает два режима работы:

1. **HTTP сервер** (существующий, `KtorServer.kt`)
   - Запуск: `./gradlew :server:run`
   - Используется для интеграции с вашим Android/Desktop приложением
   - Endpoint: `http://localhost:8081/mcp`

2. **Stdio сервер** (новый, `StdioServer.kt`)
   - Запуск: `./gradlew :server:runStdio` или `./server/run-mcp-server.sh`
   - Используется для подключения к Claude Desktop и другим MCP клиентам
   - Протокол: JSON-RPC через stdin/stdout

### Доступные инструменты

Все 13 инструментов доступны в обоих режимах:

**Погода (1):**
- getWeather

**Trello базовые (4):**
- trello_getCards
- trello_createCard
- trello_getCard
- trello_getSummary

**Trello умные (4):**
- trello_quickTask
- trello_moveCard
- trello_updateCard
- trello_searchCards

**Trello массовые (3):**
- trello_batchCreateCards
- trello_bulkUpdate
- trello_bulkMove

### Требования

**Обязательные:**
- Kotlin 2.2.0+
- Gradle 8.14.3+
- JVM 17+

**Опциональные (для функциональности):**
- Trello API Key + Token (для Trello инструментов)
- OpenWeatherMap API Key (для погоды)

### Примеры использования

**Локальное тестирование:**
```bash
./gradlew :server:runStdio
# Введите:
{"jsonrpc":"2.0","id":"1","method":"initialize","params":{}}
```

**Подключение к Claude Desktop:**
```json
{
  "mcpServers": {
    "llm-agent": {
      "command": "/path/to/project/server/run-mcp-server.sh",
      "env": {
        "TRELLO_API_KEY": "your_key",
        "TRELLO_TOKEN": "your_token",
        "OPENWEATHER_API_KEY": "your_key"
      }
    }
  }
}
```

**Использование в Claude Desktop:**
```
Получи погоду в Москве
Создай задачу "Написать отчет" на завтра в Trello (boardId: YOUR_BOARD_ID)
Покажи статистику моей доски (boardId: YOUR_BOARD_ID)
```

### Безопасность

- ✅ API ключи через переменные окружения
- ✅ `local.properties` в `.gitignore`
- ✅ Логи только в stderr (stdout занят JSON-RPC)
- ✅ Graceful shutdown при получении EOF

### Совместимость

**Протестировано с:**
- Claude Desktop (macOS)
- Прямой вызов через stdin/stdout

**Должно работать с:**
- Любым MCP-совместимым клиентом
- Windows / Linux / macOS

### Известные ограничения

1. Один процесс Gradle может быть медленным при запуске - рассмотрите использование JAR файла для продакшена
2. `disabled` поле в JSON не является стандартным - просто удалите секцию если не хотите использовать
3. Логи сервера идут в stderr - используйте Developer Tools в Claude Desktop для просмотра

### Roadmap

- [ ] Создание standalone JAR с правильным Main-Class для stdio
- [ ] Добавление более продвинутых Trello операций
- [ ] Поддержка других API (GitHub, Jira, etc.)
- [ ] Websocket транспорт для real-time коммуникации
- [ ] Кэширование результатов инструментов

### Ссылки

- [MCP Specification](https://modelcontextprotocol.io/)
- [Claude Desktop](https://claude.ai/download)
- [MCP Kotlin SDK](https://github.com/modelcontextprotocol/kotlin-sdk)