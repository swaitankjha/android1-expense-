package com.example.expenceflow.ui.transaction

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expenceflow.data.db.Account
import com.example.expenceflow.data.db.PendingTransaction
import com.example.expenceflow.ui.notification.NotificationListenerHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingReviewScreen(
    onBack: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val pendingTransactions by viewModel.pendingTransactions.collectAsState()
    val isScanningSms by viewModel.isScanningSms.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val context = LocalContext.current

    var selectedPending by remember { mutableStateOf<PendingTransaction?>(null) }
    var showPermissionSettingsDialog by remember { mutableStateOf(false) }

    val isNotificationAccessGranted = remember {
        mutableStateOf(NotificationListenerHelper.isNotificationListenerEnabled(context))
    }

    fun triggerSmsScan() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.scanSmsInbox(context) { count ->
                Toast.makeText(
                    context,
                    if (count > 0) "Found $count new transaction(s)!" else "No new transactions found in SMS inbox",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            showPermissionSettingsDialog = true
        }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            triggerSmsScan()
        } else {
            showPermissionSettingsDialog = true
        }
    }

    fun startSmsScanFlow() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            triggerSmsScan()
        } else {
            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pending Review", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { startSmsScanFlow() },
                        enabled = !isScanningSms
                    ) {
                        if (isScanningSms) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.MarkEmailUnread, contentDescription = "Scan SMS Inbox")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (!isNotificationAccessGranted.value) {
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            NotificationListenerHelper.openNotificationListenerSettings(context)
                            isNotificationAccessGranted.value = NotificationListenerHelper.isNotificationListenerEnabled(context)
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notification Access Disabled",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Tap to enable Notification Access for real-time Google Pay, PhonePe & Paytm detection.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (pendingTransactions.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailUnread,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No Pending Transactions",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Automatic message detection runs in background when bank SMS messages or payment notifications arrive. You can also scan your SMS inbox anytime below.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { startSmsScanFlow() },
                        enabled = !isScanningSms
                    ) {
                        if (isScanningSms) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("Scanning SMS Inbox...")
                        } else {
                            Icon(Icons.Default.MarkEmailUnread, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Scan Recent SMS Inbox")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pendingTransactions) { pending ->
                        PendingItem(
                            pending = pending,
                            onAdd = { selectedPending = pending },
                            onDismiss = { viewModel.dismissPendingTransaction(pending.id) }
                        )
                    }
                }
            }
        }
    }

    if (showPermissionSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionSettingsDialog = false },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            title = { Text("SMS Permission Required") },
            text = { Text("ExpenseFlow needs SMS permission to detect bank transaction messages. Please allow SMS permission in App Settings.") },
            confirmButton = {
                Button(onClick = {
                    showPermissionSettingsDialog = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionSettingsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (selectedPending != null) {
        val pending = selectedPending!!
        var title by remember { mutableStateOf(pending.merchant) }
        var category by remember { mutableStateOf(pending.category) }
        var type by remember { mutableStateOf(pending.type) }
        var accountId by remember { mutableStateOf(1L) }

        val displayAccounts = if (accounts.isEmpty()) listOf(Account(id = 1L, name = "Personal")) else accounts
        val safeSelectedIndex = displayAccounts.indexOfFirst { it.id == accountId }.coerceIn(0, displayAccounts.size - 1)

        AlertDialog(
            onDismissRequest = { selectedPending = null },
            title = { Text("Add Transaction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp)
                    ) {
                        listOf("Expense", "Income").forEach { option ->
                            val isSelected = type == option
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) {
                                            if (option == "Expense") MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.primary
                                        } else Color.Transparent
                                    )
                                    .clickable { type = option }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = option,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Text("Amount: ₹${pending.amount}", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title/Merchant") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Select Money Space", style = MaterialTheme.typography.labelSmall)
                    ScrollableTabRow(
                        selectedTabIndex = safeSelectedIndex,
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
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.confirmPendingTransaction(
                        pending = pending.copy(type = type),
                        title = title,
                        category = category,
                        accountId = accountId
                    )
                    selectedPending = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPending = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PendingItem(
    pending: PendingTransaction,
    onAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${if (pending.type == "Income") "💰" else "💸"} ₹${pending.amount}",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = if (pending.type == "Income") Color(0xFF2E7D32) else Color(0xFFC62828)
                )
                Text(pending.merchant, fontWeight = FontWeight.Bold)
                Text(
                    text = "Detected via ${pending.source} • ${
                        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(
                            Date(pending.date)
                        )}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.error)
            }
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Check, contentDescription = "Add", tint = Color(0xFF2E7D32))
            }
        }
    }
}
