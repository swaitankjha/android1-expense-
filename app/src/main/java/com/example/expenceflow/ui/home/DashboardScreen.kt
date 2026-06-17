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
    onExportExcel: () -> Unit
) {


    val transactions by viewModel.allTransactions.collectAsState()
    val monthlyGoal by budgetViewModel.monthlyGoal.collectAsState()
    val context = LocalContext.current

    var showBudgetDialog by remember { mutableStateOf(false) }

    if (showBudgetDialog) {
        SetBudgetDialog(
            budgetViewModel = budgetViewModel,
            onDismiss = { showBudgetDialog = false }
        )
    }

    val income = transactions.filter { it.type.equals("Income", true) }.sumOf { it.amount }.toFloat()
    val expense = transactions.filter { it.type.equals("Expense", true) }.sumOf { it.amount }.toFloat()
    val balance = income - expense

    // Calculate dynamic category spending
    val categorySpending = transactions
        .filter { it.type.equals("Expense", true) }
        .groupBy { it.category.split(" • ").first() }
        .mapValues { it.value.sumOf { tx -> tx.amount }.toFloat() }
        .toList()
        .sortedByDescending { it.second }
        .take(4)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = if (AppThemeState.isDark.value) {
                    SolidColor(Color(0xFF121212))   // 🌙 dark
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFFFF1C1),      // 💛 SAME gold
                            Color(0xFFB68D40)
                        )
                    )
                }
            )
    )
 {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {

            DashboardTopBar(
                onExportExcel = {
                    exportTransactionsToExcel(
                        context = context,
                        transactions = transactions
                    )
                }
            )


            Spacer(Modifier.height(12.dp))

            BalanceCardGold(
                balance = balance,
                income = income,
                expense = expense,
                onSetBudgetClick = { showBudgetDialog = true }
            )

            Spacer(Modifier.height(24.dp))

            val budgetAmount = monthlyGoal?.amount?.toFloat() ?: 1000f
            SavingsProgressGold(income, expense, budgetAmount)

            Spacer(Modifier.height(24.dp))

            RecentTransactionsGold(
                transactions = transactions,
                onViewAllClick = onViewAllClick
            )


            Spacer(Modifier.height(24.dp))

            TopSpendingSectionGold(categorySpending)

            Spacer(Modifier.height(24.dp))

            BudgetSectionGold(
                totalExpense = expense,
                monthlyBudget = budgetAmount,
                onSetBudgetClick = { showBudgetDialog = true }
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

/* -------------------------------------------------- */
/* ---------------- TOP BAR -------------------------- */
/* -------------------------------------------------- */

@Composable
fun DashboardTopBar(onExportExcel: () -> Unit) {

    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Dashboard",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3A2E0F)
        )

        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Export to Excel") },
                    onClick = {
                        expanded = false
                        onExportExcel()
                    }
                )
            }
        }
    }
}

/* -------------------------------------------------- */
/* ---------------- BALANCE CARD --------------------- */
/* -------------------------------------------------- */

@Composable
fun BalanceCardGold(
    balance: Float,
    income: Float,
    expense: Float,
    onSetBudgetClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSetBudgetClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B17)),
        elevation = CardDefaults.cardElevation(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total Balance", color = Color(0xFFFFD369), fontSize = 12.sp)
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Color(0xFFFFD369),
                    modifier = Modifier.size(16.dp)
                )
            }
            Text("₹ ${balance.toInt()}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)

            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MiniStatGold("Income", income, Color(0xFF4CAF50))
                MiniStatGold("Expense", expense, Color(0xFFE53935))
            }
        }
    }
}

@Composable
fun MiniStatGold(label: String, amount: Float, color: Color) {
    Column {
        Text(label, fontSize = 12.sp, color = Color(0xFFFFD369))
        Text("₹ ${amount.toInt()}", color = color, fontWeight = FontWeight.SemiBold)
    }
}

/* -------------------------------------------------- */
/* ---------------- SAVINGS -------------------------- */
/* -------------------------------------------------- */

@Composable
fun SavingsProgressGold(earned: Float, spent: Float, totalBudget: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (totalBudget > 0) (spent / totalBudget).coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = 1000)
    )

    Column {
        Text("Total Spending Progress", fontWeight = FontWeight.Bold, color = Color(0xFF3A2E0F))
        Spacer(Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = if (animatedProgress > 0.9f) Color(0xFFE53935) else Color(0xFF8D6E2F),
            trackColor = Color(0xFFFFECB3)
        )

        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Spent ₹${spent.toInt()}",
                fontSize = 12.sp,
                color = Color(0xFF3A2E0F)
            )
            Text(
                "Goal ₹${totalBudget.toInt()}",
                fontSize = 12.sp,
                color = Color(0xFF3A2E0F)
            )
        }
    }
}

