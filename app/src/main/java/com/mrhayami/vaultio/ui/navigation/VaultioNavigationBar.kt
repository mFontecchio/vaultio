package com.mrhayami.vaultio.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.mrhayami.vaultio.ui.theme.VaultioTheme

@Composable
fun VaultioNavigationBar(
    currentDestination: NavDestination?,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Screen.Collection,
        Screen.Wishlist,
        Screen.Stats,
        Screen.SetDownloads,
        Screen.Settings
    )

    NavigationBar(modifier = modifier) {
        items.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            NavigationBarItem(
                icon = { 
                    screen.icon?.let { 
                        Icon(imageVector = it, contentDescription = null) 
                    } 
                },
                label = { Text(screen.title) },
                selected = selected,
                onClick = { onNavigate(screen) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VaultioNavigationBarPreview() {
    VaultioTheme {
        VaultioNavigationBar(
            currentDestination = NavDestination("").apply { route = Screen.Collection.route },
            onNavigate = {}
        )
    }
}
