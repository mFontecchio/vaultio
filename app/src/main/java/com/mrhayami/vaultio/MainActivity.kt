package com.mrhayami.vaultio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.mrhayami.vaultio.ui.grading.GradingScreen
import com.mrhayami.vaultio.ui.grading.GradingViewModel
import com.mrhayami.vaultio.ui.navigation.Screen
import com.mrhayami.vaultio.ui.navigation.VaultioNavigationBar
import com.mrhayami.vaultio.ui.scanner.ScannerScreen
import com.mrhayami.vaultio.ui.screens.SetDownloadsScreen
import com.mrhayami.vaultio.ui.screens.SetDownloadsViewModel
import com.mrhayami.vaultio.ui.settings.SettingsScreen
import com.mrhayami.vaultio.ui.stats.StatsScreen
import com.mrhayami.vaultio.ui.stats.StatsViewModel
import com.mrhayami.vaultio.ui.theme.VaultioTheme
import com.mrhayami.vaultio.ui.walkthrough.WalkthroughScreen
import com.mrhayami.vaultio.ui.wishlist.WishlistScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as VaultioApplication
        val repository = app.repository
        val gradingRepository = app.gradingRepository
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
                    Screen.Stats,
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
                                    onNavigateToCardDetail = { id ->
                                        navController.navigate(
                                            Screen.CardDetail.route.replace(
                                                "{userCardId}",
                                                id.toString()
                                            )
                                        )
                                    },
                                    onNavigateToWishlist = { navController.navigate(Screen.Wishlist.route) }
                                ) 
                            }
                            composable(Screen.Wishlist.route) {
                                WishlistScreen(
                                    repository = repository,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable(Screen.Stats.route) {
                                val viewModel: StatsViewModel = viewModel(
                                    factory = remember(repository) {
                                        object : androidx.lifecycle.ViewModelProvider.Factory {
                                            override fun <T : androidx.lifecycle.ViewModel> create(
                                                modelClass: Class<T>
                                            ): T {
                                                return StatsViewModel(repository) as T
                                            }
                                        }
                                    }
                                )
                                val state by viewModel.state.collectAsState()
                                StatsScreen(
                                    state = state,
                                    onEvent = viewModel::onEvent,
                                    sideEffects = viewModel.sideEffects,
                                    onNavigation = { effect ->
                                        when (effect) {
                                            com.mrhayami.vaultio.ui.stats.StatsEffect.Navigation.GoBack -> navController.popBackStack()
                                            is com.mrhayami.vaultio.ui.stats.StatsEffect.Navigation.GoToCardDetail ->
                                                navController.navigate(
                                                    Screen.CardDetail.route.replace(
                                                        "{userCardId}",
                                                        effect.userCardId.toString()
                                                    )
                                                )
                                        }
                                    }
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
                                SetDownloadsScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                ) 
                            }
                            composable(Screen.Settings.route) {
                                SettingsScreen(
                                    repository = repository,
                                    userPreferencesRepository = userPreferencesRepository,
                                    onNavigateToDownloads = { navController.navigate(Screen.SetDownloads.route) }
                                )
                            }
                            composable(
                                route = "scanner?userCardId={userCardId}",
                                arguments = listOf(navArgument("userCardId") {
                                    type = NavType.LongType; defaultValue = -1L
                                })
                            ) { backStackEntry ->
                                val userCardId =
                                    backStackEntry.arguments?.getLong("userCardId") ?: -1L
                                ScannerScreen(
                                    repository = repository,
                                    targetUserCardId = userCardId,
                                    onNavigateBack = { navController.popBackStack() },
                                    onNavigateToGrading = { id, bmp, pendingCard ->
                                        gradingRepository.activeGradingImage = bmp
                                        if (pendingCard != null) {
                                            gradingRepository.pendingCardToGrade = pendingCard
                                        }
                                        navController.navigate("grading/$id")
                                    }
                                )
                            }
                            composable(
                                route = Screen.Grading.route,
                                arguments = listOf(navArgument("userCardId") {
                                    type = NavType.LongType
                                }),
                                enterTransition = {
                                    slideIntoContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Up,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                                },
                                exitTransition = {
                                    slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Down,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                                }
                            ) { backStackEntry ->
                                val userCardId =
                                    backStackEntry.arguments?.getLong("userCardId") ?: 0L
                                val viewModel: GradingViewModel = viewModel(
                                    factory = remember(gradingRepository, repository) {
                                        object : androidx.lifecycle.ViewModelProvider.Factory {
                                            override fun <T : androidx.lifecycle.ViewModel> create(
                                                modelClass: Class<T>
                                            ): T {
                                                return GradingViewModel(
                                                    gradingRepository,
                                                    repository
                                                ) as T
                                            }
                                        }
                                    }
                                )
                                val state by viewModel.state.collectAsState()
                                GradingScreen(
                                    state = state,
                                    userCardId = userCardId,
                                    onEvent = viewModel::onEvent,
                                    sideEffects = viewModel.sideEffects,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable(
                                route = Screen.CardDetail.route,
                                arguments = listOf(navArgument("userCardId") { type = NavType.LongType }),
                                enterTransition = {
                                    slideIntoContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Left,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                                },
                                exitTransition = {
                                    fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                                },
                                popEnterTransition = {
                                    fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                                },
                                popExitTransition = {
                                    slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Right,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                                }
                            ) { backStackEntry ->
                                val userCardId = backStackEntry.arguments?.getLong("userCardId") ?: 0L
                                CardDetailScreen(
                                    repository = repository,
                                    userCardId = userCardId,
                                    onNavigateBack = { navController.popBackStack() },
                                    onNavigateToCard = { id ->
                                        navController.navigate(
                                            Screen.CardDetail.route.replace(
                                                "{userCardId}",
                                                id.toString()
                                            )
                                        ) {
                                            popUpTo(
                                                Screen.CardDetail.route.replace(
                                                    "{userCardId}",
                                                    userCardId.toString()
                                                )
                                            ) { inclusive = true }
                                        }
                                    },
                                    onNavigateToGrading = { id, bmp ->
                                        gradingRepository.activeGradingImage = bmp
                                        navController.navigate("grading/$id")
                                    },
                                    onNavigateToScannerGrading = { id ->
                                        navController.navigate("scanner?userCardId=$id")
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
