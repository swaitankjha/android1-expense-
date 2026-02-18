package com.example.expenceflow.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
// Ensure this import matches where your BudgetViewModel is actually located
// Based on previous steps, it is likely in: com.example.expenceflow.ui.viewmodel
import com.example.expenceflow.ui.viewmodel.BudgetViewModel

@Composable
fun SetBudgetDialog(
    budgetViewModel: BudgetViewModel,
    onDismiss: () -> Unit
) {
    // Remember state for the input field
    var input by remember { mutableStateOf("") }

    Dialog(onDismissRequest = { onDismiss() }) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 8.dp,
            // Add a background color to ensure it stands out on all themes
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.wrapContentSize()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Set Monthly Budget",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = input,
                    onValueChange = { newValue ->
                        // Optional: Ensure only numbers are entered to prevent crashes/weird input
                        if (newValue.all { it.isDigit() }) {
                            input = newValue
                        }
                    },
                    label = { Text("Enter amount in ₹") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amount = input.toIntOrNull()
                            if (amount != null && amount > 0) {
                                budgetViewModel.saveBudgetGoal(amount)
                                onDismiss()
                            }
                        },
                        // Disable save if input is empty or invalid
                        enabled = input.isNotBlank() && input.toIntOrNull() != null
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
