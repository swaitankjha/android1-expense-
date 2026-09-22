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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.expenceflow.data.db.Transaction
import com.example.expenceflow.ui.transaction.TransactionViewModel
import com.example.expenceflow.data.auto.VoiceEntrySheet
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    viewModel: TransactionViewModel = hiltViewModel(),
    navController: NavController? = null
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val context = LocalContext.current

    var showVoiceSheet by remember { mutableStateOf(false) }

    if (showVoiceSheet) {
        VoiceEntrySheet(onDismiss = { showVoiceSheet = false })
    }

    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food") }
    var type by remember { mutableStateOf("Expense") }
    
    val accounts by viewModel.accounts.collectAsState()
    var selectedAccountId by remember { mutableStateOf(1L) }
    val selectedAccount = accounts.find { it.id == selectedAccountId }
    
    var paymentMode by remember { mutableStateOf("Cash") }

    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    
    var pendingMatchToConfirm by remember { mutableStateOf<com.example.expenceflow.data.db.PendingTransaction?>(null) }

    val categories = listOf("Food", "Transport", "Shopping", "Bills", "Entertainment", "Health", "Salary", "Gift", "Education", "Other")
    val modes = listOf("Cash", "UPI", "Bank", "Card")

    if (showCategorySheet) {
        ModalBottomSheet(onDismissRequest = { showCategorySheet = false }) {
            CategoryPicker(
                categories = categories,
                selectedCategory = category,
                onCategorySelected = {
                    category = it
                    showCategorySheet = false
                }
            )
        }
    }

    if (showAccountSheet) {
        ModalBottomSheet(onDismissRequest = { showAccountSheet = false }) {
            AccountPicker(
                accounts = accounts,
                selectedAccountId = selectedAccountId,
                onAccountSelected = {
                    selectedAccountId = it
                    showAccountSheet = false
                }
            )
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

    if (pendingMatchToConfirm != null) {
        val match = pendingMatchToConfirm!!
        AlertDialog(
            onDismissRequest = { pendingMatchToConfirm = null },
            title = { Text("Match Found") },
            text = { Text("We found a similar transaction from SMS (₹${match.amount} at ${match.merchant}). Is this the same transaction?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.confirmPendingTransaction(match, description, category, selectedAccountId)
                    description = ""
                    amount = ""
                    pendingMatchToConfirm = null
                    Toast.makeText(context, "Matched and Added!", Toast.LENGTH_SHORT).show()
                }) { Text("Yes, Use Detected") }
            },
            dismissButton = {
                TextButton(onClick = {
                    val amt = amount.toDoubleOrNull() ?: match.amount
                    viewModel.addTransaction(
                        if (description.isNotBlank()) description else match.merchant,
                        amt,
                        type,
                        category,
                        selectedDate,
                        paymentMode,
                        selectedAccountId,
                        context
                    )
                    description = ""
                    amount = ""
                    pendingMatchToConfirm = null
                    Toast.makeText(context, "Added as New", Toast.LENGTH_SHORT).show()
                }) { Text("No, Create New") }
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
                    // Intelligence entry
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IntelligenceButton(icon = Icons.Default.Mic, label = "Voice", onClick = { showVoiceSheet = true })
                        IntelligenceButton(icon = Icons.Default.CameraAlt, label = "OCR", onClick = { navController?.navigate("intelligence/scan") })
                        IntelligenceButton(icon = Icons.Default.FileUpload, label = "Import", onClick = { navController?.navigate("intelligence/import") })
                    }
                }

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
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Account Selector (Independent money spaces)
                        Text("Money Space (Account)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Surface(
                            onClick = { showAccountSheet = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(12.dp))
                                    Text(selectedAccount?.name ?: "Personal", fontWeight = FontWeight.Bold)
                                }
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }

                        // Category and Date
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            DetailChip(
                                label = category,
                                icon = Icons.Default.Category,
                                modifier = Modifier.weight(1f),
                                onClick = { showCategorySheet = true }
                            )
                            DetailChip(
                                label = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(selectedDate)),
                                icon = Icons.Default.CalendarToday,
                                modifier = Modifier.weight(1f),
                                onClick = { showDatePicker = true }
                            )
                        }

                        // Payment Mode (Cash, UPI, etc.)
                        Text("Payment Mode", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            modes.forEach { mode ->
                                val isSelected = paymentMode == mode
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { paymentMode = mode },
                                    label = { Text(mode) },
                                    modifier = Modifier.weight(1f),
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull()
                            if (description.isNotBlank() && amt != null && amt > 0) {
                                val match = viewModel.findMatchingPending(amt, type)
                                if (match != null) {
                                    pendingMatchToConfirm = match
                                } else {
                                    viewModel.addTransaction(
                                        title = description,
                                        amount = amt,
                                        type = type,
                                        category = category,
                                        date = selectedDate,
                                        account = paymentMode,
                                        accountId = selectedAccountId,
                                        context = context
                                    )
                                    description = ""
                                    amount = ""
                                    Toast.makeText(context, "Added!", Toast.LENGTH_SHORT).show()
                                }
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
                    Text("Recent Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                }

                val groupedTransactions = transactions.take(15).groupBy { 
                    val calendar = Calendar.getInstance().apply { timeInMillis = it.date }
                    val today = Calendar.getInstance()
                    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

                    when {
                        isSameDay(calendar, today) -> "Today"
                        isSameDay(calendar, yesterday) -> "Yesterday"
                        else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it.date))
                    }
                }

                groupedTransactions.forEach { (dateHeader, transactionsForDate) ->
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp)
                        ) {
                            Text(
                                text = dateHeader.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        }
                    }
                    items(transactionsForDate, key = { it.id }) { tx ->
                        ModernTransactionRow(
                            tx = tx,
                            onEdit = { editingTransaction = tx },
                            onDelete = { viewModel.deleteTransaction(tx) }
                        )
                    }
                }
                
                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

@Composable
fun CategoryPicker(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
        Text("Select Category", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { cat ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onCategorySelected(cat) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedCategory == cat) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        val icon = when (cat.lowercase()) {
                            "food" -> Icons.Default.Restaurant
                            "transport" -> Icons.Default.DirectionsCar
                            "shopping" -> Icons.Default.ShoppingBag
                            "bills" -> Icons.Default.Receipt
                            "entertainment" -> Icons.Default.Movie
                            "health" -> Icons.Default.MedicalServices
                            "salary" -> Icons.Default.Payments
                            "gift" -> Icons.Default.CardGiftcard
                            "education" -> Icons.Default.School
                            else -> Icons.Default.Category
                        }
                        Icon(imageVector = icon, contentDescription = null, tint = if (selectedCategory == cat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(16.dp))
                        Text(cat, style = MaterialTheme.typography.bodyLarge, fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

@Composable
fun AccountPicker(
    accounts: List<com.example.expenceflow.data.db.Account>,
    selectedAccountId: Long,
    onAccountSelected: (Long) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
        Text("Select Account (Money Space)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(accounts) { account ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onAccountSelected(account.id) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedAccountId == account.id) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = null, tint = if (selectedAccountId == account.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(16.dp))
                        Text(account.name, style = MaterialTheme.typography.bodyLarge, fontWeight = if (selectedAccountId == account.id) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

@Composable
fun IntelligenceButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(56.dp)) {
            Icon(icon, contentDescription = label)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
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
    val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(tx.date))
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp, 
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        tx.category, 
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        time,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
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
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
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
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Delete, 
                        null, 
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.4f), 
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
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
