package com.mrhayami.vaultio.ui.collection.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mrhayami.vaultio.ui.collection.CollectionEvent
import com.mrhayami.vaultio.ui.collection.GridSettings
import com.mrhayami.vaultio.ui.collection.ListSettings
import com.mrhayami.vaultio.ui.collection.PokedexSettings
import com.mrhayami.vaultio.ui.collection.ViewMode

@Composable
fun ViewSettingsSheet(
    viewMode: ViewMode,
    listSettings: ListSettings,
    gridSettings: GridSettings,
    pokedexSettings: PokedexSettings,
    onEvent: (CollectionEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .navigationBarsPadding()
    ) {
        Text("View Settings", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        when (viewMode) {
            ViewMode.LIST -> {
                SettingsToggle(
                    "Show Prices",
                    listSettings.showPrices
                ) { onEvent(CollectionEvent.OnUpdateListSettings(listSettings.copy(showPrices = it))) }
                SettingsToggle(
                    "Compact Mode",
                    listSettings.isCompact
                ) { onEvent(CollectionEvent.OnUpdateListSettings(listSettings.copy(isCompact = it))) }
            }

            ViewMode.GRID -> {
                Text(
                    "Columns: ${gridSettings.columns}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = gridSettings.columns.toFloat(),
                    onValueChange = {
                        onEvent(
                            CollectionEvent.OnUpdateGridSettings(
                                gridSettings.copy(
                                    columns = it.toInt()
                                )
                            )
                        )
                    },
                    valueRange = 2f..5f,
                    steps = 2
                )
                SettingsToggle("Show Quantity Badges", gridSettings.showBadges) {
                    onEvent(
                        CollectionEvent.OnUpdateGridSettings(gridSettings.copy(showBadges = it))
                    )
                }
            }

            ViewMode.POKEDEX -> {
                SettingsToggle("Show Uncollected Slots", pokedexSettings.showUncollected) {
                    onEvent(
                        CollectionEvent.OnUpdatePokedexSettings(pokedexSettings.copy(showUncollected = it))
                    )
                }
                SettingsToggle("Use Official Art", pokedexSettings.useOfficialArt) {
                    onEvent(
                        CollectionEvent.OnUpdatePokedexSettings(pokedexSettings.copy(useOfficialArt = it))
                    )
                }
                SettingsToggle("Use Shiny Sprites", pokedexSettings.useShinySprites) {
                    onEvent(
                        CollectionEvent.OnUpdatePokedexSettings(pokedexSettings.copy(useShinySprites = it))
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SettingsToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun Spacer(modifier: Modifier) {
    androidx.compose.foundation.layout.Spacer(modifier = modifier)
}
