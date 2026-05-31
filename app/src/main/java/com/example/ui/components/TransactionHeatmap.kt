package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Transaction
import com.example.ui.theme.ColorIncome
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TransactionHeatmap(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayStr = sdf.format(Date())

    // Map of expenditure amounts per date
    val expenses = transactions.filter { it.type == "EXPENSE" }
    val expendituresByDate = expenses.groupBy { sdf.format(Date(it.timestamp)) }
        .mapValues { (_, list) -> list.sumOf { it.amount * it.exchangeRate } }

    val daysOfWeek = listOf("周一", "周三", "周五", "周日")

    // We'll show a grid representing 6 columns (weeks) by 7 rows (days, Mon-Sun)
    // Representing ~42 days back from "Sunday" of the current week.
    val calendar = Calendar.getInstance()
    // Align calendar to the upcoming Sunday or current day of week
    val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1 = Sun, 2 = Mon ...

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("transaction_heatmap_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "消费热力图 (Ledger Spend Density)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Day Labels on left
                Column(
                    modifier = Modifier.width(36.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    daysOfWeek.forEach { day ->
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 6 Weeks x 7 Days Grid
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Generate 6 weeks (each week has 7 days)
                    // Index 0 = leftmost (oldest week), Index 5 = rightmost (this week)
                    for (weekIndex in 0 until 6) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (dayIndex in 0 until 7) {
                                // Calculate how many days to subtract relative to today
                                val totalDaysAgo = (5 - weekIndex) * 7 + (6 - dayIndex)
                                val cellCal = Calendar.getInstance().apply {
                                    add(Calendar.DAY_OF_YEAR, -totalDaysAgo)
                                }
                                val cellDateStr = sdf.format(cellCal.time)
                                val cellSpend = expendituresByDate[cellDateStr] ?: 0.0

                                // Determine color block depending on spend amount
                                val cellColor = when {
                                    cellSpend == 0.0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    cellSpend < 15.0 -> ColorIncome.copy(alpha = 0.15f)
                                    cellSpend < 60.0 -> ColorIncome.copy(alpha = 0.4f)
                                    cellSpend < 200.0 -> ColorIncome.copy(alpha = 0.7f)
                                    else -> ColorIncome // Darkest emerald for high-spend
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(cellColor)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Heatmap key/legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "极简",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                listOf(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ColorIncome.copy(alpha = 0.15f),
                    ColorIncome.copy(alpha = 0.4f),
                    ColorIncome.copy(alpha = 0.7f),
                    ColorIncome
                ).forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "频繁支出",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
