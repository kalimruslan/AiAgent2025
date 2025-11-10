package ru.llm.agent.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import ru.llm.agent.presentation.ui.ConversationScreen
import ru.llm.agent.presentation.ui.OptionsScreen
import ru.llm.agent.core.uikit.AgentAiTheme

@Composable
fun StartApp() {
    AgentAiTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { contentPadding ->
            Box(
                modifier = Modifier
                    .padding(contentPadding)
                    .fillMaxSize(),
            ) {
                AppNavigation()
            }
        }
    }
}

/**
 * Навигационные маршруты приложения с type-safe параметрами
 */
@Serializable
object ConversationsRoute

@Serializable
data class OptionsRoute(val conversationId: String)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ConversationsRoute
    ) {
        composable<ConversationsRoute> {
            ConversationScreen(
                onNavigateToOptions = { conversationId ->
                    navController.navigate(OptionsRoute(conversationId = conversationId))
                }
            )
        }

        composable<OptionsRoute> { backStackEntry ->
            val optionsRoute: OptionsRoute = backStackEntry.toRoute()
            OptionsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                conversationId = optionsRoute.conversationId
            )
        }
    }
}
