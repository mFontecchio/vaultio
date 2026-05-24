package com.mrhayami.vaultio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mrhayami.vaultio.data.PricingUtils
import com.mrhayami.vaultio.data.local.FolderEntity
import com.mrhayami.vaultio.data.remote.TcgDexCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MetadataModal(
    card: TcgDexCard,
    modifier: Modifier = Modifier,
    folders: List<FolderEntity> = emptyList(),
    initialCondition: String = PricingUtils.CONDITION_NM,
    onConfirm: (Int, String, String, String, List<Long>) -> Unit,
    onWishlistConfirm: ((Int, String, String, String) -> Unit)? = null,
    onBack: () -> Unit
) {
    var quantity by remember { mutableIntStateOf(1) }
    var condition by remember { mutableStateOf(initialCondition) }
    var printing by remember { mutableStateOf(PricingUtils.PRINTING_UNLIMITED) }
    var finish by remember { mutableStateOf(PricingUtils.FINISH_NORMAL) }
    val selectedFolderIds = remember { mutableStateListOf<Long>() }

    // Use LocalInspectionMode to ensure visibility in Previews and avoid potential render issues with animations
    val isInspectionMode = LocalInspectionMode.current
    var isVisible by remember { mutableStateOf(isInspectionMode) }
    LaunchedEffect(Unit) {
        if (!isInspectionMode) isVisible = true
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { -20 }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Text(
                    "Add ${card.name}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(400, 100)) + scaleIn(tween(400, 100), initialScale = 0.9f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Card(
                modifier = Modifier.padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                AsyncImage(
                    model = card.image?.let { "$it/high.webp" },
                    contentDescription = null,
                    modifier = Modifier
                        .height(280.dp)
                        .padding(8.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(500, 200, FastOutSlowInEasing)) +
                    slideInVertically(tween(500, 200, FastOutSlowInEasing)) { 40 }
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Quantity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .padding(horizontal = 4.dp)
                        ) {
                            IconButton(
                                onClick = { if (quantity > 1) quantity-- },
                                modifier = Modifier.clip(CircleShape)
                            ) {
                                Icon(
                                    Icons.Rounded.Remove,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                "$quantity",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            IconButton(
                                onClick = { quantity++ },
                                modifier = Modifier.clip(CircleShape)
                            ) {
                                Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val conditions = listOf(
                        PricingUtils.CONDITION_NM,
                        PricingUtils.CONDITION_LP,
                        PricingUtils.CONDITION_MP,
                        PricingUtils.CONDITION_HP,
                        PricingUtils.CONDITION_DMG
                    )
                    DropdownSelector("Condition", condition, conditions) { condition = it }

                    Spacer(modifier = Modifier.height(16.dp))

                    val printings = listOf(
                        PricingUtils.PRINTING_UNLIMITED,
                        PricingUtils.PRINTING_SHADOWLESS,
                        PricingUtils.PRINTING_PROMO,
                        PricingUtils.PRINTING_1ST_EDITION
                    )
                    DropdownSelector("Printing", printing, printings) { printing = it }

                    Spacer(modifier = Modifier.height(16.dp))

                    val finishes = listOf(
                        PricingUtils.FINISH_NORMAL,
                        PricingUtils.FINISH_HOLOFOIL,
                        PricingUtils.FINISH_REVERSE_HOLO,
                        PricingUtils.FINISH_TEXTURED,
                        PricingUtils.FINISH_GOLD
                    )
                    DropdownSelector("Finish", finish, finishes) { finish = it }

                    if (folders.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Add to Folders",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            folders.forEach { folder ->
                                key(folder.id) {
                                    val isSelected = selectedFolderIds.contains(folder.id)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            if (isSelected) selectedFolderIds.remove(folder.id)
                                            else selectedFolderIds.add(folder.id)
                                        },
                                        label = { Text(folder.name) },
                                        shape = CircleShape
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(500, 350)) + slideInVertically(tween(500, 350)) { 20 }
        ) {
            Column {
                Button(
                    onClick = {
                        onConfirm(
                            quantity,
                            condition,
                            printing,
                            finish,
                            selectedFolderIds.toList()
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        "Add to Collection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (onWishlistConfirm != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            onWishlistConfirm(
                                quantity,
                                condition,
                                printing,
                                finish
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = CircleShape
                    ) {
                        Text(
                            "Add to Wishlist",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(
    label: String,
    value: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun MetadataModalPreview() {
    MaterialTheme {
        MetadataModal(
            card = TcgDexCard(
                id = "swsh1-1",
                localId = "1",
                name = "Charizard",
                image = "https://assets.tcgdex.net/en/swsh/swsh1/1",
                rarity = "Rare Holo",
                category = "Pokemon",
                dexId = listOf(6),
                types = listOf("Fire")
            ),
            folders = listOf(
                FolderEntity(id = 1L, name = "Favorites", icon = "star"),
                FolderEntity(id = 2L, name = "Trade", icon = "swap")
            ),
            onConfirm = { _, _, _, _, _ -> },
            onBack = {}
        )
    }
}
