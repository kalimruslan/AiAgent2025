# Подключение к удалённому MCP серверу (VDS)

Если ваш MCP сервер работает на VDS (например, Beget) и доступен по HTTPS, у вас есть несколько способов подключить его к Claude Desktop.

## Важно понять

**Claude Desktop работает только через stdio транспорт**, а не через HTTP/HTTPS напрямую. Это означает, что нужен промежуточный слой для преобразования stdio в HTTP запросы.

## Архитектура

```
Claude Desktop (stdio)
    ↓
Local Proxy/SSH (stdio → HTTP)
    ↓
Your VDS Server (HTTP)
    ↓
MCP Tools (Trello, Weather, etc.)
```

## Способ 1: HTTP-to-Stdio прокси (Рекомендуется)

Этот способ создаёт локальный прокси-процесс, который преобразует stdio в HTTP.

### Настройка на VDS

1. Убедитесь, что HTTP сервер запущен на VDS:
```bash
# На VDS
./gradlew :server:run
```

Сервер должен быть доступен по адресу `https://kalimruslan-rt.ru/mcp`

2. Настройте CORS и HTTPS на вашем сервере (если нужно)

### Настройка локально

1. Обновите `local.properties`:
```properties
REMOTE_MCP_SERVER_URL=https://kalimruslan-rt.ru
```

2. Протестируйте прокси локально:
```bash
./gradlew :server:runProxy
# Введите тестовую команду:
{"jsonrpc":"2.0","id":"1","method":"initialize","params":{}}
```

3. Настройте Claude Desktop (`~/Library/Application Support/Claude/claude_desktop_config.json`):
```json
{
  "mcpServers": {
    "llm-agent-remote": {
      "command": "/Users/ruslankalimullin/AndroidStudioProjects/kmp/LlmAgent2025/server/run-mcp-proxy.sh",
      "env": {
        "REMOTE_MCP_SERVER_URL": "https://kalimruslan-rt.ru"
      }
    }
  }
}
```

### Преимущества
- ✅ Простая настройка
- ✅ Не требует SSH доступа
- ✅ Работает через обычный HTTPS

### Недостатки
- ⚠️ Дополнительный сетевой хоп
- ⚠️ Требует настройки HTTPS на VDS

## Способ 2: SSH туннель

Этот способ использует SSH для выполнения команд напрямую на VDS.

### Настройка

1. Настройте SSH доступ к вашему VDS без пароля (SSH ключи):
```bash
ssh-copy-id username@kalimruslan-rt.ru
```

2. Убедитесь, что проект развёрнут на VDS в `/path/to/project`

3. Отредактируйте `server/run-mcp-remote.sh`:
```bash
SSH_HOST="kalimruslan-rt.ru"
SSH_USER="your_username"
PROJECT_PATH="/path/to/project/on/server"
```

4. Настройте Claude Desktop:
```json
{
  "mcpServers": {
    "llm-agent-ssh": {
      "command": "/Users/ruslankalimullin/AndroidStudioProjects/kmp/LlmAgent2025/server/run-mcp-remote.sh"
    }
  }
}
```

### Преимущества
- ✅ Прямое выполнение на сервере
- ✅ Безопасное соединение
- ✅ Не требует HTTP сервера

### Недостатки
- ⚠️ Требует SSH доступ
- ⚠️ Может быть медленнее из-за SSH overhead
- ⚠️ Нужно развернуть весь проект на VDS

## Способ 3: Локальный сервер (Рекомендуется для разработки)

Запускайте MCP сервер локально на вашем компьютере.

### Настройка

1. API ключи в `local.properties`:
```properties
TRELLO_API_KEY=your_key
TRELLO_TOKEN=your_token
OPENWEATHER_API_KEY=your_key
```

2. Claude Desktop конфигурация:
```json
{
  "mcpServers": {
    "llm-agent-local": {
      "command": "/Users/ruslankalimullin/AndroidStudioProjects/kmp/LlmAgent2025/server/run-mcp-server.sh",
      "env": {
        "TRELLO_API_KEY": "your_key",
        "TRELLO_TOKEN": "your_token",
        "OPENWEATHER_API_KEY": "your_key"
      }
    }
  }
}
```

