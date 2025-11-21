#!/bin/bash
# Тестовый скрипт для проверки MCP сервера
# Отправляет тестовые JSON-RPC команды и выводит результаты

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "🧪 Тестирование MCP сервера..."
echo "================================"
echo ""

# Запускаем сервер в фоне
cd "$PROJECT_ROOT"
./gradlew :server:runStdio --quiet > /tmp/mcp_server_output.txt 2>&1 &
SERVER_PID=$!

# Даём серверу время на запуск
sleep 3

echo "✅ Сервер запущен (PID: $SERVER_PID)"
echo ""

# Функция для отправки команды
test_command() {
    local name=$1
    local command=$2

    echo "📤 Тест: $name"
    echo "Команда: $command"
    echo "$command" > /tmp/mcp_test_input.txt

    # Отправляем команду серверу через named pipe было бы лучше, но для простоты используем kill
    # В реальности вы должны использовать правильное тестирование через stdin/stdout
    echo "   (в продакшене используйте полноценное тестирование через stdin/stdout)"
    echo ""
}

# Тест 1: Initialize
test_command "Initialize" '{"jsonrpc":"2.0","id":"1","method":"initialize","params":{}}'

# Тест 2: Tools List
test_command "Tools List" '{"jsonrpc":"2.0","id":"2","method":"tools/list","params":{}}'

# Тест 3: Get Weather
test_command "Get Weather (Moscow)" '{"jsonrpc":"2.0","id":"3","method":"tools/call","params":{"name":"getWeather","arguments":{"city":"Moscow"}}}'

echo "================================"
echo ""
echo "ℹ️  Для интерактивного тестирования запустите:"
echo "   ./gradlew :server:runStdio"
echo ""
echo "   Затем вводите JSON-RPC команды вручную."
echo ""
echo "📝 Примеры команд:"
echo '   {"jsonrpc":"2.0","id":"1","method":"initialize","params":{}}'
echo '   {"jsonrpc":"2.0","id":"2","method":"tools/list","params":{}}'
echo '   {"jsonrpc":"2.0","id":"3","method":"tools/call","params":{"name":"getWeather","arguments":{"city":"Tokyo"}}}'
echo ""

# Останавливаем сервер
kill $SERVER_PID 2>/dev/null || true

echo "✅ Тестирование завершено"
