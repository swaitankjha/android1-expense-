package com.example.expenceflow.data.auto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expenceflow.data.db.Account

data class SelectedImport(
    val uri: Uri,
    val type: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportCenterScreen(
    onBack: () -> Unit,
    viewModel: ImportViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isImporting by viewModel.isImporting.collectAsState()
    val summary by viewModel.importSummary.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    var showPermissionSettingsDialog by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<SelectedImport?>(null) }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.scanSmsInbox(context)
        } else {
            showPermissionSettingsDialog = true
        }
    }

    fun startSmsScanFlow() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.scanSmsInbox(context)
        } else {
            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
        }
    }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { pendingImport = SelectedImport(it, "CSV") }
    }

    val excelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { pendingImport = SelectedImport(it, "EXCEL") }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { pendingImport = SelectedImport(it, "PDF") }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Center", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Bulk Import Statements",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                "Import bank statements or scan SMS to add transactions directly to your preferred account.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(32.dp))

            if (isImporting) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Adding transactions to your account...")
            } else {
                ImportOptionCard(
                    title = "Scan SMS Inbox",
                    description = "Auto-detect bank transactions from your recent SMS.",
                    icon = Icons.Default.MarkEmailUnread,
                    onClick = { startSmsScanFlow() }
                )

                Spacer(Modifier.height(16.dp))

                ImportOptionCard(
                    title = "CSV Statement",
                    description = "Import standard Excel/CSV exports.",
                    icon = Icons.Default.TableChart,
                    onClick = {
                        csvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "application/vnd.ms-excel", "text/plain", "*/*"))
                    }
                )

                Spacer(Modifier.height(16.dp))

                ImportOptionCard(
                    title = "Excel Statement",
                    description = "Import .xlsx or .xls files.",
                    icon = Icons.Default.TableChart,
                    onClick = {
                        excelLauncher.launch(arrayOf(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "application/vnd.ms-excel",
                            "application/octet-stream"
                        ))
                    }
                )

                Spacer(Modifier.height(16.dp))

                ImportOptionCard(
                    title = "PDF Statement",
                    description = "Import HDFC, SBI, ICICI PDFs.",
                    icon = Icons.Default.Description,
                    onClick = {
                        pdfLauncher.launch(arrayOf("application/pdf"))
                    }
                )

                summary?.let {
                    Spacer(Modifier.height(24.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = it,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    if (pendingImport != null) {
        val fileImport = pendingImport!!
        ImportDestinationDialog(
            accounts = accounts,
            fileType = fileImport.type,
            onDismiss = { pendingImport = null },
            onCreateAccount = { newAccountName, onCreated ->
                viewModel.createAccountAndGet(newAccountName, onCreated)
            },
            onConfirmImport = { selectedAccount ->
                viewModel.importFileToAccount(context, fileImport.uri, fileImport.type, selectedAccount)
                pendingImport = null
            }
        )
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
}

@Composable
fun ImportDestinationDialog(
    accounts: List<Account>,
    fileType: String,
    onDismiss: () -> Unit,
    onCreateAccount: (String, (Account) -> Unit) -> Unit,
    onConfirmImport: (Account) -> Unit
) {
    val displayAccounts = if (accounts.isEmpty()) listOf(Account(id = 1L, name = "Personal")) else accounts
    var selectedAccount by remember { mutableStateOf(displayAccounts.first()) }
    var showNewAccountInput by remember { mutableStateOf(false) }
    var newAccountNameInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Select Destination Account", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Where would you like to add all transactions extracted from this $fileType file?",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Select Account / Money Space:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(displayAccounts) { account ->
                        val isSelected = selectedAccount.id == account.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedAccount = account },
                            label = { Text(account.name, fontWeight = FontWeight.Bold) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                if (!showNewAccountInput) {
                    TextButton(
                        onClick = { showNewAccountInput = true },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Create Custom Account for this File")
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("New Account Name", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = newAccountNameInput,
                            onValueChange = { newAccountNameInput = it },
                            placeholder = { Text("e.g. SBI Card, Business, Office") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showNewAccountInput = false }) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    if (newAccountNameInput.isNotBlank()) {
                                        onCreateAccount(newAccountNameInput) { createdAccount ->
                                            selectedAccount = createdAccount
                                            showNewAccountInput = false
                                            newAccountNameInput = ""
                                        }
                                    }
                                }
                            ) {
                                Text("Add & Select")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmImport(selectedAccount) }
            ) {
                Text("Add All to '${selectedAccount.name}'")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ImportOptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
