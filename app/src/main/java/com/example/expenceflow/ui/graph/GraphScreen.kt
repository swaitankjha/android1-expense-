package com.example.expenceflow.ui.graph

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.expenceflow.ui.transaction.TransactionViewModel
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(viewModel: TransactionViewModel) {
    val transactions by viewModel.allTransactions.collectAsState()
    
    // Toggle between Category and Account view
    var breakdownType by remember { mutableStateOf("Category") }

    val filteredTxs = transactions.filter { it.type.equals("Expense", true) }
    
    val displayData = if (breakdownType == "Category") {
        filteredTxs.groupBy { it.category }.mapValues { it.value.sumOf { tx -> tx.amount } }
    } else {
        filteredTxs.groupBy { it.account }.mapValues { it.value.sumOf { tx -> tx.amount } }
    }

    val pieEntries = displayData.map {
        PieEntry(it.value.toFloat(), it.key)
    }

    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Visual Insights", fontWeight = FontWeight.Black) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Interactive Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp)
            ) {
                listOf("Category", "Account").forEach { type ->
                    val isSelected = breakdownType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) colorScheme.primary else Color.Transparent)
                            .clickable { breakdownType = type }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type,
                            color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Glassmorphic Chart Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                if (pieEntries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No spending data available", color = colorScheme.onSurfaceVariant)
                    }
                } else {
                    AndroidView(
                        factory = { context ->
                            PieChart(context).apply {
                                description.isEnabled = false
                                isDrawHoleEnabled = true
                                setHoleColor(Color.Transparent.toArgb())
                                setTransparentCircleAlpha(0)
                                holeRadius = 75f
                                centerText = "Total\nSpending"
                                setCenterTextSize(14f)
                                setCenterTextColor(colorScheme.onSurface.toArgb())
                                setEntryLabelColor(Color.Transparent.toArgb())
                                
                                legend.apply {
                                    verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                                    horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                                    orientation = Legend.LegendOrientation.HORIZONTAL
                                    setDrawInside(false)
                                    textColor = colorScheme.onSurface.toArgb()
                                    isEnabled = true
                                    yEntrySpace = 5f
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { chart ->
                            val dataSet = PieDataSet(pieEntries, "").apply {
                                colors = listOf(
                                    colorScheme.primary.toArgb(),
                                    colorScheme.secondary.toArgb(),
                                    colorScheme.tertiary.toArgb(),
                                    colorScheme.error.toArgb(),
                                    Color(0xFF673AB7).toArgb(),
                                    Color(0xFF009688).toArgb(),
                                    Color(0xFFFF9800).toArgb()
                                )
                                setDrawValues(false)
                                sliceSpace = 4f
                            }
                            chart.data = PieData(dataSet)
                            chart.centerText = "₹%,.0f".format(pieEntries.sumOf { it.value.toDouble() })
                            chart.animateY(1200, Easing.EaseInOutQuad)
                            chart.invalidate()
                        }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Detailed Breakdown",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                val sortedData = displayData.toList().sortedByDescending { it.second }
                items(sortedData) { (label, amount) ->
                    BreakdownItem(
                        label = label, 
                        amount = amount, 
                        percentage = (amount / displayData.values.sumOf { it }).toFloat(),
                        isCategory = breakdownType == "Category"
                    )
                }
            }
        }
    }
}

@Composable
fun BreakdownItem(label: String, amount: Double, percentage: Float, isCategory: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp, 
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (isCategory) {
                    val icon = when (label.lowercase()) {
                        "food" -> Icons.Default.Restaurant
                        "transport" -> Icons.Default.DirectionsCar
                        "shopping" -> Icons.Default.ShoppingBag
                        "bills" -> Icons.Default.Receipt
                        "entertainment" -> Icons.Default.Movie
                        "health" -> Icons.Default.MedicalServices
                        "salary" -> Icons.Default.Payments
                        "gift" -> Icons.Default.CardGiftcard
                        "education" -> Icons.Default.School
                        else -> Icons.Default.Category
                    }
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                } else {
                    Text(
                        text = "${(percentage * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                LinearProgressIndicator(
                    progress = { percentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Text(
                text = "₹%,.0f".format(amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
