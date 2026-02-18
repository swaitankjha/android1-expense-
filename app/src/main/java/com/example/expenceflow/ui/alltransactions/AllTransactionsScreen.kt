package com.example.expenceflow.ui.alltransactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expenceflow.data.db.Transaction
import com.example.expenceflow.ui.transaction.TransactionViewModel
import com.example.expenceflow.utils.exportTransactionsToExcel
import com.example.expenceflow.ui.theme.*

@Composable
fun AllTransactionsScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val context = LocalContext.current

    var selectedType by remember { mutableStateOf("All") }
    var sortByNewest by remember { mutableStateOf(true) }

    var editTx by remember { mutableStateOf<Transaction?>(null) }
    var deleteTx by remember { mutableStateOf<Transaction?>(null) }

    val filteredTransactions = transactions
        .filter {
            when (selectedType) {
                "Income" -> it.type.equals("Income", true)
                "Expense" -> it.type.equals("Expense", true)
                else -> true
            }
        }
        .sortedBy { if (sortByNewest) -it.date else it.date }

    Box(
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
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {

            // 🔙 TOP BAR
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        null,
                        tint = if (AppThemeState.isDark.value)
                            DarkTextSecondary
                        else
                            GoldTextDark
                    )
                }
                Text(
                    "All Transactions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color =
                        if (AppThemeState.isDark.value)
                            DarkTextPrimary
                        else
                            GoldTextDark
                )
            }

            Spacer(Modifier.height(12.dp))

            // 🔥 FILTER + EXPORT
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Row {
                    listOf("All", "Income", "Expense").forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type) },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {

                    FilterChip(
                        selected = true,
                        onClick = { sortByNewest = !sortByNewest },
                        label = {
                            Text(if (sortByNewest) "Newest ↓" else "Oldest ↑")
                        }
                    )

                    IconButton(
                        onClick = {
                            exportTransactionsToExcel(
                                context = context,
                                transactions = filteredTransactions
                            )
                        }
                    ) {
                        Icon(
                            Icons.Default.FileDownload,
                            null,
                            tint =
                                if (AppThemeState.isDark.value)
                                    DarkTextSecondary
                                else
                                    GoldTextDark
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 📃 LIST
            LazyColumn {
                items(filteredTransactions, key = { it.id }) { tx ->
                    TransactionItemWithActions(
                        transaction = tx,
                        onEdit = { editTx = tx },
                        onDelete = { deleteTx = tx }
                    )
                }
            }
        }
    }

    // ❌ DELETE CONFIRM
    deleteTx?.let { tx ->
        AlertDialog(
            onDismissRequest = { deleteTx = null },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTransaction(tx)
                    deleteTx = null
                }) {
                    Text("Delete", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTx = null }) {
                    Text("Cancel")
                }
            },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure?") }
        )
    }

    // ✏️ EDIT
    editTx?.let { tx ->
        EditTransactionDialog(
            transaction = tx,
            onSave = {
                viewModel.updateTransaction(it)
                editTx = null
            },
            onDismiss = { editTx = null }
        )
    }
}

/* ---------------- ITEM ---------------- */

@Composable
fun TransactionItemWithActions(
    transaction: Transaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
        Column(Modifier.padding(12.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        transaction.title,
                        fontWeight = FontWeight.SemiBold,
                        color =
                            if (AppThemeState.isDark.value)
                                DarkTextPrimary
                            else
                                GoldTextDark
                    )
                    Text(
                        transaction.category,
                        fontSize = 12.sp,
                        color =
                            if (AppThemeState.isDark.value)
                                DarkTextSecondary
                            else
                                Color.Gray
                    )
                }

                Text(
                    "₹${transaction.amount}",
                    fontWeight = FontWeight.Bold,
                    color =
                        if (transaction.type == "Expense")
                            ErrorRed
                        else
                            SuccessGreen
                )
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    Icons.Default.Edit,
                    null,
                    tint = DarkTextSecondary,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .clickable { onEdit() }
                )
                Icon(
                    Icons.Default.Delete,
                    null,
                    tint = ErrorRed,
                    modifier = Modifier.clickable { onDelete() }
                )
            }
        }
    }
}

/* ---------------- EDIT DIALOG ---------------- */

@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    onSave: (Transaction) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(transaction.title) }
    var amount by remember { mutableStateOf(transaction.amount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onSave(transaction.copy(title = title, amount = amount.toDouble()))
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Edit Transaction") },
        text = {
            Column {
                OutlinedTextField(title, { title = it }, label = { Text("Title") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(amount, { amount = it }, label = { Text("Amount") })
            }
        }
    )
}
