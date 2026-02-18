package com.example.expenceflow.ui
import androidx.compose.ui.graphics.SolidColor
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expenceflow.data.db.Transaction
import com.example.expenceflow.ui.transaction.TransactionViewModel
import com.example.expenceflow.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionScreen(
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val context = LocalContext.current

    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food") }
    var type by remember { mutableStateOf("Expense") }
    var account by remember { mutableStateOf("Cash") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush =
                    if (AppThemeState.isDark.value)
                        SolidColor(DarkBg)   // 👈 Color ko Brush bana diya
                    else
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFFFF1C1),
                                Color(0xFFB68D40)
                            )
                        )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            // 🔥 HEADER
            Text(
                text = "Add Transaction",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color =
                    if (AppThemeState.isDark.value)
                        DarkTextPrimary
                    else
                        Color.Black
            )

            Spacer(Modifier.height(16.dp))

            // 🟡 INPUT CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor =
                        if (AppThemeState.isDark.value)
                            DarkSurface
                        else
                            Color(0xFF1C1B17)
                )
            ) {
                Column(Modifier.padding(16.dp)) {

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    // 🔁 Expense / Income
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("Expense", "Income").forEach { option ->
                            FilterChip(
                                selected = type == option,
                                onClick = { type = option },
                                label = { Text(option) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor =
                                        if (option == "Expense") ErrorRed else SuccessGreen,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // 🏦 ACCOUNT
                    Text(
                        "Account",
                        fontSize = 12.sp,
                        color =
                            if (AppThemeState.isDark.value)
                                DarkTextSecondary
                            else
                                Color(0xFFFFD369)
                    )

                    Spacer(Modifier.height(4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Cash", "UPI", "Bank").forEach { acc ->
                            AssistChip(
                                onClick = { account = acc },
                                label = { Text(acc) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor =
                                        if (account == acc)
                                            GoldAccent
                                        else
                                            Color.DarkGray,
                                    labelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull()
                            if (description.isNotBlank() && amt != null && amt > 0) {
                                viewModel.addTransaction(
                                    title = description,
                                    amount = amt,
                                    type = type,
                                    category = "$category • $account",
                                    context = context
                                )
                                description = ""
                                amount = ""
                                category = "Food"
                                account = "Cash"
                                type = "Expense"
                            } else {
                                Toast.makeText(
                                    context,
                                    "Enter valid details",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldAccent
                        )
                    ) {
                        Text("Add", color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 📜 HISTORY
            Text(
                text = "Recent Transactions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color =
                    if (AppThemeState.isDark.value)
                        DarkTextPrimary
                    else
                        Color.Black
            )

            Spacer(Modifier.height(8.dp))

            if (transactions.isEmpty()) {
                Text(
                    "No transactions yet",
                    color =
                        if (AppThemeState.isDark.value)
                            DarkTextSecondary
                        else
                            Color.Gray
                )
            } else {
                LazyColumn {
                    items(transactions, key = { it.id }) { tx ->
                        TransactionItem(tx)
                    }
                }
            }
        }
    }
}

/* -------------------------------------------------- */
/* ---------------- TRANSACTION ITEM ---------------- */
/* -------------------------------------------------- */

@Composable
fun TransactionItem(transaction: Transaction) {

    val dateFormat = remember {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                if (AppThemeState.isDark.value)
                    DarkSurface
                else
                    Color(0xFFFFF8E1) // soft ivory
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    fontWeight = FontWeight.SemiBold,
                    color =
                        if (AppThemeState.isDark.value)
                            DarkTextPrimary
                        else
                            Color.Black
                )
                Text(
                    text = "${transaction.category} • ${
                        dateFormat.format(Date(transaction.date))
                    }",
                    fontSize = 12.sp,
                    color =
                        if (AppThemeState.isDark.value)
                            DarkTextSecondary
                        else
                            Color.Gray
                )
            }

            Text(
                text = "₹${transaction.amount}",
                fontWeight = FontWeight.Bold,
                color =
                    if (transaction.type == "Expense")
                        ErrorRed
                    else
                        SuccessGreen
            )
        }
    }
}
