package com.mrhayami.vaultio.ui.collection.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.mrhayami.vaultio.data.local.FolderEntity
import com.mrhayami.vaultio.ui.collection.getIconFromName
import com.mrhayami.vaultio.ui.theme.VaultioPreview
import com.mrhayami.vaultio.ui.theme.VaultioPreviews

@Composable
fun FolderDialog(
    folder: FolderEntity,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(folder.name) }
    var selectedIcon by remember { mutableStateOf(folder.icon ?: "folder") }

    val colorOptions = remember {
        listOf(
            Color(0xFF78C850), // Grass
            Color(0xFFF08030), // Fire
            Color(0xFF6890F0), // Water
            Color(0xFFF8D030), // Electric
            Color(0xFFF85888), // Psychic
            Color(0xFFA8A878), // Normal
            Color(0xFFE0C068), // Ground
            Color(0xFFA040A0), // Poison
            Color(0xFFC03028), // Fighting
            Color(0xFFB8A038), // Rock
            Color(0xFFA8B820), // Bug
            Color(0xFF705898), // Ghost
            Color(0xFFB8B8D0), // Steel
            Color(0xFF98D8D8), // Ice
            Color(0xFF7038F8), // Dragon
            Color(0xFF705848), // Dark
            Color(0xFFEE99AC)  // Fairy
        )
    }

    var selectedColor by remember {
        mutableStateOf(folder.color ?: colorOptions[1].toArgb().toLong().toString())
    }

    val icons = remember {
        listOf(
            "folder",
            "star",
            "favorite",
            "label",
            "history",
            "cloud",
            "auto_awesome",
            "bolt"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (folder.id == 0L) "New Folder" else "Edit Folder") },
        shape = RoundedCornerShape(28.dp),
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Folder Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Icon", style = MaterialTheme.typography.labelLarge)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(icons) { iconName ->
                        IconButton(
                            onClick = { selectedIcon = iconName },
                            modifier = Modifier
                                .size(48.dp)
                                .border(
                                    width = 2.dp,
                                    color = if (selectedIcon == iconName) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(getIconFromName(iconName), contentDescription = null)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Theme Color", style = MaterialTheme.typography.labelLarge)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(colorOptions) { color ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = 2.dp,
                                    color = if (selectedColor == color.toArgb().toLong()
                                            .toString()
                                    ) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color.toArgb().toLong().toString() }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) onConfirm(
                    name,
                    selectedIcon,
                    selectedColor
                )
            }) {
                Text(if (folder.id == 0L) "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@VaultioPreviews
@Composable
private fun FolderDialogNewPreview() {
    VaultioPreview {
        FolderDialog(
            folder = FolderEntity(id = 0L, name = ""),
            onDismiss = {},
            onConfirm = { _, _, _ -> }
        )
    }
}

@VaultioPreviews
@Composable
private fun FolderDialogEditPreview() {
    VaultioPreview {
        FolderDialog(
            folder = FolderEntity(
                id = 1L,
                name = "Favorites",
                icon = "star",
                color = Color(0xFFF08030).toArgb().toLong().toString()
            ),
            onDismiss = {},
            onConfirm = { _, _, _ -> }
        )
    }
}
