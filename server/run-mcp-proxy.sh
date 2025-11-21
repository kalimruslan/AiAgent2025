#!/bin/bash
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
JAR_PATH="$SCRIPT_DIR/build/libs/mcp-server-proxy.jar"

# Проверяем наличие JAR
if [ ! -f "$JAR_PATH" ]; then
    echo "[PROXY SCRIPT] ERROR: JAR not found at $JAR_PATH" >&2
    echo "[PROXY SCRIPT] Run: ./gradlew :server:fatJarProxy" >&2
    exit 1
fi

# Читаем REMOTE_MCP_SERVER_URL из переменных окружения (будет задан в claude_desktop_config.json)
# Или используем значение по умолчанию
REMOTE_URL="${REMOTE_MCP_SERVER_URL:-https://kalimruslan-rt.ru}"

echo "[PROXY SCRIPT] Starting proxy..." >&2
echo "[PROXY SCRIPT] JAR: $JAR_PATH" >&2
echo "[PROXY SCRIPT] Remote URL: $REMOTE_URL" >&2

# Запускаем JAR с переменной окружения
exec env REMOTE_MCP_SERVER_URL="$REMOTE_URL" java -jar "$JAR_PATH"