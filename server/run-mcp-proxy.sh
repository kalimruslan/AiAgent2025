#!/bin/bash
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
JAR_PATH="$PROJECT_ROOT/server/build/libs/mcp-server.jar"

if [ ! -f "$JAR_PATH" ]; then
    echo "JAR not found: $JAR_PATH" >&2
    echo "Run: ./gradlew :server:shadowJar" >&2
    exit 1
fi

# Запускаем JAR напрямую
exec java -jar "$JAR_PATH"