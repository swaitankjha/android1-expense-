package com.example.expenceflow.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SavingsProgressRing(expenses: Int, goal: Int) {
    // Calculate raw ratio to determine color
    val ratio = if (goal == 0) 0f else expenses.toFloat() / goal

    // Clamp progress between 0.0 and 1.0 for the UI component
    val progress = ratio.coerceIn(0f, 1f)

    // Determine color: Red if over budget (ratio > 1.0), otherwise Primary
    val progressColor = if (ratio > 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Monthly Spending", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Box(contentAlignment = Alignment.Center) {
            // Background ring (optional, makes it look better)
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.size(150.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 8.dp,
            )

            // Foreground progress ring
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(150.dp),
                color = progressColor,
                strokeWidth = 8.dp,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "₹$expenses / ₹$goal",
            style = MaterialTheme.typography.titleLarge,
            color = progressColor // Text also turns red if over budget
        )
    }
}
