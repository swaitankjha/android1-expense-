package com.example.expenceflow.ui
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
@Composable
fun HomeScreen() {
    Text("Home Screen", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))
}