### Преимущества
- ✅ Самый быстрый
- ✅ Простая отладка
- ✅ Нет зависимости от сети

### Недостатки
- ⚠️ Требует локальный запуск Gradle
- ⚠️ Не работает, когда компьютер выключен

## Рекомендации для продакшена

Если вы хотите использовать VDS для продакшена:

### 1. Оптимизируйте HTTP сервер

Создайте standalone JAR для более быстрого запуска:

```bash
./gradlew :server:shadowJar
```

Запустите на VDS:
```bash
java -jar server/build/libs/mcp-server.jar
```

### 2. Настройте systemd service (Linux)

Создайте `/etc/systemd/system/mcp-server.service`:
```ini
[Unit]
Description=MCP Server
After=network.target

[Service]
Type=simple
User=your_user
WorkingDirectory=/path/to/project
ExecStart=/usr/bin/java -jar /path/to/project/server/build/libs/mcp-server.jar
Restart=always
Environment="TRELLO_API_KEY=your_key"
Environment="TRELLO_TOKEN=your_token"
Environment="OPENWEATHER_API_KEY=your_key"

[Install]
WantedBy=multi-user.target
```

Запустите:
```bash
sudo systemctl enable mcp-server
sudo systemctl start mcp-server
```

### 3. Настройте nginx как reverse proxy

```nginx
server {
    listen 443 ssl;
    server_name kalimruslan-rt.ru;

    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    location /mcp {
        proxy_pass http://localhost:8081/mcp;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 4. Добавьте CORS headers (если нужно)

В `KtorServer.kt`:
```kotlin
install(CORS) {
    anyHost()
    allowHeader(HttpHeaders.ContentType)
}
```

## Troubleshooting

### Прокси не подключается к удалённому серверу

1. Проверьте, что сервер доступен:
```bash
curl https://kalimruslan-rt.ru/mcp -X POST \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":"1","method":"initialize","params":{}}'
```

2. Проверьте логи прокси в Developer Tools Claude Desktop

3. Убедитесь, что URL в `local.properties` правильный

### SSH туннель не работает

1. Проверьте SSH подключение:
```bash
ssh username@kalimruslan-rt.ru
```

2. Убедитесь, что проект существует на сервере:
```bash
ssh username@kalimruslan-rt.ru "ls -la /path/to/project"
```

3. Проверьте права на выполнение:
```bash
ssh username@kalimruslan-rt.ru "cd /path/to/project && ./gradlew --version"
```

### HTTP сервер на VDS не отвечает

1. Проверьте, что сервер запущен:
```bash
ssh username@kalimruslan-rt.ru "ps aux | grep java"
```

2. Проверьте порт:
```bash
ssh username@kalimruslan-rt.ru "netstat -tulpn | grep 8081"
```

3. Проверьте firewall:
```bash
ssh username@kalimruslan-rt.ru "sudo ufw status"
```

## Безопасность

⚠️ **Важно для продакшена:**

1. **HTTPS обязателен** - не используйте HTTP для продакшена
2. **Аутентификация** - добавьте API ключи или OAuth
3. **Rate limiting** - ограничьте количество запросов
4. **Мониторинг** - настройте логирование и алерты
5. **Переменные окружения** - не храните ключи в коде

## Выбор правильного способа

| Сценарий | Рекомендация |
|----------|--------------|
| Разработка локально | Локальный сервер (Способ 3) |
| Разработка с командой | HTTP прокси (Способ 1) |
| Продакшен с VDS | HTTP прокси + systemd (Способ 1) |
| Высокая безопасность | SSH туннель (Способ 2) |
| Минимальная задержка | Локальный сервер (Способ 3) |

## Дополнительные ресурсы

- [MCP Specification](https://modelcontextprotocol.io/)
- [Ktor Documentation](https://ktor.io/)
- [Nginx Reverse Proxy](https://docs.nginx.com/nginx/admin-guide/web-server/reverse-proxy/)