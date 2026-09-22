package com.example.expenceflow.ui.home
import com.example.expenceflow.ui.theme.*

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expenceflow.data.db.Transaction
import com.example.expenceflow.ui.transaction.TransactionViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import com.example.expenceflow.utils.exportTransactionsToExcel
import com.example.expenceflow.utils.exportTransactionsToCsv
import com.example.expenceflow.ui.theme.AppThemeState
import androidx.compose.ui.graphics.SolidColor
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expenceflow.ui.viewmodel.BudgetViewModel
import com.example.expenceflow.ui.SetBudgetDialog
/* -------------------------------------------------- */
/* ---------------- DASHBOARD ----------------------- */
/* -------------------------------------------------- */

@Composable
fun DashboardScreen(
    viewModel: TransactionViewModel,
    budgetViewModel: BudgetViewModel = hiltViewModel(),
    onViewAllClick: () -> Unit,
    onPendingClick: () -> Unit = {}
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val currentAccountId by viewModel.currentAccountId.collectAsState()
    val pendingTransactions by viewModel.pendingTransactions.collectAsState()
    
    val monthlyGoal by budgetViewModel.monthlyGoal.collectAsState()
    val context = LocalContext.current
    
    // ...
    
    // ...

    val income = transactions.filter { it.type.equals("Income", true) }.sumOf { it.amount }.toFloat()
    val expense = transactions.filter { it.type.equals("Expense", true) }.sumOf { it.amount }.toFloat()
    val balance = income - expense

    val categorySpending = transactions
        .filter { it.type.equals("Expense", true) }
        .groupBy { it.category.split(" • ").first() }
        .mapValues { it.value.sumOf { tx -> tx.amount }.toFloat() }
        .toList()
        .sortedByDescending { it.second }
        .take(4)

    var showBudgetDialog by remember { mutableStateOf(false) }

    if (showBudgetDialog) {
        SetBudgetDialog(
            budgetViewModel = budgetViewModel,
            onDismiss = { showBudgetDialog = false }
        )
    }

    Scaffold(
        topBar = {
            Column {
                DashboardTopBarModern(
                    onExportExcel = {
                        exportTransactionsToExcel(context = context, transactions = transactions)
                    },
                    onExportCsv = {
                        exportTransactionsToCsv(context = context, transactions = transactions)
                    }
                )
                AccountSelector(
                    accounts = accounts,
                    currentAccountId = currentAccountId,
                    onAccountSelected = { viewModel.selectAccountId(it) }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            if (pendingTransactions.isNotEmpty()) {
                PendingIndicator(
                    count = pendingTransactions.size,
                    onClick = onPendingClick
                )
                Spacer(Modifier.height(16.dp))
            }

            ModernBalanceCard(
                balance = balance,
                income = income,
                expense = expense,
                monthlyGoal = monthlyGoal?.amount?.toFloat() ?: 0f,
                onSetBudgetClick = { showBudgetDialog = true }
            )

            Spacer(Modifier.height(24.dp))

            SectionHeader("Spending Progress")
            val budgetAmount = monthlyGoal?.amount?.toFloat() ?: 1000f
            ModernSpendingProgress(spent = expense, totalBudget = budgetAmount)

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader("Recent Activity")
                TextButton(onClick = onViewAllClick) {
                    Text("See all", style = MaterialTheme.typography.labelLarge)
                }
            }

            if (transactions.isEmpty()) {
                Text(
                    "No transactions yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                transactions.take(5).forEach {
                    ModernTransactionItem(it)
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            SectionHeader("Top Categories")
            Spacer(Modifier.height(12.dp))
            ModernTopCategories(categorySpending)

            Spacer(Modifier.height(24.dp))
            
            FinancialTipCard(income, expense)

            Spacer(Modifier.height(24.dp))
            
            ModernBudgetOverview(
                totalExpense = expense,
                monthlyBudget = budgetAmount,
                onSetBudgetClick = { showBudgetDialog = true }
            )
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun PendingIndicator(count: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.NotificationsActive, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.width(16.dp))
            Text(
                text = "$count transactions need attention",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
fun DashboardTopBarModern(
    onExportExcel: () -> Unit,
    onExportCsv: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "ExpenseFlow",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Intelligent Money Tracking",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            Box {
                FilledTonalIconButton(
                    onClick = { expanded = true },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Export CSV") },
                        leadingIcon = { Icon(Icons.Default.FileDownload, null) },
                        onClick = {
                            expanded = false
                            onExportCsv()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Export Excel (.xlsx)") },
                        leadingIcon = { Icon(Icons.Default.TableChart, null) },
                        onClick = {
                            expanded = false
                            onExportExcel()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AccountSelector(
    accounts: List<com.example.expenceflow.data.db.Account>,
    currentAccountId: Long?,
    onAccountSelected: (Long?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        item {
            val isSelected = currentAccountId == null
            Surface(
                modifier = Modifier
                    .width(100.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onAccountSelected(null) },
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "All",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        items(accounts) { account ->
            val isSelected = currentAccountId == account.id
            Surface(
                modifier = Modifier
                    .width(140.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onAccountSelected(account.id) },
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    // Balance display for account could be added here if we had the account balances
                }
            }
        }
    }
}

@Composable
fun ModernBalanceCard(
    balance: Float,
    income: Float,
    expense: Float,
    monthlyGoal: Float,
    onSetBudgetClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Decorative background circles could be added here
            
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Total Balance",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                        Text(
                            "₹%,.0f".format(balance),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        
                        // New Velocity Badge
                        val dailyBudget = if (monthlyGoal > 0) (monthlyGoal / 30) else 0f
                        val isOverSpeed = (expense / 30) > dailyBudget && dailyBudget > 0
                        
                        Surface(
                            color = if (isOverSpeed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isOverSpeed) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = if (isOverSpeed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (isOverSpeed) "Spending Fast" else "Healthy Pace",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isOverSpeed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f),
                        modifier = Modifier.size(40.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BalanceStat(
                        label = "Income",
                        amount = income,
                        icon = Icons.Default.ArrowDownward,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    VerticalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f), modifier = Modifier.height(30.dp))
                    BalanceStat(
                        label = "Expense",
                        amount = expense,
                        icon = Icons.Default.ArrowUpward,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun BalanceStat(label: String, amount: Float, icon: ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
            Text("₹%,.0f".format(amount), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun ModernSpendingProgress(spent: Float, totalBudget: Float) {
    val progress = if (totalBudget > 0) (spent / totalBudget).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape),
                color = if (progress > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Spent ₹%,.0f".format(spent),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Goal ₹%,.0f".format(totalBudget),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ModernTransactionItem(tx: Transaction) {
    val date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(tx.date))
    val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(tx.date))
    val isExpense = tx.type.equals("Expense", true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isExpense) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
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
                Text(
                    tx.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$date • $time",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
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
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
            
            Text(
                "${if (isExpense) "-" else "+"} ₹%,.0f".format(tx.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun ModernTopCategories(categorySpending: List<Pair<String, Float>>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        categorySpending.forEach { (category, amount) ->
            var isHovered by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(if (isHovered) 1.05f else 1f)
            
            Card(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .clickable { isHovered = !isHovered },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val icon = when (category.lowercase()) {
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
                    
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon, 
                            null, 
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        category, 
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "₹%,.0f".format(amount), 
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun ModernBudgetOverview(
    totalExpense: Float,
    monthlyBudget: Float,
    onSetBudgetClick: () -> Unit
) {
    val progress = if (monthlyBudget > 0) (totalExpense / monthlyBudget).coerceIn(0f, 1f) else 0f
    val calendar = Calendar.getInstance()
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
    val daysLeft = (daysInMonth - currentDay).coerceAtLeast(1)
    
    val dailyBudget = if (monthlyBudget > 0) monthlyBudget / daysInMonth else 0f
    val remainingBudget = (monthlyBudget - totalExpense).coerceAtLeast(0f)
    val suggestedDaily = remainingBudget / daysLeft

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(64.dp),
                            strokeWidth = 6.dp,
                            color = if (progress > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(Modifier.width(20.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Monthly Budget",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "₹%,.0f".format(monthlyBudget),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    
                    FilledTonalIconButton(onClick = onSetBudgetClick) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp))
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    BudgetInfoItem("Daily Limit", "₹%,.0f".format(dailyBudget))
                    BudgetInfoItem("Days Left", "$daysLeft days")
                    BudgetInfoItem("Suggested", "₹%,.0f/day".format(suggestedDaily))
                }
            }
        }
        
        if (progress > 0.8f) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "You've used ${(progress * 100).toInt()}% of your budget!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetInfoItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FinancialTipCard(income: Float, expense: Float) {
    val savingsRate = if (income > 0) ((income - expense) / income * 100).toInt() else 0
    val tip = when {
        savingsRate >= 50 -> "Amazing! You're saving more than half of your income. Consider investing."
        savingsRate >= 20 -> "Good job! You're hitting the 20% savings rule."
        savingsRate > 0 -> "You're on the right track. Try to find small ways to cut expenses."
        else -> "Your expenses are exceeding your income. Let's look at your top spending."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lightbulb, null, tint = MaterialTheme.colorScheme.tertiary)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    "Savings Rate: $savingsRate%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    tip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}
