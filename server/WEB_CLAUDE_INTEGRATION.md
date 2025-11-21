# Интеграция MCP сервера с веб-версией Claude

## Важно

**Веб-версия Claude (claude.ai) не поддерживает MCP серверы напрямую.** MCP работает только в Claude Desktop через stdio/SSH транспорт.

Однако есть несколько способов использовать ваши инструменты с веб-версией Claude.

## Решение 1: Промпт с контекстом (Рекомендуется для простых задач)

Вызывайте HTTP API вашего MCP сервера вручную и передавайте результаты в веб-версию Claude.

### Шаг 1: Запустите HTTP сервер

```bash
# Локально
./gradlew :server:run

# Или на VDS
# Сервер доступен на https://kalimruslan-rt.ru/mcp
```

### Шаг 2: Вызовите инструмент через API

**Пример: Получить погоду**

```bash
curl -X POST https://kalimruslan-rt.ru/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "1",
    "method": "tools/call",
    "params": {
      "name": "getWeather",
      "arguments": {"city": "Moscow"}
    }
  }'
```

**Ответ:**
```json
{
  "jsonrpc": "2.0",
  "id": "1",
  "result": {
    "content": [{
      "type": "text",
      "text": "🌤️ Погода в городе Moscow:\n\n🌡️ Температура: -5°C\n..."
    }]
  }
}
```

### Шаг 3: Передайте результат в Claude (веб)

В чате с Claude напишите:

```
Вот погода в Москве:

🌤️ Погода в городе Moscow:
🌡️ Температура: -5°C
🌡️ Ощущается как: -8°C
☁️ Условия: облачно
💧 Влажность: 75%

Что мне надеть на прогулку?
```

### Автоматизация с помощью скрипта

Создайте helper скрипт для вызова инструментов:

```bash
#!/bin/bash
# Файл: query-tool.sh

TOOL_NAME=$1
CITY=$2
SERVER_URL="https://kalimruslan-rt.ru/mcp"

RESULT=$(curl -s -X POST "$SERVER_URL" \
  -H "Content-Type: application/json" \
  -d "{
    \"jsonrpc\": \"2.0\",
    \"id\": \"1\",
    \"method\": \"tools/call\",
    \"params\": {
      \"name\": \"$TOOL_NAME\",
      \"arguments\": {\"city\": \"$CITY\"}
    }
  }" | jq -r '.result.content[0].text')

echo "$RESULT"

# Копируем в буфер обмена (macOS)
echo "$RESULT" | pbcopy
echo "Результат скопирован в буфер обмена!"
```

**Использование:**
```bash
chmod +x query-tool.sh
./query-tool.sh getWeather Moscow
# Результат автоматически в буфере обмена - вставьте в чат Claude
```

## Решение 2: Custom ChatGPT Action (если используете ChatGPT)

Для ChatGPT можно создать Custom GPT с Actions, которые вызывают ваш MCP сервер через HTTP.

Это не применимо к Claude, но упоминаем для полноты картины.

## Решение 3: Создать веб-интерфейс с интеграцией Claude API

Создайте собственный веб-интерфейс, который:
1. Вызывает ваши MCP инструменты
2. Отправляет результаты в Claude API
3. Показывает ответы пользователю

### Архитектура

```
Пользователь
    ↓
Ваш веб-интерфейс
    ↓
    ├─→ MCP Server (инструменты)
    │
    └─→ Claude API (генерация ответов)
```

### Пример реализации (псевдокод)

```javascript
// Frontend
async function askClaude(userMessage) {
  // 1. Отправляем на ваш бэкенд
  const response = await fetch('/api/chat', {
    method: 'POST',
    body: JSON.stringify({ message: userMessage })
  });

  return response.json();
}

// Backend (Node.js/Kotlin/Python)
app.post('/api/chat', async (req, res) => {
  const userMessage = req.body.message;

  // 2. Определяем, нужен ли инструмент
  if (needsWeather(userMessage)) {
    // Вызываем MCP инструмент
    const weather = await callMcpTool('getWeather', { city: 'Moscow' });

    // 3. Формируем промпт с контекстом
    const enrichedPrompt = `
      Пользователь спросил: ${userMessage}

      Вот актуальная погода:
      ${weather}

      Ответь пользователю на основе этих данных.
    `;

    // 4. Вызываем Claude API
    const claudeResponse = await callClaudeAPI(enrichedPrompt);

    return res.json({ response: claudeResponse });
  }

  // Обычный запрос без инструментов
  const claudeResponse = await callClaudeAPI(userMessage);
  return res.json({ response: claudeResponse });
});
```

## Решение 4: Browser Extension (Расширение для браузера)

Создайте расширение для Chrome/Firefox, которое:
- Перехватывает сообщения в веб-версии Claude
- Вызывает ваш MCP сервер
- Вставляет результаты в чат

### Схема работы

```
1. Пользователь пишет: "Погода в Москве?"
2. Extension перехватывает
3. Extension вызывает https://kalimruslan-rt.ru/mcp
4. Extension получает результат
5. Extension вставляет в чат: "Вот погода: ..."
6. Пользователь отправляет обогащённое сообщение Claude
```

### Пример манифеста (manifest.json)

```json
{
  "manifest_version": 3,
  "name": "Claude MCP Helper",
  "version": "1.0",
  "permissions": ["activeTab", "storage"],
  "host_permissions": ["https://claude.ai/*", "https://kalimruslan-rt.ru/*"],
  "content_scripts": [{
    "matches": ["https://claude.ai/*"],
    "js": ["content.js"]
  }]
}
```

