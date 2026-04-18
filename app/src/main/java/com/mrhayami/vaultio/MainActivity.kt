package com.mrhayami.vaultio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mrhayami.vaultio.data.DarkThemeConfig
import com.mrhayami.vaultio.data.ThemeBrand
import com.mrhayami.vaultio.ui.card_detail.CardDetailScreen
import com.mrhayami.vaultio.ui.collection.CollectionScreen
import com.mrhayami.vaultio.ui.navigation.Screen
import com.mrhayami.vaultio.ui.navigation.VaultioNavigationBar
import com.mrhayami.vaultio.ui.scanner.ScannerScreen
import com.mrhayami.vaultio.ui.screens.SetDownloadsScreen
import com.mrhayami.vaultio.ui.screens.SetDownloadsViewModel
import com.mrhayami.vaultio.ui.settings.SettingsScreen
import com.mrhayami.vaultio.ui.theme.VaultioTheme
import com.mrhayami.vaultio.ui.walkthrough.WalkthroughScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as VaultioApplication
        val repository = app.repository
        val userPreferencesRepository = app.userPreferencesRepository
        
        setContent {
            val themeBrand by userPreferencesRepository.themeBrand.collectAsState(initial = ThemeBrand.DEFAULT)
            val darkThemeConfig by userPreferencesRepository.darkThemeConfig.collectAsState(initial = DarkThemeConfig.FOLLOW_SYSTEM)
            val shouldShowWalkthrough by userPreferencesRepository.shouldShowWalkthrough.collectAsState(initial = null)
            
            val useDarkTheme = when (darkThemeConfig) {
                DarkThemeConfig.FOLLOW_SYSTEM -> isSystemInDarkTheme()
                DarkThemeConfig.LIGHT -> false
                DarkThemeConfig.DARK -> true
            }

            VaultioTheme(themeBrand = themeBrand, darkTheme = useDarkTheme) {
                val navController = rememberNavController()
                
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val showBottomBar = listOf(
                    Screen.Collection,
                    Screen.SetDownloads,
                    Screen.Settings
                ).any { it.route == currentRoute }

                Scaffold(
                    contentWindowInsets = WindowInsets(0.dp),
                    bottomBar = {
                        if (showBottomBar) {
                            VaultioNavigationBar(
                                currentDestination = navBackStackEntry?.destination,
                                onNavigate = { screen ->
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    if (shouldShowWalkthrough != null) {
                        val startDestination = if (shouldShowWalkthrough == true) {
                            Screen.Walkthrough.route
                        } else {
                            Screen.Collection.route
                        }
                        
                        NavHost(
                            navController,
                            startDestination = startDestination,
                            Modifier.padding(innerPadding)
                        ) {
                            composable(Screen.Walkthrough.route) {
                                WalkthroughScreen(
                                    userPreferencesRepository = userPreferencesRepository,
                                    onFinish = {
                                        navController.navigate(Screen.Collection.route) {
                                            popUpTo(Screen.Walkthrough.route) { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable(Screen.Collection.route) { 
                                CollectionScreen(
                                    repository = repository,
                                    userPreferencesRepository = userPreferencesRepository,
                                    onNavigateToScanner = { navController.navigate(Screen.Scanner.route) },
                                    onNavigateToCardDetail = { id -> navController.navigate("card_detail/$id") }
                                ) 
                            }
                            composable(Screen.SetDownloads.route) { 
                                val viewModel: SetDownloadsViewModel = viewModel(
                                    factory = remember(repository) { 
                                        object : androidx.lifecycle.ViewModelProvider.Factory {
                                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                                return SetDownloadsViewModel(repository) as T
                                            }
                                        }
                                    }
                                )
                                SetDownloadsScreen(viewModel) 
                            }
                            composable(Screen.Settings.route) { SettingsScreen(repository, userPreferencesRepository) }
                            composable(Screen.Scanner.route) { 
                                ScannerScreen(
                                    repository = repository,
                                    onNavigateBack = { navController.popBackStack() }
                                ) 
                            }
                            composable(
                                route = "card_detail/{userCardId}",
                                arguments = listOf(navArgument("userCardId") { type = NavType.LongType }),
                                enterTransition = {
                                    slideIntoContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Left,
                                        animationSpec = tween(500, easing = FastOutSlowInEasing)
                                    ) + fadeIn(animationSpec = tween(500))
                                },
                                exitTransition = {
                                    slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Left,
                                        animationSpec = tween(500, easing = FastOutSlowInEasing)
                                    ) + fadeOut(animationSpec = tween(500))
                                },
                                popEnterTransition = {
                                    slideIntoContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Right,
                                        animationSpec = tween(500, easing = FastOutSlowInEasing)
                                    ) + fadeIn(animationSpec = tween(500))
                                },
                                popExitTransition = {
                                    slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Right,
                                        animationSpec = tween(500, easing = FastOutSlowInEasing)
                                    ) + fadeOut(animationSpec = tween(500))
                                }
                            ) { backStackEntry ->
                                val userCardId = backStackEntry.arguments?.getLong("userCardId") ?: 0L
                                CardDetailScreen(
                                    repository = repository,
                                    userCardId = userCardId,
                                    onNavigateBack = { navController.popBackStack() },
                                    onNavigateToCard = { id -> 
                                        navController.navigate("card_detail/$id") {
                                            popUpTo("card_detail/$userCardId") { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
