package com.mrhayami.vaultio.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mrhayami.vaultio.data.remote.TcgDexCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataModal(
    card: TcgDexCard,
    onConfirm: (Int, String, String, String) -> Unit,
    onBack: () -> Unit
) {
    var quantity by remember { mutableIntStateOf(1) }
    var condition by remember { mutableStateOf("Near Mint") }
    var printing by remember { mutableStateOf("Standard") }
    var finish by remember { mutableStateOf("Non Holo") }

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            Text("Add ${card.name}", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(16.dp))

        AsyncImage(
            model = "${card.image}/high.webp",
            contentDescription = null,
            modifier = Modifier
                .height(250.dp)
                .align(Alignment.CenterHorizontally),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Quantity: $quantity", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { if (quantity > 1) quantity-- }) { Icon(Icons.Rounded.Remove, null) }
            IconButton(onClick = { quantity++ }) { Icon(Icons.Rounded.Add, null) }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val conditions = listOf("Mint", "Near Mint", "Lightly Played", "Moderately Played", "Heavily Played", "Damaged")
        var expandedCondition by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expandedCondition,
            onExpandedChange = { expandedCondition = !expandedCondition }
        ) {
            OutlinedTextField(
                value = condition,
                onValueChange = {},
                readOnly = true,
                label = { Text("Condition") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCondition) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expandedCondition,
                onDismissRequest = { expandedCondition = false }
            ) {
                conditions.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            condition = item
                            expandedCondition = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val printings = listOf("Standard", "Holo", "Reverse Holo", "1st Edition")
        var expandedPrinting by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expandedPrinting,
            onExpandedChange = { expandedPrinting = !expandedPrinting }
        ) {
            OutlinedTextField(
                value = printing,
                onValueChange = {},
                readOnly = true,
                label = { Text("Printing") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPrinting) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expandedPrinting,
                onDismissRequest = { expandedPrinting = false }
            ) {
                printings.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            printing = item
                            expandedPrinting = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val finishes = listOf("Non Holo", "Holo", "Reverse Holo", "Textured", "Gold")
        var expandedFinish by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expandedFinish,
            onExpandedChange = { expandedFinish = !expandedFinish }
        ) {
            OutlinedTextField(
                value = finish,
                onValueChange = {},
                readOnly = true,
                label = { Text("Finish") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFinish) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expandedFinish,
                onDismissRequest = { expandedFinish = false }
            ) {
                finishes.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            finish = item
                            expandedFinish = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onConfirm(quantity, condition, printing, finish) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add to Collection")
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
