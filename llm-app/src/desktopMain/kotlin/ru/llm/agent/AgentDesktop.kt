package ru.llm.agent

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ru.llm.agent.common.app.initKoinApp
import org.koin.core.context.startKoin
import ru.llm.agent.common.StartApp
import ru.llm.agent.core.utils.PlatformContext

@Suppress("ktlint:standard:function-signature")
fun main() = application {
    startKoin {
        initKoinApp(
            platformContext = PlatformContext.INSTANCE,
            isDebug = false,
        )
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "AgentAI",
    ) {
        StartApp()
    }
}
