package ru.llm.agent

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ru.llm.agent.common.app.initKoinApp
import org.koin.core.context.startKoin
import ru.llm.agent.compose.presenter.ChatScreen
import ru.llm.agent.core.uikit.AgentAiTheme
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
        AgentAiTheme {
            Scaffold(
                modifier = Modifier.fillMaxSize()
            ) { contentPadding ->
                Box(
                    modifier = Modifier
                        .padding(contentPadding)
                        .fillMaxSize(),
                ) {
                    ChatScreen()
                }
            }
        }
    }
}
