#!/bin/bash
# test-mcp.sh

LOG_FILE="/tmp/mcp-debug.log"

{
    echo "=== MCP Start $(date) ==="
    echo "PWD: $(pwd)"
    echo "USER: $(whoami)"
    echo "PATH: $PATH"
    echo "ENV:"
    env | sort
    echo "=== Launching actual script ==="
} >> "$LOG_FILE" 2>&1

# Запускаем ваш оригинальный скрипт
exec /Users/ruslankalimullin/AndroidStudioProjects/kmp/LlmAgent2025/server/run-mcp-proxy.sh >> "$LOG_FILE" 2>&1