### Пример content script (упрощённо)

```javascript
// content.js
// Перехватываем отправку сообщений
document.addEventListener('submit', async (e) => {
  const textarea = document.querySelector('textarea');
  const message = textarea.value;

  // Проверяем, нужен ли инструмент
  if (message.includes('погода')) {
    e.preventDefault();

    // Вызываем MCP
    const weather = await fetch('https://kalimruslan-rt.ru/mcp', {
      method: 'POST',
      body: JSON.stringify({
        jsonrpc: '2.0',
        id: '1',
        method: 'tools/call',
        params: {
          name: 'getWeather',
          arguments: { city: extractCity(message) }
        }
      })
    }).then(r => r.json());

    // Обогащаем сообщение
    textarea.value = `${message}\n\nКонтекст:\n${weather.result.content[0].text}`;

    // Отправляем
    e.target.submit();
  }
});
```

## Решение 5: Используйте Claude Desktop вместо веб-версии

**Самое простое и надёжное решение** - используйте Claude Desktop с полной поддержкой MCP.

### Почему Claude Desktop лучше для MCP:

✅ **Нативная поддержка MCP** - никаких костылей
✅ **Автоматический вызов инструментов** - Claude сам решает, когда их использовать
✅ **Безопасность** - stdio транспорт, никаких публичных API
✅ **Скорость** - локальное выполнение инструментов
✅ **Простота** - настроил один раз и работает

### Сравнение веб vs Desktop

| Функция | Веб-версия | Claude Desktop |
|---------|------------|----------------|
| MCP инструменты | ❌ Нет | ✅ Да |
| Автовызов tools | ❌ Нет | ✅ Да |
| Настройка | ⚠️ Костыли | ✅ Простая |
| Безопасность | ⚠️ HTTP API | ✅ Stdio/SSH |
| Скорость tools | ⚠️ Сеть | ✅ Локально |

## Рекомендации

### Для разработки и личного использования:
👉 **Используйте Claude Desktop** с MCP серверами ([QUICKSTART.md](./QUICKSTART.md))

### Для конечных пользователей без Claude Desktop:
👉 **Создайте веб-интерфейс** (Решение 3) с интеграцией Claude API

### Для быстрых запросов:
👉 **Используйте скрипт** (Решение 1) для вызова инструментов и копирования в буфер

## API эндпоинты вашего MCP сервера

Ваш сервер на `https://kalimruslan-rt.ru` предоставляет следующие методы:

### 1. Initialize
```bash
curl -X POST https://kalimruslan-rt.ru/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":"1","method":"initialize","params":{}}'
```

### 2. List Tools
```bash
curl -X POST https://kalimruslan-rt.ru/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":"2","method":"tools/list","params":{}}'
```

### 3. Call Tool
```bash
# Погода
curl -X POST https://kalimruslan-rt.ru/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc":"2.0",
    "id":"3",
    "method":"tools/call",
    "params":{
      "name":"getWeather",
      "arguments":{"city":"Tokyo"}
    }
  }'

# Trello - получить карточки
curl -X POST https://kalimruslan-rt.ru/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc":"2.0",
    "id":"4",
    "method":"tools/call",
    "params":{
      "name":"trello_getCards",
      "arguments":{"boardId":"YOUR_BOARD_ID","filter":"open"}
    }
  }'
```

## Утилиты для упрощения работы

### Python wrapper

```python
import requests
import json

class McpClient:
    def __init__(self, server_url):
        self.server_url = server_url
        self.request_id = 0

    def call_tool(self, tool_name, arguments):
        self.request_id += 1
        response = requests.post(self.server_url, json={
            "jsonrpc": "2.0",
            "id": str(self.request_id),
            "method": "tools/call",
            "params": {
                "name": tool_name,
                "arguments": arguments
            }
        })
        result = response.json()
        return result['result']['content'][0]['text']

# Использование
mcp = McpClient('https://kalimruslan-rt.ru/mcp')
weather = mcp.call_tool('getWeather', {'city': 'Moscow'})
print(weather)

# Теперь вставьте weather в веб-версию Claude
```

### JavaScript wrapper

```javascript
class McpClient {
  constructor(serverUrl) {
    this.serverUrl = serverUrl;
    this.requestId = 0;
  }

  async callTool(toolName, args) {
    this.requestId++;
    const response = await fetch(this.serverUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        jsonrpc: '2.0',
        id: String(this.requestId),
        method: 'tools/call',
        params: {
          name: toolName,
          arguments: args
        }
      })
    });

    const result = await response.json();
    return result.result.content[0].text;
  }
}

// Использование
const mcp = new McpClient('https://kalimruslan-rt.ru/mcp');
const weather = await mcp.callTool('getWeather', { city: 'Moscow' });
console.log(weather);
```

## Заключение

Хотя веб-версия Claude не поддерживает MCP напрямую, вы можете:

1. ✅ Использовать **Claude Desktop** для полной MCP интеграции (рекомендуется)
2. ✅ Вызывать HTTP API вручную и копировать результаты
3. ✅ Создать собственный веб-интерфейс с Claude API
4. ✅ Написать browser extension для автоматизации

**Для вашего случая с VDS на kalimruslan-rt.ru:**
- HTTP сервер уже доступен по HTTPS
- Можно сразу вызывать через curl/fetch
- Рекомендуем попробовать Claude Desktop для полноценной работы