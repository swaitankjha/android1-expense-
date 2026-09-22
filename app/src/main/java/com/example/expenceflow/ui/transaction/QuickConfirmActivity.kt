package com.example.expenceflow.ui.transaction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.expenceflow.data.db.Account
import com.example.expenceflow.data.db.PendingTransaction
import com.example.expenceflow.ui.theme.ExpenceFlowTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel

@AndroidEntryPoint
class QuickConfirmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pendingId = intent.getLongExtra("pending_id", -1L)
        val amount = intent.getDoubleExtra("amount", 0.0)
        val merchant = intent.getStringExtra("merchant") ?: "Unknown"
        val type = intent.getStringExtra("type") ?: "Expense"
        val category = intent.getStringExtra("category") ?: "Other"

        if (pendingId == -1L && amount <= 0.0) {
            finish()
            return
        }

        setContent {
            ExpenceFlowTheme {
                QuickConfirmDialog(
                    pendingId = pendingId,
                    fallbackAmount = amount,
                    fallbackMerchant = merchant,
                    fallbackType = type,
                    fallbackCategory = category,
                    onDismiss = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickConfirmDialog(
    pendingId: Long,
    fallbackAmount: Double = 0.0,
    fallbackMerchant: String = "Unknown",
    fallbackType: String = "Expense",
    fallbackCategory: String = "Other",
    onDismiss: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val pendingTransactions by viewModel.pendingTransactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val dbPending = pendingTransactions.find { it.id == pendingId }

    val pending = dbPending ?: PendingTransaction(
        id = pendingId,
        amount = fallbackAmount,
        merchant = fallbackMerchant,
        type = fallbackType,
        category = fallbackCategory,
        date = System.currentTimeMillis(),
        source = "Notification"
    )

    var title by remember(pending.merchant) { mutableStateOf(pending.merchant) }
    var category by remember(pending.category) { mutableStateOf(pending.category) }
    var type by remember(pending.type) { mutableStateOf(pending.type) }
    var accountId by remember { mutableLongStateOf(1L) }

    val displayAccounts = if (accounts.isEmpty()) listOf(Account(id = 1L, name = "Personal")) else accounts
    val safeAccountIndex = displayAccounts.indexOfFirst { it.id == accountId }.coerceIn(0, displayAccounts.size - 1)

    val categories = listOf("Food", "Transport", "Shopping", "Bills", "Entertainment", "Health", "Salary", "Gift", "Education", "Other")
    val safeCategoryIndex = categories.indexOf(category).coerceIn(0, categories.size - 1)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Confirm Transaction",
                    style = MaterialTheme.typography.headlineSmall
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    listOf("Expense", "Income").forEach { option ->
                        val isSelected = type == option
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            selected = isSelected,
                            onClick = { type = option },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) {
                                if (option == "Expense") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            } else Color.Transparent
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = option,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Amount: ₹${pending.amount}",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (type == "Income") Color(0xFF2E7D32) else Color(0xFFC62828)
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Category", style = MaterialTheme.typography.labelMedium)
                ScrollableTabRow(
                    selectedTabIndex = safeCategoryIndex,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    categories.forEach { cat ->
                        Tab(
                            selected = category == cat,
                            onClick = { category = cat },
                            text = { Text(cat) }
                        )
                    }
                }

                Text("Account", style = MaterialTheme.typography.labelMedium)
                ScrollableTabRow(
                    selectedTabIndex = safeAccountIndex,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    displayAccounts.forEach { account ->
                        Tab(
                            selected = accountId == account.id,
                            onClick = { accountId = account.id },
                            text = { Text(account.name) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.confirmPendingTransaction(
                                pending.copy(type = type),
                                title,
                                category,
                                accountId
                            )
                            onDismiss()
                        }
                    ) {
                        Text("Save Transaction")
                    }
                }
            }
        }
    }
}
