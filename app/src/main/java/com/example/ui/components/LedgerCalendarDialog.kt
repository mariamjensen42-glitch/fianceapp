package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LedgerCalendarDialog(
    transactions: List<Transaction>,
    onDismiss: () -> Unit,
    onDeleteTransaction: (Int) -> Unit,
    totalBudget: Double? = null
) {
    // Current year and month selected in calendar view
    val systemCalendar = remember { Calendar.getInstance() }
    var selectedYear by remember { mutableStateOf(systemCalendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(systemCalendar.get(Calendar.MONTH)) } // 0-Indexed

    // Chosen day of the currently displayed month
    var selectedDay by remember { mutableStateOf(systemCalendar.get(Calendar.DAY_OF_MONTH)) }

    // Update selected date to 1st of month when month changes if previous selectedDay is higher than month max
    LaunchedEffect(selectedYear, selectedMonth) {
        val tempCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
        }
        val maxDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        if (selectedDay > maxDays) {
            selectedDay = maxDays
        }
    }

    // Process month grid values
    val calendarGridData = remember(selectedYear, selectedMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Sunday = 1, Monday = 2...

        // Start week on Monday
        val firstDayOffset = when (firstDayOfWeek) {
            Calendar.SUNDAY -> 6
            else -> firstDayOfWeek - 2
        }

        List(firstDayOffset) { null } + (1..daysInMonth).toList()
    }

    // Index all transactions for the selected month to map to days rapidly
    val dailyTransactions = remember(transactions, selectedYear, selectedMonth) {
        val groups = mutableMapOf<Int, MutableList<Transaction>>()
        val tempCal = Calendar.getInstance()

        transactions.forEach { tx ->
            tempCal.timeInMillis = tx.timestamp
            val txYear = tempCal.get(Calendar.YEAR)
            val txMonth = tempCal.get(Calendar.MONTH)
            val txDay = tempCal.get(Calendar.DAY_OF_MONTH)

            if (txYear == selectedYear && txMonth == selectedMonth) {
                groups.getOrPut(txDay) { mutableListOf() }.add(tx)
            }
        }
        groups
    }

    // Summary scores for the displayed month
    val monthStats = remember(dailyTransactions) {
        var totalExp = 0.0
        var totalInc = 0.0
        dailyTransactions.forEach { (_, txList) ->
            txList.forEach { tx ->
                if (tx.type == "EXPENSE") {
                    totalExp += tx.amount
                } else if (tx.type == "INCOME") {
                    totalInc += tx.amount
                }
            }
        }
        Pair(totalExp, totalInc)
    }

    val monthLabel = remember(selectedYear, selectedMonth) {
        "${selectedYear}年 ${String.format("%02d", selectedMonth + 1)}月"
    }

    val todayCalendar = remember { Calendar.getInstance() }
    val todayYear = todayCalendar.get(Calendar.YEAR)
    val todayMonth = todayCalendar.get(Calendar.MONTH)
    val todayDay = todayCalendar.get(Calendar.DAY_OF_MONTH)

    val dailySums = remember(dailyTransactions) {
        val sums = mutableMapOf<Int, Pair<Double, Double>>() // day -> Pair(expense, income)
        dailyTransactions.forEach { (day, txList) ->
            val expense = txList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val income = txList.filter { it.type == "INCOME" }.sumOf { it.amount }
            sums[day] = Pair(expense, income)
        }
        sums
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .testTag("calendar_dialog_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Diagonal header close bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "记账日历看板",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("calendar_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "关闭日历")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Year / Month pick switches
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (selectedMonth == 0) {
                                selectedMonth = 11
                                selectedYear -= 1
                            } else {
                                selectedMonth -= 1
                            }
                        },
                        modifier = Modifier.testTag("calendar_prev_month")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上个月")
                    }

                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IconButton(
                        onClick = {
                            if (selectedMonth == 11) {
                                selectedMonth = 0
                                selectedYear += 1
                            } else {
                                selectedMonth += 1
                            }
                        },
                        modifier = Modifier.testTag("calendar_next_month")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下个月")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Monthly sums
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("本月总支出", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("¥ ${String.format("%.1f", monthStats.first)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9).copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("本月总收入", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("¥ ${String.format("%.1f", monthStats.second)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                if (totalBudget != null && totalBudget > 0) {
                    val spent = monthStats.first
                    val percentage = (spent / totalBudget * 100).toInt()
                    val progressFraction = (spent / totalBudget).toFloat().coerceIn(0f, 1f)
                    val isNearLimit = spent >= totalBudget * 0.8
                    val isOverspent = spent > totalBudget
                    
                    val progressColor = if (isOverspent) {
                        Color(0xFFD32F2F) // Exceeded: Dark Red
                    } else if (isNearLimit) {
                        Color(0xFFFFA000) // Near limit: Amber
                    } else {
                        MaterialTheme.colorScheme.primary // Normal
                    }
                    val containerColor = if (isOverspent) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    } else if (isNearLimit) {
                        Color(0xFFFFF8E1) // Soft warning amber
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).testTag("calendar_budget_progress_card"),
                        colors = CardDefaults.cardColors(containerColor = containerColor)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isOverspent) "⚠️ 本月已超支！" else if (isNearLimit) "⚠️ 消费即将达到80%预警！" else "📊 本月度预算进度",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isOverspent) Color(0xFFD32F2F) else if (isNearLimit) Color(0xFFC43E00) else MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "已用 $percentage% (¥${String.format("%.1f", spent)} / ¥${String.format("%.0f", totalBudget)})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = progressColor,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Days of week header row
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    val weekdays = listOf("一", "二", "三", "四", "五", "六", "日")
                    weekdays.forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // Calendar days block grid (using column with nested rows)
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val rows = calendarGridData.chunked(7)
                    rows.forEach { weekCells ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            weekCells.forEach { dayNum ->
                                if (dayNum == null) {
                                    Box(modifier = Modifier.weight(1f))
                                } else {
                                    val isSelected = (dayNum == selectedDay)
                                    val isToday = (dayNum == todayDay) && (selectedMonth == todayMonth) && (selectedYear == todayYear)

                                    // Extract pre-calculated sums
                                    val cellSums = dailySums[dayNum] ?: Pair(0.0, 0.0)
                                    val dayExpense = cellSums.first
                                    val dayIncome = cellSums.second

                                    val cellBorder = if (isToday) {
                                        Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                    } else {
                                        Modifier
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                else Color.Transparent
                                            )
                                            .then(cellBorder)
                                            .clickable { selectedDay = dayNum }
                                            .testTag("calendar_day_$dayNum"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Text(
                                                text = dayNum.toString(),
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = if (isSelected || isToday) FontWeight.Black else FontWeight.Normal,
                                                    fontSize = 12.sp
                                                ),
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                                else if (isToday) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurface
                                            )

                                            // Draw transaction cost or dot indicator
                                            if (dayExpense > 0) {
                                                Text(
                                                    text = "-${String.format("%.0f", dayExpense)}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = Color(0xFFC2185B),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            } else if (dayIncome > 0) {
                                                Text(
                                                    text = "+${String.format("%.0f", dayIncome)}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = Color(0xFF2E7D32),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                // Detail transactions list of targeted day
                val selectedDayTransactions = dailyTransactions[selectedDay] ?: emptyList()

                Text(
                    text = "🗓️ ${selectedMonth + 1}月${selectedDay}日 账单明细",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                if (selectedDayTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "这一天是空白的噢，没有记账记录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedDayTransactions, key = { it.id }) { item ->
                            val category = CategoryRegistry.getCategoryById(item.category)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(category?.color?.copy(alpha = 0.2f) ?: Color.Gray.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = category?.icon ?: Icons.Default.Info,
                                            contentDescription = null,
                                            tint = category?.color ?: Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = category?.name ?: "未分类",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }

                                    val amountText = if (item.type == "EXPENSE") {
                                        "- ¥ ${String.format("%.2f", item.amount)}"
                                    } else {
                                        "+ ¥ ${String.format("%.2f", item.amount)}"
                                    }
                                    val amountColor = if (item.type == "EXPENSE") Color(0xFFC2185B) else Color(0xFF2E7D32)

                                    Text(
                                        text = amountText,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = amountColor
                                    )

                                    Spacer(modifier = Modifier.width(6.dp))

                                    IconButton(
                                        onClick = { onDeleteTransaction(item.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "删除该账目",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
