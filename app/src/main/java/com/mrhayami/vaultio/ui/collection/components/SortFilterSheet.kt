package com.mrhayami.vaultio.ui.collection.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mrhayami.vaultio.data.PricingUtils
import com.mrhayami.vaultio.ui.collection.CollectionEvent
import com.mrhayami.vaultio.ui.collection.FilterSettings
import com.mrhayami.vaultio.ui.collection.SortDirection
import com.mrhayami.vaultio.ui.collection.SortMode
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SortFilterSheet(
    sortMode: SortMode,
    sortDirection: SortDirection,
    filterSettings: FilterSettings,
    availableRarities: List<String>,
    availableCategories: List<String>,
    availableTypes: List<String>,
    onEvent: (CollectionEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sort & Filter", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { onEvent(CollectionEvent.OnClearFilters) }) { Text("Clear All") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Sort By", style = MaterialTheme.typography.labelLarge)
        FlowRow(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SortMode.entries.forEach { mode ->
                FilterChip(
                    selected = sortMode == mode,
                    onClick = { onEvent(CollectionEvent.OnSortModeChange(mode)) },
                    label = {
                        Text(
                            mode.name.replace("_", " ").lowercase(Locale.US)
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() })
                    },
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Direction", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.width(16.dp))
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = sortDirection == SortDirection.ASCENDING,
                    onClick = { onEvent(CollectionEvent.OnSortDirectionChange(SortDirection.ASCENDING)) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Asc") }
                SegmentedButton(
                    selected = sortDirection == SortDirection.DESCENDING,
                    onClick = { onEvent(CollectionEvent.OnSortDirectionChange(SortDirection.DESCENDING)) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("Desc") }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        if (availableRarities.isNotEmpty()) {
            Text("Rarity", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableRarities.forEach { rarity ->
                    FilterChip(
                        selected = filterSettings.rarities.contains(rarity),
                        onClick = { onEvent(CollectionEvent.OnToggleRarityFilter(rarity)) },
                        label = { Text(rarity) },
                    )
                }
            }
        }

        if (availableCategories.isNotEmpty()) {
            Text("Category", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableCategories.forEach { category ->
                    FilterChip(
                        selected = filterSettings.categories.contains(category),
                        onClick = { onEvent(CollectionEvent.OnToggleCategoryFilter(category)) },
                        label = { Text(category) },
                    )
                }
            }
        }

        if (availableTypes.isNotEmpty()) {
            Text("Type", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableTypes.forEach { type ->
                    FilterChip(
                        selected = filterSettings.types.contains(type),
                        onClick = { onEvent(CollectionEvent.OnToggleTypeFilter(type)) },
                        label = { Text(type) },
                    )
                }
            }
        }

        Text("Condition", style = MaterialTheme.typography.labelLarge)
        FlowRow(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val conditions = remember {
                listOf(
                    PricingUtils.CONDITION_NM,
                    PricingUtils.CONDITION_LP,
                    PricingUtils.CONDITION_MP,
                    PricingUtils.CONDITION_HP,
                    PricingUtils.CONDITION_DMG
                )
            }
            conditions.forEach { cond ->
                FilterChip(
                    selected = filterSettings.conditions.contains(cond),
                    onClick = { onEvent(CollectionEvent.OnToggleConditionFilter(cond)) },
                    label = { Text(cond) },
                )
            }
        }

        Text("Finish", style = MaterialTheme.typography.labelLarge)
        FlowRow(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val finishes =
                remember { listOf("Non Holo", "Holo", "Reverse Holo", "Textured", "Gold") }
            finishes.forEach { finish ->
                FilterChip(
                    selected = filterSettings.finishes.contains(finish),
                    onClick = { onEvent(CollectionEvent.OnToggleFinishFilter(finish)) },
                    label = { Text(finish) },
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
