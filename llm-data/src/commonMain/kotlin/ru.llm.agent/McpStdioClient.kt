package ru.llm.agent

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.serializer
import ru.llm.agent.core.utils.Logger
import ru.llm.agent.core.utils.createLogger
import ru.llm.agent.data.mcp.McpCallToolParams
import ru.llm.agent.data.mcp.McpCallToolRequest
import ru.llm.agent.data.mcp.McpCallToolResponse
import ru.llm.agent.data.mcp.McpTool
import ru.llm.agent.data.mcp.jsonrpc.McpRequest
import ru.llm.agent.data.mcp.jsonrpc.McpToolsListResponse

/**
 * Запрос инициализации MCP сервера
 */
@Serializable
private data class McpInitializeRequest(
    val id: Int,
    val method: String,
    val params: McpInitializeParams,
    val jsonrpc: String = "2.0"
)

@Serializable
private data class McpInitializeParams(
    val protocolVersion: String,
    val clientInfo: ClientInfo,
    val capabilities: JsonObject = JsonObject(emptyMap())
)

@Serializable
private data class ClientInfo(
    val name: String,
    val version: String
)

/**
 * Клиент для работы с локальными MCP серверами через stdio.
 * Запускает процесс и общается с ним через stdin/stdout.
 */
public class McpStdioClient(
    private val command: String,
    private val args: List<String>,
    private val env: Map<String, String>?
) {
    private val logger: Logger = createLogger("McpStdioClient")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true  // Включаем сериализацию полей со значениями по умолчанию
    }
    private val mutex = Mutex()

    private var requestId = 0
    private var process: Process? = null
    private var isRunning = false
    private var isInitialized = false

    /**
     * Запустить процесс MCP сервера
     */
    public suspend fun start(): Unit = mutex.withLock {
        if (isRunning) {
            logger.warning("Process already running")
            return
        }

        try {
            process = startProcess(command, args, env)
            isRunning = true
            logger.info("Started MCP process: $command ${args.joinToString(" ")}")

            // Запускаем поток для мониторинга stderr
            Thread {
                try {
                    process?.errorStream?.bufferedReader()?.forEachLine { line ->
                        logger.warning("MCP stderr: $line")
                    }
                } catch (e: Exception) {
                    // Игнорируем ошибки чтения stderr
                }
            }.start()
        } catch (e: Exception) {
            logger.error("Failed to start process: ${e.message}")
            throw e
        }
    }

    /**
     * Инициализировать MCP сервер
     */
    private fun initialize() {
        if (isInitialized) {
            return
        }

        val initRequest = McpInitializeRequest(
            id = ++requestId,
            method = "initialize",
            params = McpInitializeParams(
                protocolVersion = "2024-11-05",
                clientInfo = ClientInfo(
                    name = "llm-agent-kmp",
                    version = "1.0.0"
                )
            )
        )

        logger.info("Initializing MCP server")

        val proc = process ?: throw IllegalStateException("Process not initialized")
        val requestJson = json.encodeToString(McpInitializeRequest.serializer(), initRequest)

        logger.info("Sending initialize request: $requestJson")

        proc.outputStream.write("$requestJson\n".toByteArray())
        proc.outputStream.flush()

        // Читаем ответ на инициализацию с таймаутом
        val reader = proc.inputStream.bufferedReader()
        var initResponse: String? = null
        var error: Exception? = null

        val readerThread = Thread {
            try {
                initResponse = reader.readLine()
                logger.info("Initialize response: $initResponse")
            } catch (e: Exception) {
                error = e
                logger.error("Error reading initialize response: ${e.message}")
            }
        }

        readerThread.start()
        readerThread.join(10000) // Ждем максимум 10 секунд

        if (readerThread.isAlive) {
            readerThread.interrupt()
            throw IllegalStateException("Timeout waiting for MCP server initialize response (10s)")
        }

        error?.let { throw it }

        if (initResponse == null) {
            throw IllegalStateException("No initialize response from MCP server")
        }

        isInitialized = true
        logger.info("MCP server initialized successfully")
    }

    /**
     * Получить список инструментов
     */
    public suspend fun listTools(): List<McpTool> = mutex.withLock {
        ensureRunning()

        // Инициализируем сервер если еще не инициализирован
        if (!isInitialized) {
            initialize()
        }

        val request = McpRequest(
            method = "tools/list",
            id = ++requestId
        )

        logger.info("Requesting tools list from stdio server")

        val response = sendRequestAndWaitResponse(request, McpToolsListResponse.serializer())
        logger.info("Received ${response.result.tools.size} tools from stdio server")

        return response.result.tools
    }

    /**
     * Вызвать инструмент
     */
    public suspend fun callTool(name: String, arguments: JsonObject): String = mutex.withLock {
        ensureRunning()

        val request = McpCallToolRequest(
            params = McpCallToolParams(
                name = name,
                arguments = arguments
            ),
            id = ++requestId
        )

        logger.info("Calling tool '$name' on stdio server")

        val response = sendRequestAndWaitResponse(request, McpCallToolResponse.serializer())
        logger.info("Tool '$name' executed successfully")

        return response.result.content.firstOrNull()?.text ?: ""
    }

    /**
     * Остановить процесс
     */
    public suspend fun stop(): Unit = mutex.withLock {
        if (!isRunning) {
            return
        }

        try {
            process?.destroy()
            process?.waitFor()
            logger.info("Stopped MCP process")
        } catch (e: Exception) {
            logger.warning("Error stopping process: ${e.message}")
            process?.destroyForcibly()
        } finally {
            process = null
            isRunning = false
        }
    }

    /**
     * Проверить, что процесс запущен
     */
    private fun ensureRunning() {
        if (!isRunning || process?.isAlive != true) {
            throw IllegalStateException("MCP process is not running")
        }
    }

    /**
     * Отправить запрос и дождаться ответа
     */
    private fun <REQ, RESP> sendRequestAndWaitResponse(
        request: REQ,
        responseSerializer: KSerializer<RESP>
    ): RESP where REQ : Any {
        val proc = process ?: throw IllegalStateException("Process not initialized")

        // Сериализуем запрос в JSON и отправляем в stdin
        val requestJson = when (request) {
            is McpRequest -> json.encodeToString(McpRequest.serializer(), request)
            is McpCallToolRequest -> json.encodeToString(McpCallToolRequest.serializer(), request)
            else -> throw IllegalArgumentException("Unsupported request type: ${request::class}")
        }

        logger.info("Sending request: $requestJson")

        proc.outputStream.write("$requestJson\n".toByteArray())
        proc.outputStream.flush()

        // Создаем отдельный поток для чтения, чтобы можно было установить таймаут
        val reader = proc.inputStream.bufferedReader()
        var responseLine: String? = null
        var error: Exception? = null

        val readerThread = Thread {
            try {
                responseLine = reader.readLine()
                logger.info("Received response: $responseLine")
            } catch (e: Exception) {
                error = e
                logger.error("Error reading response: ${e.message}")
            }
        }

        readerThread.start()
        readerThread.join(10000) // Ждем максимум 10 секунд

        if (readerThread.isAlive) {
            readerThread.interrupt()
            throw IllegalStateException("Timeout waiting for MCP server response (10s)")
        }

        error?.let { throw it }

        val response = responseLine ?: throw IllegalStateException("No response from MCP server")

        // Десериализуем ответ
        return json.decodeFromString(responseSerializer, response)
    }
}

/**
 * Platform-specific функция запуска процесса.
 * Должна быть реализована через expect/actual.
 */
internal expect fun startProcess(
    command: String,
    args: List<String>,
    env: Map<String, String>?
): Process