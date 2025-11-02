package ru.llm.agent.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.flowOf
import ru.llm.agent.compose.presenter.McpClientScreen
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

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val currentBackStackEntry = navController.currentBackStackEntry
    val conversationId = currentBackStackEntry?.savedStateHandle?.getStateFlow("conversationId", "")
        ?.collectAsStateWithLifecycle("")

    NavHost(
        navController = navController,
        startDestination = Screen.McpClient.route
    ) {
        /*composable(Screen.Conversations.route) {
            ConversationScreen(
                onNavigateToOptions = { converationId ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("conversationId", conversationId)
                    navController.navigate(Screen.Options.route)
                }
            )
        }

        composable(
            route = Screen.Options.route,
        ) {
            OptionsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                conversationId = conversationId?.value.orEmpty()
            )
        }

        composable(Screen.DiffTwoModels.route) {
            DiffTwoModelsScreen()
        }

        composable(Screen.TwoAgentsInteraction.route) {
            InteractionTwoAgentsScreen()
        }

        composable(Screen.WorkingWithTokens.route) {
            TokensScreen()
        }

        composable(Screen.McpClient.route) {
            McpClientScreen()
        }
        */

    }

}

sealed class Screen(val route: String) {
    object Conversations : Screen("conversations")
    object Options : Screen("options/{conversationId}")

    object DiffTwoModels : Screen("diff_two_models")
    object TwoAgentsInteraction : Screen("two_agents_interaction")
    // 8 день. Работа с токенами
    object WorkingWithTokens : Screen("working_with_tokens")
    object McpClient : Screen("mcp_client")
}
