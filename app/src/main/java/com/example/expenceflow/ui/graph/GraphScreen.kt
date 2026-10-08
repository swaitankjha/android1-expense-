package com.example.expenceflow.ui.graph

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.expenceflow.data.model.ExpensePeriodFilter
import com.example.expenceflow.data.model.ExpensePeriodFilterUtil
import com.example.expenceflow.data.model.ExpensePeriodManager
import com.example.expenceflow.ui.transaction.TransactionViewModel
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(viewModel: TransactionViewModel) {
    val context = LocalContext.current
    val allTransactions by viewModel.allTransactions.collectAsState()

    var activeCyclePair by remember { mutableStateOf(ExpensePeriodManager.getActivePeriod(context)) }
    var activeCycleName by remember { mutableStateOf(ExpensePeriodManager.getActiveCycleName(context)) }

    var selectedFilter by remember { mutableStateOf<ExpensePeriodFilter>(ExpensePeriodFilter.AllTime) }
    var breakdownType by remember { mutableStateOf("Category") }

    var showCustomRangeDialog by remember { mutableStateOf(false) }
    var showMonthPickerDialog by remember { mutableStateOf(false) }
    var showSetActiveCycleDialog by remember { mutableStateOf(false) }

    val periodTransactions = remember(allTransactions, selectedFilter, activeCyclePair) {
        ExpensePeriodFilterUtil.filterTransactions(
            transactions = allTransactions,
            filter = selectedFilter,
            activeStart = activeCyclePair?.first,
            activeEnd = activeCyclePair?.second
        )
    }

    val expenseTxs = periodTransactions.filter { it.type.equals("Expense", true) }
    val totalExpense = expenseTxs.sumOf { it.amount }

    val displayData = if (breakdownType == "Category") {
        expenseTxs.groupBy { it.category }.mapValues { it.value.sumOf { tx -> tx.amount } }
    } else {
        expenseTxs.groupBy { it.account }.mapValues { it.value.sumOf { tx -> tx.amount } }
    }

    val pieEntries = displayData.map {
        PieEntry(it.value.toFloat(), it.key)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Visual Insights", fontWeight = FontWeight.Black) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            PeriodFilterBar(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
                onOpenMonthPicker = { showMonthPickerDialog = true },
                onOpenCustomRange = { showCustomRangeDialog = true }
            )

            Spacer(Modifier.height(16.dp))

            BreakdownTypeToggleCard(
                breakdownType = breakdownType,
                onTypeSelected = { breakdownType = it }
            )

            Spacer(Modifier.height(16.dp))

            GlassmorphicChartCard(
                pieEntries = pieEntries,
                totalExpense = totalExpense
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Detailed Breakdown (${periodTransactions.size} txs)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                val sortedData = displayData.toList().sortedByDescending { it.second }
                items(sortedData, key = { it.first }) { (label, amount) ->
                    BreakdownItem(
                        label = label,
                        amount = amount,
                        percentage = if (totalExpense > 0) (amount / totalExpense).toFloat() else 0f,
                        isCategory = breakdownType == "Category"
                    )
                }
            }
        }
    }

    if (showCustomRangeDialog) {
        CustomDateRangeDialog(
            onDismiss = { showCustomRangeDialog = false },
            onRangeSelected = { start, end ->
                selectedFilter = ExpensePeriodFilter.CustomRange(start, end)
                showCustomRangeDialog = false
            }
        )
    }

    if (showMonthPickerDialog) {
        MonthPickerDialog(
            onDismiss = { showMonthPickerDialog = false },
            onMonthSelected = { year, month ->
                selectedFilter = ExpensePeriodFilter.SpecificMonth(year, month)
                showMonthPickerDialog = false
            }
        )
    }

    if (showSetActiveCycleDialog) {
        SetActiveCycleDialog(
            currentStart = activeCyclePair?.first,
            currentEnd = activeCyclePair?.second,
            currentName = activeCycleName,
            onDismiss = { showSetActiveCycleDialog = false },
            onSaveCycle = { start, end, name ->
                ExpensePeriodManager.setActivePeriod(context, start, end, name)
                activeCyclePair = Pair(start, end)
                activeCycleName = name
                selectedFilter = ExpensePeriodFilter.CurrentPeriod
                showSetActiveCycleDialog = false
            }
        )
    }
}

