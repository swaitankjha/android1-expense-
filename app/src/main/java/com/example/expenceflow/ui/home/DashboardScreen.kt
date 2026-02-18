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
/* -------------------------------------------------- */
/* ---------------- DASHBOARD ----------------------- */
/* -------------------------------------------------- */

@Composable
fun DashboardScreen(
    viewModel: TransactionViewModel,
    onViewAllClick: () -> Unit,
    onExportExcel: () -> Unit
) {


    val transactions by viewModel.allTransactions.collectAsState()
    val context = LocalContext.current

    val income = transactions.filter { it.type.equals("Income", true) }.sumOf { it.amount }.toFloat()
    val expense = transactions.filter { it.type.equals("Expense", true) }.sumOf { it.amount }.toFloat()
    val balance = income - expense

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

            BalanceCardGold(balance, income, expense)

            Spacer(Modifier.height(24.dp))

            SavingsProgressGold(income, expense, maxOf(income, expense, 1f))

            Spacer(Modifier.height(24.dp))

            RecentTransactionsGold(
                transactions = transactions,
                onViewAllClick = onViewAllClick
            )


            Spacer(Modifier.height(24.dp))

            TopSpendingSectionGold()

            Spacer(Modifier.height(24.dp))

            BudgetSectionGold()

            Spacer(modifier = Modifier.weight(1f))
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
fun BalanceCardGold(balance: Float, income: Float, expense: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B17)),
        elevation = CardDefaults.cardElevation(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Total Balance", color = Color(0xFFFFD369), fontSize = 12.sp)
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
    Column {
        Text("This Month", fontWeight = FontWeight.Bold, color = Color(0xFF3A2E0F))
        Spacer(Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = spent / totalBudget,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = Color(0xFF8D6E2F),
            trackColor = Color(0xFFFFECB3)
        )

        Spacer(Modifier.height(4.dp))
        Text(
            "Spent ₹${spent.toInt()} of ₹${earned.toInt()}",
            fontSize = 12.sp,
            color = Color(0xFF3A2E0F)
        )
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
fun TopSpendingSectionGold() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf(
            Icons.Default.Fastfood to "Food",
            Icons.Default.Home to "Home",
            Icons.Default.DirectionsCar to "Travel",
            Icons.Default.Movie to "Fun"
        ).forEach { (icon, label) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF1C1B17))
                    .padding(14.dp)
            ) {
                Icon(icon, null, tint = Color(0xFFFFD369))
                Text(label, fontSize = 11.sp, color = Color.White)
            }
        }
    }
}

/* -------------------------------------------------- */
/* ---------------- BUDGET --------------------------- */
/* -------------------------------------------------- */

@Composable
fun BudgetSectionGold() {
    val items = listOf(
        BudgetItem("Food", 0.6f, Icons.Default.Fastfood),
        BudgetItem("Travel", 0.4f, Icons.Default.Flight),
        BudgetItem("Entertainment", 0.8f, Icons.Default.Movie)
    )

    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(items) { item ->
            Card(
                modifier = Modifier
                    .width(200.dp)
                    .height(150.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B17))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Icon(item.icon, null, tint = Color(0xFFFFD369))
                    Spacer(Modifier.height(8.dp))
                    Text(item.label, color = Color.White)
                    LinearProgressIndicator(
                        progress = item.progress,
                        color = Color(0xFFB68D40),
                        trackColor = Color.DarkGray
                    )
                }
            }
        }
    }
}

data class BudgetItem(val label: String, val progress: Float, val icon: ImageVector)
