package com.example.expenceflow.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveRatesScreen(
    viewModel: LiveRatesViewModel = viewModel()
) {
    val marketState by viewModel.marketState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Market Rates",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshRates(force = true) }) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = marketState) {
                is MarketUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is MarketUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Unable to fetch latest rates", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.refreshRates(force = true) }) {
                            Text("Try Again")
                        }
                    }
                }
                is MarketUiState.Success -> {
                    val market = state.data
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            MarketHeaderCard(market)
                        }
                        item {
                            SectionTitle("Precious Metals")
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                MarketTileModern(
                                    title = "Gold 24K",
                                    value = "₹%,.0f".format(market.gold.gold24),
                                    unit = "/g",
                                    icon = Icons.Default.CurrencyRupee,
                                    modifier = Modifier.weight(1f)
                                )
                                MarketTileModern(
                                    title = "Gold 22K",
                                    value = "₹%,.0f".format(market.gold.gold22),
                                    unit = "/g",
                                    icon = Icons.Default.CurrencyRupee,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                MarketTileModern(
                                    title = "Silver",
                                    value = "₹%,.2f".format(market.silver.gram),
                                    unit = "/g",
                                    icon = Icons.Default.CurrencyRupee,
                                    modifier = Modifier.weight(1f)
                                )
                                MarketTileModern(
                                    title = "Silver",
                                    value = "₹%,.0f".format(market.silver.kg),
                                    unit = "/kg",
                                    icon = Icons.Default.CurrencyRupee,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        item {
                            SectionTitle("Fuel Prices")
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                MarketTileModern(
                                    title = "Petrol",
                                    value = "₹%,.2f".format(market.fuel.petrol),
                                    unit = "/L",
                                    icon = Icons.Default.LocalGasStation,
                                    modifier = Modifier.weight(1f)
                                )
                                MarketTileModern(
                                    title = "Diesel",
                                    value = "₹%,.2f".format(market.fuel.diesel),
                                    unit = "/L",
                                    icon = Icons.Default.LocalGasStation,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        item {
                            Text(
                                text = "Last updated: ${market.updatedAt}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarketHeaderCard(market: MarketResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                "Market Overview",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    "₹%,.0f".format(market.gold.gold24),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    " /g Gold",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun MarketTileModern(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}
