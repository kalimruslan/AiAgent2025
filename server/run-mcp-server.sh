#!/bin/bash
# Скрипт для запуска MCP сервера в stdio режиме
# Используется для подключения к Claude Desktop

set -e

# Переходим в корневую директорию проекта
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT"

# Запускаем Gradle задачу
exec ./gradlew :server:runStdio --quiet