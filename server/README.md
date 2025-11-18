# MCP Server с интеграцией Яндекс.Трекер

MCP (Model Context Protocol) сервер на основе официального Kotlin SDK с поддержкой работы с Яндекс.Трекер.

## Возможности

### Базовые инструменты
- `echo` - возврат введенного текста
- `add` - сложение двух чисел
- `getCurrentTime` - получение текущего времени
- `calculate` - вычисление математических выражений

### Инструменты для работы с API
- **`getWeather`** - получение реальной погоды через OpenWeatherMap API

### Инструменты Яндекс.Трекер
- `tracker_getIssues` - получение списка задач
- `tracker_createIssue` - создание новой задачи
- `tracker_getIssue` - получение информации о задаче

## Настройка

### Переменные окружения

#### OpenWeatherMap API (для погоды)

```bash
export OPENWEATHER_API_KEY="ваш_api_key"
```

**Как получить API ключ:**
1. Зарегистрируйтесь на https://openweathermap.org/
2. Перейдите в раздел API keys
3. Скопируйте ваш API key
4. Бесплатный план: 1000 запросов/день, этого достаточно для тестирования

#### Яндекс.Трекер API (для задач)

```bash
export YANDEX_TRACKER_ORG_ID="ваш_org_id"
export YANDEX_TRACKER_TOKEN="ваш_oauth_token"
```

**Как получить токен и org_id:**
1. **OAuth токен**:
   - Перейдите на https://oauth.yandex.ru/
   - Создайте новое приложение или используйте существующее
   - Получите OAuth токен с правами на Яндекс.Трекер

2. **Organization ID**:
   - Откройте Яндекс.Трекер в браузере
   - ID организации можно найти в URL: `https://tracker.yandex.ru/admin/orgs/{ORG_ID}/`
   - Или через API: `https://api.tracker.yandex.net/v2/myself`

## Запуск

### Сборка

```bash
./gradlew :server:build
```

### Запуск сервера

```bash
./gradlew :server:run
```

Или через jar:

```bash
java -jar server/build/libs/mcp-server.jar
```

Сервер будет доступен на `http://0.0.0.0:8081`

## Примеры использования

### Получение списка tools

```bash
curl -X POST http://localhost:8081/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "1",
    "method": "tools/list",
    "params": {}
  }'
```

### Получение задач из очереди

```bash
curl -X POST http://localhost:8081/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "2",
    "method": "tools/call",
    "params": {
      "name": "tracker_getIssues",
      "arguments": {
        "queue": "TEST",
        "limit": 5
      }
    }
  }'
```

### Создание задачи

```bash
curl -X POST http://localhost:8081/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "3",
    "method": "tools/call",
    "params": {
      "name": "tracker_createIssue",
      "arguments": {
        "queue": "TEST",
        "summary": "Новая задача из MCP",
        "description": "Описание задачи",
        "type": "task",
        "priority": "normal"
      }
    }
  }'
```

### Получение информации о задаче

```bash
curl -X POST http://localhost:8081/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "4",
    "method": "tools/call",
    "params": {
      "name": "tracker_getIssue",
      "arguments": {
        "issueKey": "TEST-123"
      }
    }
  }'
```

## Архитектура

- **MCP Kotlin SDK** - официальный SDK для Model Context Protocol
- **Ktor Server** - HTTP сервер для JSON-RPC
- **Ktor Client** - HTTP клиент для внешних API
- **Яндекс.Трекер API** - интеграция с системой управления задачами

## Документация

- [MCP Specification](https://spec.modelcontextprotocol.io/)
- [Kotlin SDK](https://github.com/modelcontextprotocol/kotlin-sdk)
- [Яндекс.Трекер API](https://cloud.yandex.ru/docs/tracker/concepts/)