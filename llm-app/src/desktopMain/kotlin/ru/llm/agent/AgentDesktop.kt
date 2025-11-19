package ru.llm.agent

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.koin.core.context.startKoin
import ru.llm.agent.common.StartApp
import ru.llm.agent.common.app.initKoinApp
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
        state = rememberWindowState(
            width = 1500.dp,
            height = 1100.dp,
            position = WindowPosition(Alignment.Center)
        )
    ) {
        StartApp()
    }
}
