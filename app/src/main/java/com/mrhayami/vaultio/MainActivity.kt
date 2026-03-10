package com.mrhayami.vaultio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mrhayami.vaultio.ui.navigation.Screen
import com.mrhayami.vaultio.ui.collection.CollectionScreen
import com.mrhayami.vaultio.ui.screens.SetDownloadsScreen
import com.mrhayami.vaultio.ui.settings.SettingsScreen
import com.mrhayami.vaultio.ui.scanner.ScannerScreen
import com.mrhayami.vaultio.ui.card_detail.CardDetailScreen
import com.mrhayami.vaultio.ui.theme.VaultioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as VaultioApplication
        val repository = app.repository
        val userPreferencesRepository = app.userPreferencesRepository
        
        setContent {
            VaultioTheme {
                val navController = rememberNavController()
                val items = listOf(
                    Screen.Collection,
                    Screen.SetDownloads,
                    Screen.Settings
                )
                
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val showBottomBar = items.any { it.route == currentRoute }

                Scaffold(
                    contentWindowInsets = WindowInsets(0.dp),
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                val currentDestination = navBackStackEntry?.destination
                                items.forEach { screen ->
                                    NavigationBarItem(
                                        icon = { screen.icon?.let { Icon(it, contentDescription = null) } },
                                        label = { Text(screen.title) },
                                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                        onClick = {
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
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController,
                        startDestination = Screen.Collection.route,
                        Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Collection.route) { 
                            CollectionScreen(
                                repository = repository,
                                userPreferencesRepository = userPreferencesRepository,
                                onNavigateToScanner = { navController.navigate(Screen.Scanner.route) },
                                onNavigateToCardDetail = { id -> navController.navigate("card_detail/$id") }
                            ) 
                        }
                        composable(Screen.SetDownloads.route) { SetDownloadsScreen(repository) }
                        composable(Screen.Settings.route) { SettingsScreen(repository) }
                        composable(Screen.Scanner.route) { 
                            ScannerScreen(
                                repository = repository,
                                onNavigateBack = { navController.popBackStack() }
                            ) 
                        }
                        composable(
                            route = "card_detail/{userCardId}",
                            arguments = listOf(navArgument("userCardId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val userCardId = backStackEntry.arguments?.getLong("userCardId") ?: 0L
                            CardDetailScreen(
                                repository = repository,
                                userCardId = userCardId,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
