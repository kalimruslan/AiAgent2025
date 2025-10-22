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
import ru.llm.agent.compose.presenter.ConversationScreen
import ru.llm.agent.compose.presenter.DiffTwoModelsScreen
import ru.llm.agent.compose.presenter.OptionsScreen
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

    NavHost(
        navController = navController,
        startDestination = Screen.DiffTwoModels.route
    ) {
        composable(Screen.Conversations.route) {
            ConversationScreen(
                onNavigateToOptions = {
                    navController.navigate(Screen.Options.route)
                }
            )
        }

        composable(Screen.Options.route) {
            OptionsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.DiffTwoModels.route) {
            DiffTwoModelsScreen()
        }
    }
}

sealed class Screen(val route: String) {
    object Conversations : Screen("conversations")
    object Options : Screen("options")

    object DiffTwoModels : Screen("diff_two_models")
}
