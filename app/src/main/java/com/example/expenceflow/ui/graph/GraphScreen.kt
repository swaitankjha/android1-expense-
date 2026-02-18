package com.example.expenceflow.ui.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.expenceflow.data.db.Transaction
import com.example.expenceflow.ui.transaction.TransactionViewModel
import com.example.expenceflow.ui.theme.*
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*

@Composable
fun GraphScreen(viewModel: TransactionViewModel) {

    val transactions by viewModel.allTransactions.collectAsState()

    val expenseGroups = transactions
        .filter { it.type.equals("Expense", true) }
        .groupBy { it.category }

    val pieEntries = expenseGroups.map {
        PieEntry(
            it.value.sumOf { tx -> tx.amount }.toFloat(),
            it.key
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush =
                    if (AppThemeState.isDark.value)
                        SolidColor(DarkBg)
                    else
                        Brush.verticalGradient(
                            listOf(GoldLightBg, GoldAccent)
                        )
            )
            .padding(16.dp)
    ) {

        // 🔥 HEADER
        Text(
            text = "Insights",
            fontSize = 22.sp,
            color =
                if (AppThemeState.isDark.value)
                    DarkTextPrimary
                else
                    GoldTextDark
        )

        Spacer(Modifier.height(16.dp))

        // 📊 PIE CHART CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor =
                    if (AppThemeState.isDark.value)
                        DarkSurface
                    else
                        Color(0xFFFFF8E1) // soft ivory
            )
        ) {
            if (pieEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No expense data",
                        color =
                            if (AppThemeState.isDark.value)
                                DarkTextSecondary
                            else
                                Color.Gray
                    )
                }
            } else {
                AndroidView(
                    factory = { context ->
                        PieChart(context).apply {
                            data = PieData(
                                PieDataSet(pieEntries, "").apply {
                                    valueTextSize = 12f
                                    valueTextColor =
                                        if (AppThemeState.isDark.value)
                                            DarkTextSecondary.toArgb()
                                        else
                                            Color.DarkGray.toArgb()


                                    colors = listOf(
                                        GoldAccent.toArgb(),
                                        Color(0xFF4CAF50).toArgb(),
                                        Color(0xFFE53935).toArgb(),
                                        Color(0xFF42A5F5).toArgb()
                                    )
                                }
                            )
                            description.isEnabled = false
                            legend.isEnabled = true
                            invalidate()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // 🔥 BREAKDOWN TITLE
        Text(
            text = "Spending Breakdown",
            fontSize = 19.sp,
            color =
                if (AppThemeState.isDark.value)
                    DarkTextPrimary
                else
                    GoldTextDark
        )

        Spacer(Modifier.height(8.dp))

        // 📃 BREAKDOWN LIST
        LazyColumn {
            expenseGroups.forEach { (category, items) ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 7.dp),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                if (AppThemeState.isDark.value)
                                    DarkSurface
                                else
                                    Color(0xFFFFF8E1)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    category,
                                    color =
                                        if (AppThemeState.isDark.value)
                                            DarkTextPrimary
                                        else
                                            GoldTextDark
                                )
                                Text(
                                    "${items.size} transactions",
                                    fontSize = 12.sp,
                                    color =
                                        if (AppThemeState.isDark.value)
                                            DarkTextSecondary
                                        else
                                            Color.Gray
                                )
                            }
                            Text(
                                "₹ ${items.sumOf { it.amount }.toInt()}",
                                color =
                                    if (AppThemeState.isDark.value)
                                        DarkGoldAccent
                                    else
                                        GoldTextDark
                            )
                        }
                    }
                }
            }
        }
    }
}
