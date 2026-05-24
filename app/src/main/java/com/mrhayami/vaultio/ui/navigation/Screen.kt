package com.mrhayami.vaultio.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Scanner
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Collection : Screen("collection", "Collection", Icons.Rounded.Collections)
    object Wishlist : Screen("wishlist", "Wishlist", Icons.Rounded.FavoriteBorder)
    object Stats : Screen("stats", "Statistics", Icons.Rounded.BarChart)
    object SetDownloads : Screen("set_downloads", "Downloads", Icons.Rounded.Download)
    object Settings : Screen("settings", "Settings", Icons.Rounded.Settings)
    object Scanner : Screen("scanner", "Scanner", Icons.Rounded.Scanner)
    object Walkthrough : Screen("walkthrough", "Welcome")
    object Grading : Screen("grading/{userCardId}", "AI Grading")
}
