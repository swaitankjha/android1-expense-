package com.example.expenceflow.ui.alltransactions

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expenceflow.data.db.Transaction
import com.example.expenceflow.ui.transaction.TransactionViewModel
import com.example.expenceflow.utils.exportTransactionsToExcel
import com.example.expenceflow.ui.theme.*
import com.example.expenceflow.ui.EditTransactionDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTransactionsScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("All") }
    var sortByNewest by remember { mutableStateOf(true) }

    var editTx by remember { mutableStateOf<Transaction?>(null) }
    var deleteTx by remember { mutableStateOf<Transaction?>(null) }

    val filteredTransactions = transactions
        .filter {
            val matchesSearch = it.title.contains(searchQuery, ignoreCase = true) || 
                                it.category.contains(searchQuery, ignoreCase = true)
            val matchesType = when (selectedType) {
                "Income" -> it.type.equals("Income", true)
                "Expense" -> it.type.equals("Expense", true)
                else -> true
            }
            matchesSearch && matchesType
        }
        .sortedBy { if (sortByNewest) -it.date else it.date }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Transactions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { exportTransactionsToExcel(context, filteredTransactions) }) {
                        Icon(Icons.Default.FileDownload, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                placeholder = { Text("Search transactions...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Filters
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("All", "Income", "Expense").forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type) }
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { sortByNewest = !sortByNewest }) {
                    Icon(
                        imageVector = if (sortByNewest) Icons.Default.VerticalAlignBottom else Icons.Default.VerticalAlignTop,
                        contentDescription = "Sort",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // List
            if (filteredTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        ModernTransactionCard(
                            tx = tx,
                            onEdit = { editTx = tx },
                            onDelete = { deleteTx = tx }
                        )
                    }
                }
            }
        }
    }

    // Edit/Delete dialogs...
    deleteTx?.let { tx ->
        AlertDialog(
            onDismissRequest = { deleteTx = null },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete '${tx.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(tx)
                        deleteTx = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteTx = null }) { Text("Cancel") } }
        )
    }

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

@Composable
fun ModernTransactionCard(tx: Transaction, onEdit: () -> Unit, onDelete: () -> Unit) {
    val isExpense = tx.type.equals("Expense", true)
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(
                    if (isExpense) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isExpense) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(tx.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${if (isExpense) "-" else "+"}₹%,.0f".format(tx.amount),
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
