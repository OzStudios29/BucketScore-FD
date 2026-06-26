package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.BucketScoreViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BucketScoreApp()
                }
            }
        }
    }
}

@Composable
fun BucketScoreApp() {
    val navController = rememberNavController()
    val viewModel: BucketScoreViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(
                onSplashFinished = {
                    if (viewModel.currentUser != null) {
                        navController.navigate("dashboard") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        navController.navigate("auth") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("auth") {
            AuthScreen(
                viewModel = viewModel,
                onAuthSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("auth") { inclusive = true }
                    }
                },
                onNavigateToPublicScorecard = { matchId ->
                    navController.navigate("public_scorecard/$matchId")
                }
            )
        }

        composable("dashboard") {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToCreateTeam = { navController.navigate("create_team") },
                onNavigateToCreateMatch = { navController.navigate("create_match") },
                onNavigateToLineup = { matchId -> navController.navigate("starting_lineup/$matchId") },
                onNavigateToScoring = { matchId -> navController.navigate("live_scoring/$matchId") },
                onNavigateToSummary = { matchId -> navController.navigate("match_summary/$matchId") },
                onLogout = {
                    navController.navigate("auth") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            )
        }

        composable("create_team") {
            CreateTeamScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("create_match") {
            CreateMatchScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLineupSetup = { matchId ->
                    navController.navigate("starting_lineup/$matchId") {
                        popUpTo("create_match") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "starting_lineup/{matchId}",
            arguments = listOf(navArgument("matchId") { type = NavType.LongType })
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getLong("matchId") ?: 0L
            StartingLineupScreen(
                matchId = matchId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToScoring = { id ->
                    navController.navigate("live_scoring/$id") {
                        popUpTo("starting_lineup/$id") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "live_scoring/{matchId}",
            arguments = listOf(navArgument("matchId") { type = NavType.LongType })
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getLong("matchId") ?: 0L
            LiveScoringScreen(
                matchId = matchId,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                },
                onNavigateToSummary = { id ->
                    navController.navigate("match_summary/$id") {
                        popUpTo("live_scoring/$id") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "match_summary/{matchId}",
            arguments = listOf(navArgument("matchId") { type = NavType.LongType })
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getLong("matchId") ?: 0L
            MatchSummaryScreen(
                matchId = matchId,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "public_scorecard/{matchId}",
            arguments = listOf(navArgument("matchId") { type = NavType.LongType }),
            deepLinks = listOf(
                navDeepLink { uriPattern = "bucketscore://scorecard/{matchId}" },
                navDeepLink { uriPattern = "https://bucketscore.web.app/scorecard/{matchId}" }
            )
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getLong("matchId") ?: 0L
            PublicScorecardScreen(
                matchId = matchId,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "match/{matchCode}",
            arguments = listOf(navArgument("matchCode") { type = NavType.StringType }),
            deepLinks = listOf(
                navDeepLink { uriPattern = "https://bucketscore.app/match/{matchCode}" }
            )
        ) { backStackEntry ->
            val matchCode = backStackEntry.arguments?.getString("matchCode") ?: ""
            MatchCodeRedirector(
                matchCode = matchCode,
                viewModel = viewModel,
                onRedirect = { id ->
                    navController.navigate("public_scorecard/$id") {
                        popUpTo("match/{matchCode}") { inclusive = true }
                    }
                },
                onFail = {
                    navController.navigate("auth") {
                        popUpTo("match/{matchCode}") { inclusive = true }
                    }
                }
            )
        }
    }
}
