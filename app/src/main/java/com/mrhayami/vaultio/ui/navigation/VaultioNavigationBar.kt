package com.mrhayami.vaultio.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    Box(modifier = modifier) {
        NavigationBar {
            NavItem(Screen.Collection, currentDestination, onNavigate)
            NavItem(Screen.Wishlist, currentDestination, onNavigate)
            // Center spacer for elevated scanner FAB
            NavigationBarItem(
                selected = currentDestination?.hierarchy?.any { it.route == Screen.Scanner.route } == true,
                onClick = { onNavigate(Screen.Scanner) },
                icon = { },
                label = { Text("Scan") },
                enabled = false
            )
            NavItem(Screen.Stats, currentDestination, onNavigate)
            NavItem(Screen.Settings, currentDestination, onNavigate)
        }

        FloatingActionButton(
            onClick = { onNavigate(Screen.Scanner) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-28).dp)
                .size(56.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Screen.Scanner.icon?.let {
                Icon(imageVector = it, contentDescription = "Scanner")
            }
        }
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
