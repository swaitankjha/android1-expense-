package com.example.expenceflow.ui.alltransactions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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

import java.text.SimpleDateFormat
import java.util.*

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
    var selectedAccountFilter by remember { mutableStateOf("All") }
    var sortByNewest by remember { mutableStateOf(true) }

    var editTx by remember { mutableStateOf<Transaction?>(null) }
    var deleteTx by remember { mutableStateOf<Transaction?>(null) }

    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val isSelectionMode = selectedIds.isNotEmpty()

    val filteredTransactions = transactions
        .filter {
            val matchesSearch = it.title.contains(searchQuery, ignoreCase = true) || 
                                it.category.contains(searchQuery, ignoreCase = true) ||
                                it.account.contains(searchQuery, ignoreCase = true) ||
                                it.amount.toString().contains(searchQuery)
            val matchesType = when (selectedType) {
                "Income" -> it.type.equals("Income", true)
                "Expense" -> it.type.equals("Expense", true)
                else -> true
            }
            val matchesAccount = if (selectedAccountFilter == "All") true else it.account == selectedAccountFilter
            matchesSearch && matchesType && matchesAccount
        }
        .sortedBy { if (sortByNewest) -it.date else it.date }

    val selectedTotal = filteredTransactions.filter { it.id in selectedIds }
        .sumOf { if (it.type.equals("Expense", true)) -it.amount else it.amount }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedIds.size} Selected", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(Icons.Default.Close, null)
                        }
                    },
                    actions = {
                        IconButton(onClick = { 
                            selectedIds = filteredTransactions.map { it.id }.toSet() 
                        }) {
                            Icon(Icons.Default.SelectAll, "Select All")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            } else {
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
        },
        bottomBar = {
            if (isSelectionMode) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).padding(bottom = 32.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Amount", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = "₹%,.0f".format(selectedTotal),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = if (selectedTotal >= 0) SuccessGreen else ErrorRed
                            )
                        }
                        Button(onClick = { selectedIds = emptySet() }) {
                            Text("Clear")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // ... (rest of the code)
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
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                // Type Filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                
                // Account Filter (Secondary Filter Row)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("All", "Cash", "UPI", "Bank").forEach { acc ->
                        FilterChip(
                            selected = selectedAccountFilter == acc,
                            onClick = { selectedAccountFilter = acc },
                            label = { Text(acc) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                }
            }

            // List
            if (filteredTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val groupedTransactions = filteredTransactions.groupBy { 
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = it.date
                    val today = Calendar.getInstance()
                    val yesterday = Calendar.getInstance()
                    yesterday.add(Calendar.DAY_OF_YEAR, -1)

                    when {
                        isSameDay(calendar, today) -> "Today"
                        isSameDay(calendar, yesterday) -> "Yesterday"
                        else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it.date))
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    groupedTransactions.forEach { (dateHeader, transactionsForDate) ->
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.background,
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)
                            ) {
                                Text(
                                    text = dateHeader.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                        items(transactionsForDate, key = { it.id }) { tx ->
                            val isSelected = tx.id in selectedIds
                            ModernTransactionCard(
                                tx = tx,
                                isSelected = isSelected,
                                onLongClick = {
                                    selectedIds = if (isSelected) selectedIds - tx.id else selectedIds + tx.id
                                },
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedIds = if (isSelected) selectedIds - tx.id else selectedIds + tx.id
                                    } else {
                                        editTx = tx
                                    }
                                },
                                onEdit = { editTx = tx },
                                onDelete = { deleteTx = tx }
                            )
                        }
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

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModernTransactionCard(
    tx: Transaction,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit, 
    onDelete: () -> Unit
) {
    val isExpense = tx.type.equals("Expense", true)
    val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(tx.date))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 0.5.dp, 
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isExpense) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (tx.category.lowercase()) {
                    "food" -> Icons.Default.Restaurant
                    "transport" -> Icons.Default.DirectionsCar
                    "shopping" -> Icons.Default.ShoppingBag
                    "bills" -> Icons.Default.Receipt
                    "entertainment" -> Icons.Default.Movie
                    "health" -> Icons.Default.MedicalServices
                    "salary" -> Icons.Default.Payments
                    "gift" -> Icons.Default.CardGiftcard
                    "education" -> Icons.Default.School
                    else -> if (isExpense) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        tx.category, 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when(tx.account.lowercase()) {
                                    "cash" -> Icons.Default.Payments
                                    "upi" -> Icons.Default.QrCode
                                    "bank" -> Icons.Default.AccountBalance
                                    else -> Icons.Default.Wallet
                                },
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = tx.account.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
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