@Composable
fun PeriodFilterBar(
    selectedFilter: ExpensePeriodFilter,
    onFilterSelected: (ExpensePeriodFilter) -> Unit,
    onOpenMonthPicker: () -> Unit,
    onOpenCustomRange: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            FilterChip(
                selected = selectedFilter is ExpensePeriodFilter.CurrentPeriod,
                onClick = { onFilterSelected(ExpensePeriodFilter.CurrentPeriod) },
                label = { Text("Current Cycle") },
                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }
        item {
            FilterChip(
                selected = selectedFilter is ExpensePeriodFilter.ThisMonth,
                onClick = { onFilterSelected(ExpensePeriodFilter.ThisMonth) },
                label = { Text("This Month") }
            )
        }
        item {
            FilterChip(
                selected = selectedFilter is ExpensePeriodFilter.SpecificMonth,
                onClick = onOpenMonthPicker,
                label = {
                    val text = (selectedFilter as? ExpensePeriodFilter.SpecificMonth)?.getLabel() ?: "Select Month"
                    Text(text)
                },
                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }
        item {
            FilterChip(
                selected = selectedFilter is ExpensePeriodFilter.CustomRange,
                onClick = onOpenCustomRange,
                label = {
                    val text = (selectedFilter as? ExpensePeriodFilter.CustomRange)?.getLabel() ?: "Custom Range"
                    Text(text)
                },
                leadingIcon = { Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }
        item {
            FilterChip(
                selected = selectedFilter is ExpensePeriodFilter.AllTime,
                onClick = { onFilterSelected(ExpensePeriodFilter.AllTime) },
                label = { Text("All Time") }
            )
        }
    }
}

@Composable
fun BreakdownTypeToggleCard(
    breakdownType: String,
    onTypeSelected: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp)
    ) {
        listOf("Category", "Account").forEach { type ->
            val isSelected = breakdownType == type
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) colorScheme.primary else Color.Transparent)
                    .clickable { onTypeSelected(type) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = type,
                    color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun GlassmorphicChartCard(
    pieEntries: List<PieEntry>,
    totalExpense: Double
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        if (pieEntries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No spending data for this period", color = colorScheme.onSurfaceVariant)
            }
        } else {
            AndroidView(
                factory = { context ->
                    PieChart(context).apply {
                        description.isEnabled = false
                        isDrawHoleEnabled = true
                        setHoleColor(Color.Transparent.toArgb())
                        setTransparentCircleAlpha(0)
                        holeRadius = 75f
                        centerText = "Total\nSpending"
                        setCenterTextSize(14f)
                        setCenterTextColor(colorScheme.onSurface.toArgb())
                        setEntryLabelColor(Color.Transparent.toArgb())

                        legend.apply {
                            verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                            orientation = Legend.LegendOrientation.HORIZONTAL
                            setDrawInside(false)
                            textColor = colorScheme.onSurface.toArgb()
                            isEnabled = true
                            yEntrySpace = 5f
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { chart ->
                    val dataSet = PieDataSet(pieEntries, "").apply {
                        colors = listOf(
                            colorScheme.primary.toArgb(),
                            colorScheme.secondary.toArgb(),
                            colorScheme.tertiary.toArgb(),
                            colorScheme.error.toArgb(),
                            Color(0xFF673AB7).toArgb(),
                            Color(0xFF009688).toArgb(),
                            Color(0xFFFF9800).toArgb()
                        )
                        setDrawValues(false)
                        sliceSpace = 4f
                    }
                    chart.data = PieData(dataSet)
                    chart.centerText = "₹%,.0f".format(totalExpense)
                    chart.animateY(1000, Easing.EaseInOutQuad)
                    chart.invalidate()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDateRangeDialog(
    onDismiss: () -> Unit,
    onRangeSelected: (Long, Long) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis ?: start
                    if (start != null) {
                        onRangeSelected(start, end ?: start)
                    }
                },
                enabled = dateRangePickerState.selectedStartDateMillis != null
            ) {
                Text("Apply Period")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = { Text("Select Custom Period", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold) },
            headline = {
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val startStr = dateRangePickerState.selectedStartDateMillis?.let { sdf.format(Date(it)) } ?: "Start"
                val endStr = dateRangePickerState.selectedEndDateMillis?.let { sdf.format(Date(it)) } ?: "End"
                Text("$startStr – $endStr", modifier = Modifier.padding(horizontal = 16.dp), fontSize = 14.sp)
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun MonthPickerDialog(
    onDismiss: () -> Unit,
    onMonthSelected: (year: Int, month: Int) -> Unit
) {
    val currentCal = Calendar.getInstance()
    var selectedYear by remember { mutableIntStateOf(currentCal.get(Calendar.YEAR)) }

    val months = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Month", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedYear-- }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Year")
                    }
                    Text("$selectedYear", fontWeight = FontWeight.Black, fontSize = 18.sp)
                    IconButton(onClick = { selectedYear++ }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Year")
                    }
                }

                val chunkedMonths = months.chunked(3)
                chunkedMonths.forEachIndexed { rowIndex, rowMonths ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowMonths.forEachIndexed { colIndex, monthName ->
                            val monthNum = rowIndex * 3 + colIndex + 1
                            OutlinedButton(
                                onClick = {
                                    onMonthSelected(selectedYear, monthNum)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(monthName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetActiveCycleDialog(
    currentStart: Long?,
    currentEnd: Long?,
    currentName: String,
    onDismiss: () -> Unit,
    onSaveCycle: (Long, Long, String) -> Unit
) {
    var cycleName by remember { mutableStateOf(currentName) }
    var startDate by remember { mutableLongStateOf(currentStart ?: System.currentTimeMillis()) }
    var endDate by remember { mutableLongStateOf(currentEnd ?: (System.currentTimeMillis() + 30L * 24 * 3600 * 1000)) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Define Active Custom Period", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Define a custom tracking period (e.g. 15 Sep – 23 Oct or 38 days). Historical transactions will remain preserved.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = cycleName,
                    onValueChange = { cycleName = it },
                    label = { Text("Cycle Name") },
                    placeholder = { Text("e.g. Oct-Nov Cycle, Trip Period") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Start Date", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(sdf.format(Date(startDate)), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("End Date", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(sdf.format(Date(endDate)), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveCycle(startDate, endDate, cycleName.ifBlank { "Custom Cycle" })
                }
            ) {
                Text("Set Active Cycle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showStartDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = startDate)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDate = state.selectedDateMillis ?: startDate
                    showStartDatePicker = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = state) }
    }

    if (showEndDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = endDate)
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDate = state.selectedDateMillis ?: endDate
                    showEndDatePicker = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = state) }
    }
}

@Composable
fun BreakdownItem(label: String, amount: Double, percentage: Float, isCategory: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (isCategory) {
                    val icon = when (label.lowercase()) {
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
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                } else {
                    Text(
                        text = "${(percentage * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                LinearProgressIndicator(
                    progress = { percentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(Modifier.width(16.dp))

            Text(
                text = "₹%,.0f".format(amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
