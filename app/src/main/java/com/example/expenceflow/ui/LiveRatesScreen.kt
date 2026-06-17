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

    val infiniteTransition =
        rememberInfiniteTransition(label = "rotation")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Live Market",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Gold • Silver • Fuel",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.refreshRates()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier =
                                if (isRefreshing)
                                    Modifier.rotate(rotation)
                                else Modifier
                        )
                    }
                }
            )
        }
    ) { padding ->

        when (val state = marketState) {

            is MarketUiState.Loading -> {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is MarketUiState.Error -> {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text("Failed to load market data")

                        Spacer(
                            Modifier.height(12.dp)
                        )

                        Button(
                            onClick = {
                                viewModel.refreshRates()
                            }
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            is MarketUiState.Success -> {

                val market = state.data

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    item {

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp)
                        ) {

                            Box(
                                modifier = Modifier
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFFFFD54F),
                                                Color(0xFFFF8F00)
                                            )
                                        )
                                    )
                                    .fillMaxWidth()
                                    .padding(24.dp)
                            ) {

                                Column {

                                    Text(
                                        text = "📈 Market Dashboard",
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(
                                        Modifier.height(10.dp)
                                    )

                                    Text(
                                        text = "24K Gold ₹%,.0f/g".format(
                                            market.gold.gold24
                                        ),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Silver ₹%,.2f/g".format(
                                            market.silver.gram
                                        ),
                                        color = Color.White
                                    )

                                    Spacer(
                                        Modifier.height(8.dp)
                                    )

                                    Text(
                                        text = market.updatedAt,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }

                    item {

                        Text(
                            text = "Gold Rates",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {

                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(12.dp)
                        ) {

                            MarketTile(
                                title = "24K",
                                value ="₹%,.0f/g".format(market.gold.gold24),
                                icon = Icons.Default.CurrencyRupee,
                                gradient = listOf(
                                    Color(0xFF2196F3),
                                    Color(0xFFFFB300)
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            MarketTile(
                                title = "22K",
                                value = "₹%,.0f/g".format(market.gold.gold22),
                                icon = Icons.Default.CurrencyRupee,
                                gradient = listOf(
                                    Color(0xFF673AB7),
                                    Color(0xFFFFA000)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {

                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(12.dp)
                        ) {

                            MarketTile(
                                title = "18K",
                                value = "₹%,.0f/g".format(market.gold.gold18),
                                icon = Icons.Default.CurrencyRupee,
                                gradient = listOf(
                                    Color(0xFFFFAF4D),
                                    Color(0xFFCDDC39)
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(120.dp),
                                shape = RoundedCornerShape(24.dp)
                            ) {

                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {

                                    Text(
                                        "Silver",
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(
                                        Modifier.height(8.dp)
                                    )

                                    Text("₹%,.2f/g".format(market.silver.gram))
                                    Text("₹%,.0f/kg".format(market.silver.kg))
                                }
                            }
                        }
                    }

                    item {

                        Text(
                            text = "Fuel Prices",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {

                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(12.dp)
                        ) {

                            MarketTile(
                                title = "Petrol",
                                value = "₹%,.2f/L".format(market.fuel.petrol),
                                icon = Icons.Default.LocalGasStation,
                                gradient = listOf(
                                    Color(0xFF43A047),
                                    Color(0xFF2E7D32)
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            MarketTile(
                                title = "Diesel",
                                value = "₹%,.2f/L".format(market.fuel.diesel),
                                icon = Icons.Default.LocalGasStation,
                                gradient = listOf(
                                    Color(0xFF1E88E5),
                                    Color(0xFF1565C0)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Spacer(
                            Modifier.height(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MarketTile(
    title: String,
    value: String,
    icon: ImageVector,
    gradient: List<Color>,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(24.dp)
    ) {

        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(gradient)
                )
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Column {

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White
                )

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = title,
                    color = Color.White
                )

                Text(
                    text = value,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}