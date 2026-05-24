package com.mrhayami.vaultio.ui.collection.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mrhayami.vaultio.data.local.FolderEntity
import com.mrhayami.vaultio.ui.collection.getIconFromName

@Composable
fun ExportSelectionDialog(
    folders: List<FolderEntity>,
    onDismiss: () -> Unit,
    onConfirm: (List<Long>?) -> Unit
) {
    var selectedFolderIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var exportAll by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Collection") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Select what you want to export:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { exportAll = true }
                        .padding(vertical = 8.dp)
                ) {
                    RadioButton(
                        selected = exportAll,
                        onClick = { exportAll = true }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Entire Collection")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { exportAll = false }
                        .padding(vertical = 8.dp)
                ) {
                    RadioButton(
                        selected = !exportAll,
                        onClick = { exportAll = false }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Selected Folders Only")
                }

                AnimatedVisibility(visible = !exportAll) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .padding(start = 32.dp)
                    ) {
                        LazyColumn {
                            items(folders, key = { it.id }) { folder ->
                                val rowModifier = remember(folder.id, selectedFolderIds) {
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedFolderIds =
                                                if (selectedFolderIds.contains(folder.id)) {
                                                    selectedFolderIds - folder.id
                                                } else {
                                                    selectedFolderIds + folder.id
                                                }
                                        }
                                        .padding(vertical = 4.dp)
                                }
                                val onCheckedChange = remember(folder.id, selectedFolderIds) {
                                    { checked: Boolean ->
                                        selectedFolderIds =
                                            if (checked) selectedFolderIds + folder.id else selectedFolderIds - folder.id
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = rowModifier
                                ) {
                                    Checkbox(
                                        checked = selectedFolderIds.contains(folder.id),
                                        onCheckedChange = onCheckedChange
                                    )
                                    Icon(
                                        getIconFromName(folder.icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = folder.color?.let { Color(it.toLong().toInt()) }
                                            ?: MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(folder.name, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (exportAll) {
                        onConfirm(null)
                    } else if (selectedFolderIds.isNotEmpty()) {
                        onConfirm(selectedFolderIds.toList())
                    }
                },
                enabled = exportAll || selectedFolderIds.isNotEmpty()
            ) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
