import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import ru.llm.agent.di.ragModule
import ru.llm.agent.di.networkModule
import ru.llm.agent.di.ragUseCasesModule
import ru.llm.agent.usecase.rag.IndexTextUseCase
import ru.llm.agent.usecase.rag.SearchRagDocumentsUseCase
import ru.llm.agent.usecase.rag.GetRagIndexStatsUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

/**
 * Простой тест для проверки работы RAG системы
 *
 * Перед запуском убедитесь, что:
 * 1. Ollama установлена и запущена (ollama serve)
 * 2. Загружена модель nomic-embed-text (ollama pull nomic-embed-text)
 *
 * Запуск:
 * kotlinc -cp <classpath> RagTest.kt -include-runtime -d RagTest.jar && kotlin -cp <classpath>:RagTest.jar RagTestKt
 */
class RagTestRunner : KoinComponent {
    private val indexTextUseCase: IndexTextUseCase by inject()
    private val searchUseCase: SearchRagDocumentsUseCase by inject()
    private val statsUseCase: GetRagIndexStatsUseCase by inject()

    suspend fun runTest() {
        println("🚀 Начинаем тест RAG системы...")
        println()

        // 1. Читаем тестовый файл
        println("📖 Читаем файл test-knowledge-base.txt...")
        val knowledgeBase = File("test-knowledge-base.txt").readText()
        println("✅ Файл прочитан. Размер: ${knowledgeBase.length} символов")
        println()

        // 2. Индексируем текст
        println("🔨 Индексируем текст...")
        val indexResult = indexTextUseCase(
            text = knowledgeBase,
            sourceId = "kmp-guide"
        )
        println("✅ Индексация завершена!")
        println("   Создано чанков: ${indexResult.chunksIndexed}")
        println("   Источник: ${indexResult.sourceId}")
        println()

        // 3. Проверяем статистику
        val stats = statsUseCase()
        println("📊 Статистика индекса:")
        println("   Всего документов: $stats")
        println()

        // 4. Тестовые запросы
        val queries = listOf(
            "Что такое Kotlin Multiplatform?",
            "Как работает expect/actual механизм?",
            "Какие библиотеки используются для DI в KMP?",
            "Расскажи про Compose Multiplatform",
            "Как работать с базами данных в KMP?"
        )

        println("🔍 Тестируем поиск...")
        println()

        queries.forEach { query ->
            println("Вопрос: $query")
            println("─".repeat(80))

            val results = searchUseCase(
                query = query,
                topK = 3,
                threshold = 0.3
            )

            if (results.isEmpty()) {
                println("❌ Ничего не найдено")
            } else {
                results.forEachIndexed { index, doc ->
                    println("Результат ${index + 1} (схожесть: ${String.format("%.3f", doc.similarity)}):")
                    println(doc.text.take(200) + "...")
                    println()
                }
            }
            println("═".repeat(80))
            println()
        }

        println("✨ Тест завершён!")
    }
}

fun main() = runBlocking {
    // Инициализируем Koin
    println("⚙️  Инициализация Koin...")

    // ВАЖНО: Здесь нужно предоставить реальные значения для токенов
    // В реальном приложении они берутся из local.properties
    val koinApp = startKoin {
        modules(
            // Здесь минимальный набор модулей для теста
            ragModule,
            ragUseCasesModule
        )
    }

    try {
        val testRunner = RagTestRunner()
        testRunner.runTest()
    } catch (e: Exception) {
        println("❌ Ошибка: ${e.message}")
        e.printStackTrace()
    } finally {
        stopKoin()
        println("👋 Koin остановлен")
    }
}