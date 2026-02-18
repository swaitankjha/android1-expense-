package com.example.expenceflow.ui

import androidx.compose.foundation.layout.padding // ✅ Required for Modifier.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GraphScreen() {
    Text(
        text = "Graph Screen",
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.padding(16.dp)
    )
}
