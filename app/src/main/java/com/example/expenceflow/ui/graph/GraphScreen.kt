package com.example.expenceflow.ui.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.expenceflow.ui.transaction.TransactionViewModel
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

@Composable
fun GraphScreen(viewModel: TransactionViewModel) {
    val transactions by viewModel.allTransactions.collectAsState()

    val expenseGroups = transactions
        .filter { it.type.equals("Expense", true) }
        .groupBy { it.category.split(" • ").first() }

    val pieEntries = expenseGroups.map {
        PieEntry(it.value.sumOf { tx -> tx.amount }.toFloat(), it.key)
    }

    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Expense Insights",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = colorScheme.onBackground
        )

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            if (pieEntries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No spending data yet", color = colorScheme.onSurfaceVariant)
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
                            setCenterTextSize(16f)
                            setCenterTextColor(colorScheme.onSurface.toArgb())
                            setEntryLabelColor(Color.Transparent.toArgb()) // Hide labels on chart
                            
                            legend.apply {
                                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                                orientation = Legend.LegendOrientation.HORIZONTAL
                                setDrawInside(false)
                                textColor = colorScheme.onSurface.toArgb()
                                isEnabled = true
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
                                Color(0xFF009688).toArgb()
                            )
                            setDrawValues(false)
                            sliceSpace = 3f
                        }
                        chart.data = PieData(dataSet)
                        chart.centerText = "₹%,.0f".format(pieEntries.sumOf { it.value.toDouble() })
                        chart.animateY(1000)
                        chart.invalidate()
                    }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Top Spending",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            expenseGroups.toList().sortedByDescending { it.second.sumOf { tx -> tx.amount } }
                .forEach { (category, items) ->
                    item {
                        CategoryItemModern(category, items.sumOf { it.amount }, items.size)
                    }
                }
        }
    }
}

@Composable
fun CategoryItemModern(category: String, totalAmount: Double, count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(8.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {}
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$count transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "₹%,.0f".format(totalAmount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
