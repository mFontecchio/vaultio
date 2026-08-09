package com.mrhayami.vaultio.ui.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.mrhayami.vaultio.ui.theme.VaultioPreview
import com.mrhayami.vaultio.ui.theme.VaultioPreviews

@Composable
fun VaultioNavigationBar(
    currentDestination: NavDestination?,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        NavItem(Screen.Collection, currentDestination, onNavigate)
        NavItem(Screen.Wishlist, currentDestination, onNavigate)
        NavItem(Screen.Stats, currentDestination, onNavigate)
        NavItem(Screen.Settings, currentDestination, onNavigate)
    }
}

@Composable
private fun RowScope.NavItem(
    screen: Screen,
    currentDestination: NavDestination?,
    onNavigate: (Screen) -> Unit
) {
    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
    NavigationBarItem(
        icon = {
            screen.icon?.let {
                Icon(imageVector = it, contentDescription = screen.title)
            }
        },
        label = { Text(screen.title) },
        selected = selected,
        onClick = { onNavigate(screen) }
    )
}

@VaultioPreviews
@Composable
private fun VaultioNavigationBarPreview() {
    VaultioPreview {
        VaultioNavigationBar(
            currentDestination = NavDestination("").apply { route = Screen.Collection.route },
            onNavigate = {}
        )
    }
}