/* -------------------------------------------------- */
/* ---------------- RECENT TX ------------------------ */
/* -------------------------------------------------- */

@Composable
fun RecentTransactionsGold(
    transactions: List<Transaction>,
    onViewAllClick: () -> Unit
) {

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Recent Activity",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3A2E0F)
            )

            Text(
                "View all →",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF8D6E2F),
                modifier = Modifier.clickable {
                    onViewAllClick()
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        transactions.take(3).forEach {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor =
                        if (AppThemeState.isDark.value)
                            DarkSurface
                        else
                            Color(0xFFFFF8E1) // soft ivory (light)
                )
            )
             {
                TransactionRow(it)
            }
        }
    }
}

@Composable
fun TransactionRow(tx: Transaction) {
    val date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(tx.date))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(tx.title, fontWeight = FontWeight.Medium)
            Text("$date • ${tx.category}", fontSize = 12.sp)
        }
        Text(
            "₹ ${tx.amount}",
            fontWeight = FontWeight.Bold,
            color = if (tx.type == "Expense") Color(0xFFC62828) else Color(0xFF2E7D32)
        )
    }
}

/* -------------------------------------------------- */
/* ---------------- CATEGORIES ---------------------- */
/* -------------------------------------------------- */

@Composable
fun TopSpendingSectionGold(categorySpending: List<Pair<String, Float>>) {
    Column {
        Text(
            "Top Spending Categories",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3A2E0F)
        )
        Spacer(Modifier.height(12.dp))

        if (categorySpending.isEmpty()) {
            Text("No expense data", color = Color.Gray, fontSize = 12.sp)
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                categorySpending.forEach { (category, amount) ->
                    val icon = when (category.lowercase()) {
                        "food" -> Icons.Default.Fastfood
                        "home" -> Icons.Default.Home
                        "travel", "flight" -> Icons.Default.DirectionsCar
                        "fun", "entertainment" -> Icons.Default.Movie
                        "shopping" -> Icons.Default.ShoppingBag
                        "health" -> Icons.Default.MedicalServices
                        else -> Icons.Default.Category
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1C1B17))
                            .padding(12.dp)
                            .width(60.dp)
                    ) {
                        Icon(icon, null, tint = Color(0xFFFFD369), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.height(4.dp))
                        Text(category, fontSize = 10.sp, color = Color.White, maxLines = 1)
                        Text("₹${amount.toInt()}", fontSize = 10.sp, color = Color(0xFFFFD369), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/* -------------------------------------------------- */
/* ---------------- BUDGET --------------------------- */
/* -------------------------------------------------- */

@Composable
fun BudgetSectionGold(
    totalExpense: Float,
    monthlyBudget: Float,
    onSetBudgetClick: () -> Unit
) {
    val targetProgress = if (monthlyBudget > 0) totalExpense / monthlyBudget else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1200)
    )
    val remaining = (monthlyBudget - totalExpense).coerceAtLeast(0f)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Monthly Budget Overview",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3A2E0F)
            )
            
            IconButton(onClick = onSetBudgetClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Set Budget",
                    tint = Color(0xFF8D6E2F),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clickable { onSetBudgetClick() },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B17)),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Row(
                Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(70.dp)) {
                    CircularProgressIndicator(
                        progress = animatedProgress,
                        color = if (animatedProgress > 0.9f) Color(0xFFE53935) else Color(0xFFFFD369),
                        trackColor = Color.DarkGray.copy(alpha = 0.5f),
                        strokeWidth = 8.dp,
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        "${(targetProgress * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(20.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text("Monthly Limit", color = Color(0xFFFFD369).copy(alpha = 0.7f), fontSize = 12.sp)
                    Text("₹${monthlyBudget.toInt()}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    
                    val statusText = if (remaining > 0) "₹${remaining.toInt()} remaining" else "Exceeded by ₹${(totalExpense - monthlyBudget).toInt()}"
                    val statusColor = if (remaining > 0) Color(0xFF81C784) else Color(0xFFE53935)
                    
                    Text(statusText, color = statusColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                
                Icon(
                    imageVector = if (remaining > 0) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (remaining > 0) Color(0xFF4CAF50) else Color(0xFFE53935),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

data class BudgetItem(val label: String, val progress: Float, val icon: ImageVector)
