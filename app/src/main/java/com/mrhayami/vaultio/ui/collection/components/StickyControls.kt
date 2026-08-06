package com.mrhayami.vaultio.ui.collection.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.CatchingPokemon
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mrhayami.vaultio.data.local.FolderEntity
import com.mrhayami.vaultio.ui.collection.CollectionEvent
import com.mrhayami.vaultio.ui.collection.FilterSettings
import com.mrhayami.vaultio.ui.collection.SortMode
import com.mrhayami.vaultio.ui.collection.ViewMode
import com.mrhayami.vaultio.ui.collection.getIconFromName
import java.util.Locale

@Composable
fun StickyControls(
    viewMode: ViewMode,
    sortMode: SortMode,
    filterSettings: FilterSettings,
    folders: List<FolderEntity>,
    selectedFolderId: Long?,
    totalQuantity: Int,
    totalValue: Double,
    pokedexCollectedCount: Int,
    pokedexTotalCount: Int,
    onEvent: (CollectionEvent) -> Unit,
    onSortClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                FilterChip(
                    selected = filterSettings != FilterSettings() || sortMode != SortMode.DATE_ADDED,
                    onClick = onSortClick,
                    label = { Text("Sort & Filter") },
                    leadingIcon = {
                        Icon(
                            Icons.AutoMirrored.Rounded.Sort,
                            null,
                            Modifier.size(18.dp)
                        )
                    },
                    shape = CircleShape
                )
            }

            item {
                VerticalDivider(
                    modifier = Modifier
                        .height(24.dp)
                        .padding(horizontal = 4.dp)
                )
            }

            item {
                FilterChip(
                    selected = selectedFolderId == null,
                    onClick = { onEvent(CollectionEvent.OnFolderSelect(null)) },
                    label = { Text("All") },
                    shape = CircleShape
                )
            }
            items(folders, key = { it.id }) { folder ->
                FilterChip(
                    selected = selectedFolderId == folder.id,
                    onClick = { onEvent(CollectionEvent.OnFolderSelect(folder.id)) },
                    label = { Text(folder.name) },
                    shape = CircleShape,
                    leadingIcon = {
                        Icon(
                            getIconFromName(folder.icon),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = folder.color?.let { Color(it.toLong().toInt()) }
                                ?: MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.padding(start = 4.dp)) {
                Text(
                    if (viewMode == ViewMode.POKEDEX) "$pokedexCollectedCount / $pokedexTotalCount Collected"
                    else "$totalQuantity Cards",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (viewMode != ViewMode.POKEDEX) {
                    Text(
                        "$${String.format(Locale.US, "%.2f", totalValue)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.padding(end = 4.dp),
            ) {
                SegmentedButton(
                    selected = viewMode == ViewMode.LIST,
                    onClick = { onEvent(CollectionEvent.OnViewModeChange(ViewMode.LIST)) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) { Icon(Icons.AutoMirrored.Rounded.List, contentDescription = "List") }
                SegmentedButton(
                    selected = viewMode == ViewMode.GRID,
                    onClick = { onEvent(CollectionEvent.OnViewModeChange(ViewMode.GRID)) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) { Icon(Icons.Rounded.GridView, contentDescription = "Grid") }
                SegmentedButton(
                    selected = viewMode == ViewMode.POKEDEX,
                    onClick = { onEvent(CollectionEvent.OnViewModeChange(ViewMode.POKEDEX)) },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) {
                    Icon(Icons.Rounded.CatchingPokemon, contentDescription = "Pokedex")
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun StickyControlsPreview() {
    MaterialTheme {
        StickyControls(
            viewMode = ViewMode.GRID,
            sortMode = SortMode.DATE_ADDED,
            filterSettings = FilterSettings(),
            folders = emptyList(),
            selectedFolderId = null,
            totalQuantity = 125,
            totalValue = 1250.50,
            pokedexCollectedCount = 100,
            pokedexTotalCount = 1025,
            onEvent = {},
            onSortClick = {}
        )
    }
}
