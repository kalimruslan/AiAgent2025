#!/bin/bash
# Скрипт для запуска MCP сервера на удалённом VDS через SSH
# Использует SSH для выполнения команд на сервере

set -e

# Конфигурация
SSH_HOST="kalimruslan-rt.ru"
SSH_USER="your_username"  # Замените на ваш SSH username
PROJECT_PATH="/path/to/project/on/server"  # Путь к проекту на сервере

# Запускаем stdio сервер через SSH
# SSH будет прокидывать stdin/stdout между локальной машиной и удалённым сервером
ssh "$SSH_USER@$SSH_HOST" "cd $PROJECT_PATH && ./gradlew :server:runStdio --quiet"