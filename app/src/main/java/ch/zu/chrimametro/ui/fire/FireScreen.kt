/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
 */
package ch.zu.chrimametro.ui.fire

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.zu.chrimametro.ui.theme.BrandAmber
import ch.zu.chrimametro.ui.theme.BrandEmerald
import ch.zu.chrimametro.ui.theme.BrandIndigo
import ch.zu.chrimametro.ui.theme.BrandRose
import ch.zu.chrimametro.ui.theme.BrandSky
import ch.zu.chrimametro.ui.theme.BrandTeal
import ch.zu.chrimametro.ui.theme.BrandViolet
import ch.zu.chrimametro.ui.theme.OnBrand
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun FireScreen(viewModel: FireViewModel) {
    val state by viewModel.simulationState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentPadding = PaddingValues(bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "FIRE Dashboard",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                )
            }

            item {
                DashboardOverviewSection(state.inputs, state.outputs)
            }

            item {
                CollapsibleSection(
                    title = "Planning Inputs",
                    subtitle = "Age, savings, spending and core assumptions",
                    initiallyExpanded = false
                ) {
                    InputsSection(state.inputs, viewModel)
                }
            }

            item {
                CollapsibleSection(
                    title = "Simulation Strategy",
                    subtitle = "Deterministic vs Monte Carlo and withdrawal policy",
                    initiallyExpanded = false
                ) {
                    SimulationStrategySection(state.inputs, viewModel)
                }
            }

            item {
                CollapsibleSection(
                    title = "Third Pillar",
                    subtitle = state.inputs.thirdPillarStrategy.displayName,
                    initiallyExpanded = false
                ) {
                    ThirdPillarSection(state.inputs, viewModel)
                }
            }

            item {
                CollapsibleSection(
                    title = "Target Allocation",
                    subtitle = state.inputs.targetAllocations
                        .filter { it.percentage > 0.0 }
                        .joinToString(" • ") { "${(it.percentage * 100).roundToInt()}% ${it.assetName}" },
                    initiallyExpanded = false
                ) {
                    TargetAllocationSection(state.inputs, viewModel)
                }
            }

            item {
                CollapsibleSection(
                    title = "Assets",
                    subtitle = "${state.inputs.assets.size} assets configured",
                    initiallyExpanded = false
                ) {
                    AssetsSection(state.inputs.assets, viewModel)
                }
            }

            item {
                CollapsibleSection(
                    title = "Assumptions",
                    subtitle = "Transparent model inputs",
                    initiallyExpanded = false
                ) {
                    AssumptionsCard(state.inputs)
                }
            }

            if (state.outputs.allocationAtFire.isNotEmpty()) {
                item {
                    CollapsibleSection(
                        title = "Projected Allocation at FIRE",
                        subtitle = "How the investable portfolio evolves",
                        initiallyExpanded = false
                    ) {
                        AllocationAtFireCard(state.outputs.allocationAtFire)
                    }
                }
            }

            if (state.outputs.portfolioEvolution.isNotEmpty()) {
                item {
                    CollapsibleSection(
                        title = "Yearly Breakdown",
                        subtitle = "Detailed accounting by year",
                        initiallyExpanded = false
                    ) {
                        YearlyBreakdownTable(state.outputs.portfolioEvolution)
                    }
                }
            }

            if (state.outputs.readinessByAge.isNotEmpty()) {
                item {
                    CollapsibleSection(
                        title = "Earliest Sustainable Retirement Age Breakdown",
                        subtitle = "Projected vs required portfolio by age",
                        initiallyExpanded = false
                    ) {
                        FireReadinessTable(state.outputs.readinessByAge)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun InputsSection(inputs: FireInputs, viewModel: FireViewModel) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NumberInput(
            label = "Current Age",
            value = inputs.currentAge.toDouble(),
            onValueChange = { viewModel.updateCurrentAge(it.toInt()) }
        )
        NumberInput(
            label = "Target FIRE Age",
            value = inputs.targetFireAge.toDouble(),
            onValueChange = { viewModel.updateTargetFireAge(it.toInt()) }
        )
        NumberInput(
            label = "Life Expectancy",
            value = inputs.lifeExpectancy.toDouble(),
            onValueChange = { viewModel.updateLifeExpectancy(it.toInt()) }
        )
        NumberInput(
            label = "Return to Italy Year",
            value = inputs.returnToItalyYear.toDouble(),
            onValueChange = { viewModel.updateReturnToItalyYear(it.toInt()) }
        )
        NumberInput(
            label = "Swiss Yearly Savings",
            value = inputs.swissYearlySavings,
            onValueChange = { viewModel.updateSwissYearlySavings(it) }
        )
        NumberInput(
            label = "Italian Yearly Savings",
            value = inputs.italianYearlySavings,
            onValueChange = { viewModel.updateItalianYearlySavings(it) }
        )
        NumberInput(
            label = "Monthly Spending (CHF)",
            value = inputs.monthlySpendings,
            onValueChange = { viewModel.updateMonthlySpendings(it) }
        )
        PercentageInput(
            label = "Inflation",
            value = inputs.expectedInflation * 100,
            onValueChange = { viewModel.updateExpectedInflation(it / 100) }
        )
        PercentageInput(
            label = "ETF Return",
            value = inputs.expectedEtfReturn * 100,
            onValueChange = { viewModel.updateExpectedEtfReturn(it / 100) }
        )
        PercentageInput(
            label = "Bond Return",
            value = inputs.expectedBondReturn * 100,
            onValueChange = { viewModel.updateExpectedBondReturn(it / 100) }
        )
        PercentageInput(
            label = "Crypto Return",
            value = inputs.expectedCryptoReturn * 100,
            onValueChange = { viewModel.updateExpectedCryptoReturn(it / 100) }
        )
        PercentageInput(
            label = "Gold Return",
            value = inputs.expectedGoldReturn * 100,
            onValueChange = { viewModel.updateExpectedGoldReturn(it / 100) }
        )
        NumberInput(
            label = "Swiss Pension Age",
            value = inputs.swissPensionStartAge.toDouble(),
            onValueChange = { viewModel.updateSwissPensionStartAge(it.toInt()) }
        )
        NumberInput(
            label = "Swiss Pension (CHF/year)",
            value = inputs.swissPension,
            onValueChange = { viewModel.updateSwissPension(it) }
        )
        NumberInput(
            label = "Swiss+Italian Pension Age",
            value = inputs.combinedPensionStartAge.toDouble(),
            onValueChange = { viewModel.updateCombinedPensionStartAge(it.toInt()) }
        )
        NumberInput(
            label = "Swiss+Italian Pension (CHF/year)",
            value = inputs.combinedPension,
            onValueChange = { viewModel.updateCombinedPension(it) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThirdPillarSection(inputs: FireInputs, viewModel: FireViewModel) {
    var expanded by remember { mutableStateOf(false) }
    var taxExpanded by remember { mutableStateOf(false) }
    val taxOptions = remember {
        listOf(0.0, 0.02, 0.04, 0.06, 0.08, 0.10)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Third Pillar", style = MaterialTheme.typography.titleMedium)
            Text(
                "Choose the redemption strategy and a default tax rate.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = inputs.thirdPillarStrategy.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Strategy") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    ThirdPillarStrategy.values().forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.displayName) },
                            onClick = {
                                viewModel.updateThirdPillarStrategy(option)
                                expanded = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = taxExpanded,
                onExpandedChange = { taxExpanded = !taxExpanded }
            ) {
                OutlinedTextField(
                    value = formatPercentageInput(inputs.thirdPillarRedemptionTaxRate * 100),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Default tax") },
                    suffix = { Text("%") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = taxExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                DropdownMenu(
                    expanded = taxExpanded,
                    onDismissRequest = { taxExpanded = false }
                ) {
                    taxOptions.forEach { rate ->
                        DropdownMenuItem(
                            text = { Text("${formatPercentageInput(rate * 100)}%") },
                            onClick = {
                                viewModel.updateThirdPillarRedemptionTaxRate(rate)
                                taxExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetAllocationSection(inputs: FireInputs, viewModel: FireViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Target Allocation", style = MaterialTheme.typography.titleMedium)
            Text(
                "New contributions and matured assets automatically follow this allocation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            PercentageInput(
                label = "ETF Stocks %",
                value = targetAllocationPercentage(inputs, "ETF Stocks") * 100,
                onValueChange = { viewModel.updateTargetAllocation("ETF Stocks", it / 100) }
            )
            PercentageInput(
                label = "ETF Bonds %",
                value = targetAllocationPercentage(inputs, "ETF Bonds") * 100,
                onValueChange = { viewModel.updateTargetAllocation("ETF Bonds", it / 100) }
            )
            PercentageInput(
                label = "Gold %",
                value = targetAllocationPercentage(inputs, "Gold") * 100,
                onValueChange = { viewModel.updateTargetAllocation("Gold", it / 100) }
            )
            PercentageInput(
                label = "Crypto %",
                value = targetAllocationPercentage(inputs, "Crypto") * 100,
                onValueChange = { viewModel.updateTargetAllocation("Crypto", it / 100) }
            )
        }
    }
}

@Composable
private fun AssetsSection(assets: List<AssetType>, viewModel: FireViewModel) {
    var showAddAsset by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Assets", style = MaterialTheme.typography.titleSmall)
            Button(onClick = { showAddAsset = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Asset", modifier = Modifier.padding(end = 4.dp))
                Text("Add")
            }
        }

        AssetGroup.values().forEach { group ->
            val groupedAssets = assets.filter { it.group == group }
            if (groupedAssets.isNotEmpty()) {
                AssetGroupCard(group, groupedAssets, viewModel)
            }
        }

        if (showAddAsset) {
            AddAssetDialog(
                onAdd = {
                    viewModel.addAsset(it)
                    showAddAsset = false
                },
                onDismiss = { showAddAsset = false }
            )
        }
    }
}

@Composable
private fun AssetGroupCard(
    group: AssetGroup,
    assets: List<AssetType>,
    viewModel: FireViewModel
) {
    var expanded by remember { mutableStateOf(group == AssetGroup.INVESTABLE) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        group.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        "${assets.size} assets • ${currencyShort(assets.sumOf { it.currentValue })}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            if (expanded) {
                assets.forEachIndexed { index, asset ->
                    AssetCard(
                        asset = asset,
                        onDelete = { viewModel.removeAssetByName(asset.name) },
                        onValueChange = { newValue ->
                            viewModel.updateAssetValue(asset.name, newValue)
                        }
                    )
                    if (index < assets.lastIndex) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetCard(
    asset: AssetType,
    onDelete: () -> Unit,
    onValueChange: (Double) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { expanded = !expanded },
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(asset.name, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${currencyShort(asset.currentValue)} • ${(asset.expectedAnnualReturn * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        if (expanded) "Hide details" else "Show details",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }

            if (expanded) {
                Divider()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Asset value editor
                    var editingValue by remember { mutableStateOf(asset.currentValue.toInt().toString()) }
                    OutlinedTextField(
                        value = editingValue,
                        onValueChange = {
                            editingValue = it
                            it.toDoubleOrNull()?.let(onValueChange)
                        },
                        label = { Text("Current Value (CHF)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    Text(asset.group.displayName, style = MaterialTheme.typography.labelSmall)
                    asset.events.forEach { event ->
                        Text(
                            "Maturity: ${eventDateLabel(event)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (event.finalValue != null) {
                            Text(
                                "Final value: CHF ${event.finalValue.roundToInt()}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        val destinations = eventDestinationsLabel(event)
                        if (destinations.isNotBlank()) {
                            Text(
                                destinations,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (asset.events.isEmpty()) {
                        Text(
                            "No maturity or transfer events configured.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAssetDialog(
    onAdd: (AssetType) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("0") }
    var returnRate by remember { mutableStateOf("0") }
    var group by remember { mutableStateOf(AssetGroup.INVESTABLE) }
    var expanded by remember { mutableStateOf(false) }
    var maturityYear by remember { mutableStateOf("0") }
    var maturityMonth by remember { mutableStateOf("0") }
    var finalValue by remember { mutableStateOf("0") }
    var etfStocksPct by remember { mutableStateOf("0") }
    var etfBondsPct by remember { mutableStateOf("0") }
    var cryptoPct by remember { mutableStateOf("0") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(modifier = Modifier.padding(16.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Add Asset", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Asset Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = group.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Asset Group") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        AssetGroup.values().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.displayName) },
                                onClick = {
                                    group = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                NumberInput(
                    label = "Current Value",
                    value = value.toDoubleOrNull() ?: 0.0,
                    onValueChange = { value = it.toString() }
                )

                PercentageInput(
                    label = "Annual Return %",
                    value = returnRate.toDoubleOrNull() ?: 0.0,
                    onValueChange = { returnRate = it.toString() }
                )

                Text("Optional maturity event", style = MaterialTheme.typography.titleSmall)

                NumberInput(
                    label = "Maturity Year",
                    value = maturityYear.toDoubleOrNull() ?: 0.0,
                    onValueChange = { maturityYear = it.toInt().toString() }
                )
                NumberInput(
                    label = "Maturity Month (1-12)",
                    value = maturityMonth.toDoubleOrNull() ?: 0.0,
                    onValueChange = { maturityMonth = it.toInt().toString() }
                )
                NumberInput(
                    label = "Final Value",
                    value = finalValue.toDoubleOrNull() ?: 0.0,
                    onValueChange = { finalValue = it.toString() }
                )
                PercentageInput(
                    label = "ETF Stocks destination %",
                    value = etfStocksPct.toDoubleOrNull() ?: 0.0,
                    onValueChange = { etfStocksPct = it.toString() }
                )
                PercentageInput(
                    label = "ETF Bonds destination %",
                    value = etfBondsPct.toDoubleOrNull() ?: 0.0,
                    onValueChange = { etfBondsPct = it.toString() }
                )
                PercentageInput(
                    label = "Crypto destination %",
                    value = cryptoPct.toDoubleOrNull() ?: 0.0,
                    onValueChange = { cryptoPct = it.toString() }
                )

                Button(modifier = Modifier.fillMaxWidth(), onClick = onDismiss) {
                    Text("Cancel")
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (name.isNotBlank()) {
                            val destinationAllocations = listOf(
                                AllocationTarget("ETF Stocks", (etfStocksPct.toDoubleOrNull() ?: 0.0) / 100),
                                AllocationTarget("ETF Bonds", (etfBondsPct.toDoubleOrNull() ?: 0.0) / 100),
                                AllocationTarget("Crypto", (cryptoPct.toDoubleOrNull() ?: 0.0) / 100)
                            ).filter { it.percentage > 0.0 }

                            val eventYear = maturityYear.toIntOrNull() ?: 0
                            val events = if (eventYear > 0) {
                                listOf(
                                    AssetEvent(
                                        year = eventYear,
                                        month = (maturityMonth.toIntOrNull() ?: 0).takeIf { it in 1..12 },
                                        type = AssetEventType.REINVESTMENT,
                                        finalValue = (finalValue.toDoubleOrNull() ?: 0.0).takeIf { it > 0.0 },
                                        destinationAllocations = destinationAllocations
                                    )
                                )
                            } else {
                                emptyList()
                            }

                            onAdd(
                                AssetType(
                                    name = name,
                                    currentValue = value.toDoubleOrNull() ?: 0.0,
                                    expectedAnnualReturn = (returnRate.toDoubleOrNull() ?: 0.0) / 100,
                                    group = group,
                                    events = events
                                )
                            )
                        }
                    }
                ) {
                    Text("Add")
                }
            }
        }
    }
}

@Composable
private fun OutputsSection(inputs: FireInputs, outputs: FireOutputs) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Outputs & Projections",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SummaryCard(inputs, outputs)

            AssumptionsCard(inputs)

            if (outputs.allocationAtFire.isNotEmpty()) {
                AllocationAtFireCard(outputs.allocationAtFire)
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            OutputMetric(
                label = "Current Investable Portfolio",
                value = currency(inputs.investablePortfolio)
            )

            OutputMetric(
                label = "Projected Portfolio at FIRE",
                value = currency(outputs.projectedPortfolioAtFire)
            )

            OutputMetric(
                label = "Required FIRE Portfolio",
                value = currency(outputs.requiredFirePortfolio)
            )

            OutputMetric(
                label = "FI % (Current Investable / Required FIRE)",
                value = "${outputs.fiPercentage.toInt()}%"
            )

            OutputMetric(
                label = "Earliest Sustainable Retirement Age",
                value = ageLabel(outputs.recommendedFireAge)
            )

            OutputMetric(
                label = "Coast FIRE Age",
                value = ageLabel(outputs.coastFireAge)
            )

            OutputMetric(
                label = "Barista FIRE Age",
                value = ageLabel(outputs.baristaFireAge)
            )

            OutputMetric(
                label = "Probability of Success",
                value = "TBD"
            )

            if (outputs.portfolioEvolution.isNotEmpty()) {
                YearlyBreakdownTable(outputs.portfolioEvolution)
            }

            if (outputs.readinessByAge.isNotEmpty()) {
                FireReadinessTable(outputs.readinessByAge)
            }
        }
    }
}

@Composable
private fun DashboardOverviewSection(inputs: FireInputs, outputs: FireOutputs) {
    val progress = (outputs.fiPercentage / 100.0).coerceIn(0.0, 1.0).toFloat()
    val modeLabel = if (inputs.simulationMode == SimulationMode.MONTE_CARLO) {
        "Monte Carlo"
    } else {
        "Deterministic"
    }
    val projectedLabel = if (inputs.simulationMode == SimulationMode.MONTE_CARLO) {
        "Projected at FIRE (MC Median)"
    } else {
        "Projected at FIRE"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "At a glance",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(BrandSky, BrandIndigo)
                            ),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            projectedLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = OnBrand.copy(alpha = 0.85f)
                        )
                        Text(
                            currencyShort(outputs.projectedPortfolioAtFire),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnBrand
                        )
                        Text(
                            "Need ${currencyShort(outputs.requiredFirePortfolio)} to reach FIRE",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnBrand.copy(alpha = 0.85f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DashboardMiniChip("FIRE ${outputs.fiPercentage.roundToInt()}%")
                            DashboardMiniChip("Age ${ageLabel(outputs.recommendedFireAge)}")
                            DashboardMiniChip(modeLabel)
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DashboardMetricCard(
                        label = "Net Worth",
                        value = currencyShort(inputs.totalNetWorth),
                        icon = Icons.Default.Star,
                        accent = BrandIndigo,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardMetricCard(
                        label = "Investable",
                        value = currencyShort(inputs.investablePortfolio),
                        icon = Icons.Default.CheckCircle,
                        accent = BrandEmerald,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DashboardMetricCard(
                        label = "Required at FIRE",
                        value = currencyShort(outputs.requiredFirePortfolio),
                        icon = Icons.Default.Warning,
                        accent = BrandAmber,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardMetricCard(
                        label = "FIRE Progress",
                        value = "${outputs.fiPercentage.roundToInt()}%",
                        icon = Icons.Default.Favorite,
                        accent = BrandRose,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DashboardMetricCard(
                        label = "Earliest FIRE Age",
                        value = ageLabel(outputs.recommendedFireAge),
                        icon = Icons.Default.DateRange,
                        accent = BrandViolet,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardMetricCard(
                        label = "Coast FIRE",
                        value = ageLabel(outputs.coastFireAge),
                        icon = Icons.Default.Info,
                        accent = BrandTeal,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DashboardMetricCard(
                        label = "Barista FIRE",
                        value = ageLabel(outputs.baristaFireAge),
                        icon = Icons.Default.Settings,
                        accent = BrandSky,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            if (inputs.simulationMode == SimulationMode.MONTE_CARLO) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DashboardMetricCard(
                        label = "MC Success Probability",
                        value = "${outputs.probabilityOfSuccess.roundToInt()}%",
                        icon = Icons.Default.CheckCircle,
                        accent = BrandEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardMetricCard(
                        label = "MC Iterations",
                        value = "${inputs.monteCarloIterations}",
                        icon = Icons.Default.Refresh,
                        accent = BrandIndigo,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "FIRE Progress",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${outputs.fiPercentage.roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandIndigo
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = BrandIndigo,
                    trackColor = BrandIndigo.copy(alpha = 0.15f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Current: ${currencyShort(inputs.investablePortfolio)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Required: ${currencyShort(outputs.requiredFirePortfolio)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val gap = (outputs.requiredFirePortfolio - inputs.investablePortfolio).coerceAtLeast(0.0)
                    Text(
                        "Gap: ${currencyShort(gap)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (gap > 0) BrandRose else BrandEmerald
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardMiniChip(text: String) {
    Box(
        modifier = Modifier
            .background(
                color = OnBrand.copy(alpha = 0.18f),
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = OnBrand
        )
    }
}

@Composable
private fun DashboardMetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            color = accent.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(9.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CollapsibleSection(
    title: String,
    subtitle: String? = null,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            if (expanded) {
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                content()
            }
        }
    }
}

@Composable
private fun OutputMetric(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun YearlyBreakdownTable(evolution: List<YearProjection>) {
    val expandedYearIndices = remember { mutableStateOf(setOf<Int>()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        "Year",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(0.7f)
                    )
                    Text(
                        "Start",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1.0f)
                    )
                    Text(
                        "Contrib",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1.0f)
                    )
                    Text(
                        "Returns",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1.0f)
                    )
                    Text(
                        "Withdraw",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1.0f)
                    )
                    Text(
                        "End",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1.1f)
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                evolution.forEachIndexed { index, projection ->
                    val isExpanded = index in expandedYearIndices.value
                    val rowBg = if (index % 2 == 0)
                        MaterialTheme.colorScheme.surfaceVariant
                    else
                        MaterialTheme.colorScheme.surface

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBg)
                            .clickable {
                                expandedYearIndices.value = if (isExpanded) {
                                    expandedYearIndices.value - index
                                } else {
                                    expandedYearIndices.value + index
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "${projection.year}\nage ${projection.age}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.weight(0.6f)
                        )
                        Text(
                            currencyShort(projection.startPortfolio),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1.0f)
                        )
                        Text(
                            currencyShort(projection.contributions),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1.0f)
                        )
                        Text(
                            currencyShort(projection.returns),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (projection.returns >= 0)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1.0f)
                        )
                        Text(
                            currencyShort(projection.withdrawals),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (projection.withdrawals >= 0)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1.0f)
                        )
                        Text(
                            currencyShort(projection.endPortfolio),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1.1f)
                        )
                    }

                    // Expandable detail row
                    if (isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(rowBg)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            // Asset allocation breakdown
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                projection.assetAllocation.forEach { asset ->
                                    if (asset.value > 0.0) {
                                        val pct = (asset.value / projection.endPortfolio * 100).toInt()
                                        Column(
                                            modifier = Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .padding(6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                asset.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                "$pct%",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Events
                            if (projection.events.isNotEmpty()) {
                                Text(
                                    "Events:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                projection.events.forEach { event ->
                                    Text(
                                        "• ${event.displayLabel} — ${currencyShort(event.netAmount)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }

                            // Balance error
                            if (projection.balanceError != 0.0) {
                                Text(
                                    "⚠ Balance drift: ${currencyShort(projection.balanceError)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    if (index < evolution.lastIndex) {
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FireReadinessTable(readinessByAge: List<FireReadinessRow>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            "Recommended age = first row where Projected >= Required",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        "Age",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(0.8f)
                    )
                    Text(
                        "Required",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1.2f)
                    )
                    Text(
                        "Projected",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1.2f)
                    )
                    Text(
                        "Diff",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1.0f)
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                readinessByAge.forEachIndexed { index, row ->
                    val rowBg =
                        if (index % 2 == 0) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBg)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${row.age}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.8f))
                        Text(
                            currencyShort(row.requiredPortfolio),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1.2f)
                        )
                        Text(
                            currencyShort(row.projectedPortfolio),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1.2f)
                        )
                        Text(
                            currencyShort(row.difference),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                            color = if (row.difference >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1.0f)
                        )
                    }

                    if (index < readinessByAge.lastIndex) {
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberInput(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf(formatNumberInput(value)) }

    LaunchedEffect(value) {
        val formatted = formatNumberInput(value)
        if (text != formatted) {
            text = formatted
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = { newValue ->
            text = newValue
            val normalized = newValue.replace(',', '.')
            if (normalized.isBlank()) {
                onValueChange(0.0)
            } else if (normalized != "." && normalized != "-" && normalized != "-." && normalized.toDoubleOrNull() != null) {
                onValueChange(normalized.toDouble())
            }
        },
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true
    )
}

private fun formatNumberInput(value: Double): String {
    if (value == 0.0) return ""
    val rounded = String.format(Locale.US, "%.2f", value)
    return rounded.trimEnd('0').trimEnd('.')
}

@Composable
private fun PercentageInput(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf(formatPercentageInput(value)) }

    LaunchedEffect(value) {
        val formatted = formatPercentageInput(value)
        if (text != formatted) {
            text = formatted
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = { newValue ->
            text = newValue
            val normalized = newValue.replace(',', '.')
            if (normalized.isBlank()) {
                onValueChange(0.0)
            } else if (normalized != "." && normalized != "-" && normalized != "-." && normalized.toDoubleOrNull() != null) {
                onValueChange(normalized.toDouble())
            }
        },
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        suffix = { Text("%") },
        singleLine = true
    )
}

private fun formatPercentageInput(value: Double): String {
    if (value == 0.0) return ""
    val rounded = String.format(Locale.US, "%.2f", value)
    return rounded.trimEnd('0').trimEnd('.')
}

private fun targetAllocationPercentage(inputs: FireInputs, assetName: String): Double {
    return inputs.targetAllocations.firstOrNull { it.assetName == assetName }?.percentage ?: 0.0
}

private fun eventDateLabel(event: AssetEvent): String {
    val monthLabel = event.month?.let { monthName(it) }
    return if (monthLabel != null) "$monthLabel ${event.year}" else event.year.toString()
}

private fun eventDestinationsLabel(event: AssetEvent): String {
    val allocations = if (event.destinationAllocations.isNotEmpty()) {
        event.destinationAllocations
    } else {
        event.targetAssetNames.map { AllocationTarget(it, 1.0 / event.targetAssetNames.size.coerceAtLeast(1)) }
    }
    if (allocations.isEmpty()) return ""

    val label = allocations.joinToString(", ") {
        "${(it.percentage * 100).roundToInt()}% ${it.assetName}"
    }
    return "→ $label"
}

private fun monthName(month: Int): String {
    return when (month) {
        1 -> "Jan"
        2 -> "Feb"
        3 -> "Mar"
        4 -> "Apr"
        5 -> "May"
        6 -> "Jun"
        7 -> "Jul"
        8 -> "Aug"
        9 -> "Sep"
        10 -> "Oct"
        11 -> "Nov"
        12 -> "Dec"
        else -> ""
    }
}

private fun currency(value: Double): String {
    return "CHF ${value.roundToInt()}"
}

private fun currencyShort(value: Double): String {
    val abs = Math.abs(value)
    val sign = if (value < 0) "-" else ""
    return when {
        abs >= 1_000_000 -> "${sign}CHF ${"%.1f".format(abs / 1_000_000)}M"
        abs >= 1_000 -> "${sign}CHF ${"%.0f".format(abs / 1_000)}k"
        else -> "${sign}CHF ${abs.roundToInt()}"
    }
}

private fun ageLabel(age: Int?): String {
    return if (age == null) "Not reached" else "$age years"
}

@Composable
private fun SummaryCard(inputs: FireInputs, outputs: FireOutputs) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Net Worth Summary",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            OutputMetric("Total Net Worth", currency(inputs.totalNetWorth))
            OutputMetric("Investable Assets", currency(inputs.investablePortfolio))
            OutputMetric("Retirement Assets", currency(outputs.retirementAssetsAtStart))
            OutputMetric("Excluded Assets", currency(outputs.excludedAssetsAtStart))
        }
    }
}

@Composable
private fun AssumptionsCard(inputs: FireInputs) {
    val targetAlloc = inputs.targetAllocations
        .filter { it.percentage > 0.0 }
        .joinToString(", ") { "${(it.percentage * 100).roundToInt()}% ${it.assetName}" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Assumptions",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            OutputMetric("Simulation Mode", inputs.simulationMode.displayName)
            OutputMetric("Withdrawal Strategy", inputs.withdrawalStrategy.displayName)
            OutputMetric("Inflation", "${(inputs.expectedInflation * 100).roundToInt()}%")
            OutputMetric("Expected ETF Return", "${(inputs.expectedEtfReturn * 100).roundToInt()}%")
            OutputMetric("Expected Bond Return", "${(inputs.expectedBondReturn * 100).roundToInt()}%")
            OutputMetric("Expected Crypto Return", "${(inputs.expectedCryptoReturn * 100).roundToInt()}%")
            OutputMetric("Expected Gold Return", "${(inputs.expectedGoldReturn * 100).roundToInt()}%")
            OutputMetric("Swiss Savings (yearly)", currency(inputs.swissYearlySavings))
            OutputMetric("Italian Savings (yearly)", currency(inputs.italianYearlySavings))
            OutputMetric("Monthly Spending", currency(inputs.monthlySpendings))
            OutputMetric("Target FIRE Age", "${inputs.targetFireAge}")
            OutputMetric("Life Expectancy", "${inputs.lifeExpectancy}")
            OutputMetric("Swiss Pension Age", "${inputs.swissPensionStartAge}")
            OutputMetric("Swiss Pension", currency(inputs.swissPension))
            OutputMetric("Swiss+Italian Pension Age", "${inputs.combinedPensionStartAge}")
            OutputMetric("Swiss+Italian Pension", currency(inputs.combinedPension))
            OutputMetric("Third Pillar Strategy", inputs.thirdPillarStrategy.displayName)
            OutputMetric(
                "3rd Pillar Redemption Tax",
                "${(inputs.thirdPillarRedemptionTaxRate * 100).roundToInt()}%"
            )
            if (targetAlloc.isNotBlank()) {
                OutputMetric("Target Allocation", targetAlloc)
            }
        }
    }
}

@Composable
private fun AllocationAtFireCard(slices: List<AllocationSlice>) {
    val investable = slices.filter { it.group.countsInPortfolio }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Projected Allocation at FIRE",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            investable.forEach { slice ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(slice.assetName, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "${(slice.share * 100).roundToInt()}% • ${currencyShort(slice.value)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            val retirement = slices.filter { it.group == AssetGroup.RETIREMENT }
            if (retirement.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    "Retirement (excluded from investable)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                retirement.forEach { slice ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(slice.assetName, style = MaterialTheme.typography.bodySmall)
                        Text(currencyShort(slice.value), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun SimulationStrategySection(inputs: FireInputs, viewModel: FireViewModel) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Withdrawal strategy", style = MaterialTheme.typography.labelMedium)
        WithdrawalStrategy.values().forEach { strategy ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                RadioButton(
                    selected = inputs.withdrawalStrategy == strategy,
                    onClick = { viewModel.updateWithdrawalStrategy(strategy) }
                )
                Text(strategy.displayName, style = MaterialTheme.typography.bodySmall)
            }
        }

        Divider()

        Text("Simulation mode", style = MaterialTheme.typography.labelMedium)
        SimulationMode.values().forEach { mode ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                RadioButton(
                    selected = inputs.simulationMode == mode,
                    onClick = { viewModel.updateSimulationMode(mode) }
                )
                Text(mode.displayName, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
