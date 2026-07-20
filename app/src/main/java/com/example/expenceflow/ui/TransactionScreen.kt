package com.example.expenceflow.ui

import android.widget.Toast
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expenceflow.data.db.Transaction
import com.example.expenceflow.ui.transaction.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
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
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }

    val categories = listOf("Food", "Transport", "Shopping", "Bills", "Entertainment", "Health", "Salary", "Gift", "Education", "Other")

    if (showCategorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showCategorySheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Select Category",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    category = cat
                                    showCategorySheet = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (category == cat) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = if (category == cat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    cat,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (category == cat) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    showDatePicker = false
                }) { Text("Confirm") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    if (editingTransaction != null) {
        EditTransactionDialog(
            transaction = editingTransaction!!,
            onDismiss = { editingTransaction = null },
            onSave = { updatedTx ->
                viewModel.updateTransaction(updatedTx)
                editingTransaction = null
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Add Transaction", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    // Type Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp)
                    ) {
                        listOf("Expense", "Income").forEach { option ->
                            val isSelected = type == option
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) {
                                            if (option == "Expense") MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.primary
                                        } else Color.Transparent
                                    )
                                    .clickable { type = option }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = option,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            TextField(
                                value = amount,
                                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amount = it },
                                placeholder = { Text("0.00", style = MaterialTheme.typography.headlineMedium) },
                                prefix = { Text("₹ ", style = MaterialTheme.typography.headlineMedium) },
                                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            TextField(
                                value = description,
                                onValueChange = { description = it },
                                placeholder = { Text("What was this for?") },
                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        DetailChip(
                            label = category,
                            icon = Icons.Default.Category,
                            modifier = Modifier.weight(1f),
                            onClick = { showCategorySheet = true }
                        )
                        DetailChip(
                            label = account,
                            icon = Icons.Default.AccountBalanceWallet,
                            modifier = Modifier.weight(1f),
                            onClick = { 
                                account = when(account) {
                                    "Cash" -> "UPI"
                                    "UPI" -> "Bank"
                                    "Bank" -> "Savings"
                                    "Savings" -> "Credit Card"
                                    else -> "Cash"
                                }
                            }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    DetailChip(
                        label = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault()).format(Date(selectedDate)),
                        icon = Icons.Default.CalendarToday,
                        onClick = { showDatePicker = true }
                    )
                }

                item {
                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull()
                            if (description.isNotBlank() && amt != null && amt > 0) {
                                viewModel.addTransaction(
                                    title = description,
                                    amount = amt,
                                    type = type,
                                    category = category,
                                    date = selectedDate,
                                    account = account,
                                    context = context
                                )
                                description = ""
                                amount = ""
                                Toast.makeText(context, "Added!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Please enter amount", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save Transaction", fontWeight = FontWeight.Bold)
                    }
                }

                item {
                    Text("Recent Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                items(transactions.take(10), key = { it.id }) { tx ->
                    ModernTransactionRow(
                        tx = tx,
                        onEdit = { editingTransaction = tx },
                        onDelete = { viewModel.deleteTransaction(tx) }
                    )
                }
                
                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
fun DetailChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
    }
}

@Composable
fun ModernTransactionRow(tx: Transaction, onEdit: () -> Unit, onDelete: () -> Unit) {
    val isExpense = tx.type.equals("Expense", true)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(
                if (isExpense) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ).clickable { onEdit() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isExpense) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f).clickable { onEdit() }) {
            Text(tx.title, fontWeight = FontWeight.SemiBold)
            Text(tx.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            "${if (isExpense) "-" else "+"}₹%,.0f".format(tx.amount),
            fontWeight = FontWeight.ExtraBold,
            color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(transaction: Transaction, onDismiss: () -> Unit, onSave: (Transaction) -> Unit) {
    var title by remember { mutableStateOf(transaction.title) }
    var amount by remember { mutableStateOf(transaction.amount.toString()) }
    var date by remember { mutableStateOf(transaction.date) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    date = datePickerState.selectedDateMillis ?: date
                    showDatePicker = false
                }) { Text("Confirm") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal))
                Surface(onClick = { showDatePicker = true }, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date(date)))
                        Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(20.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amount.toDoubleOrNull() ?: transaction.amount
                onSave(transaction.copy(title = title, amount = amt, date = date))
            }) { Text("Update") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
