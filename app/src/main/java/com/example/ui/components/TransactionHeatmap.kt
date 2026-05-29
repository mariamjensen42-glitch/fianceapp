package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Transaction
import java.util.*

@Composable
fun TransactionHeatmap(
    transactions: List<Transaction>,
    analysisType: String, // "EXPENSE" or "INCOME"
    modifier: Modifier = Modifier
) {
    // Generate data for the last 140 days (20 weeks)
    val numWeeks = 20
    val numDays = numWeeks * 7

    val heatmapData = remember(transactions, analysisType) {
        val calendar = Calendar.getInstance()
        // Reset time to start of day
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Ensure we end on a Saturday so that our columns align exactly to weeks (Sun-Sat).
        // Let's find out how many days until Saturday
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // Sunday is 1, Saturday is 7
        val daysToSaturday = 7 - currentDayOfWeek
        
        // This is the theoretical "end date" (the upcoming Saturday if today isn't Saturday)
        calendar.add(Calendar.DAY_OF_YEAR, daysToSaturday)

        val endTimestamp = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -numDays + 1)
        val startTimestamp = calendar.timeInMillis

        // Group by day index relative to start
        val dayAmounts = DoubleArray(numDays)

        transactions.filter { it.type == analysisType }.forEach { tx ->
            if (tx.timestamp in startTimestamp..endTimestamp) {
                // calculate index with DST safety
                val diffDays = ((tx.timestamp - startTimestamp + 12L * 3600 * 1000) / (1000 * 60 * 60 * 24)).toInt()
                if (diffDays in 0 until numDays) {
                    // For expenses, maybe ignore reimbursed ones, but standard reporting might include them.
                    dayAmounts[diffDays] += tx.amount
                }
            }
        }

        val maxAmount = dayAmounts.maxOrNull()?.takeIf { it > 0 } ?: 1.0
        
        dayAmounts.map { amount ->
            if (amount == 0.0) 0 else {
                val intensity = (amount / maxAmount)
                when {
                    intensity < 0.25 -> 1
                    intensity < 0.5 -> 2
                    intensity < 0.75 -> 3
                    else -> 4
                }
            }
        }.toIntArray()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            val title = if (analysisType == "EXPENSE") "支出活跃度 (近20周)" else "收入活跃度 (近20周)"
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Scrollable row if we want, or just fit it 
            // 20 columns * (box + spacing) = 20 * 12 = 240dp... fits perfectly on a screen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                // Day Labels (Mon, Wed, Fri)
                Column(
                    modifier = Modifier.padding(end = 8.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val days = listOf("日", "一", "二", "三", "四", "五", "六")
                    for (i in 0 until 7) {
                        Text(
                            text = if (i % 2 == 0) days[i] else "",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.height(10.dp)
                        )
                    }
                }

                // Grid 20 columns by 7 rows
                for (col in 0 until numWeeks) {
                    Column(
                        modifier = Modifier.padding(end = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (row in 0 until 7) {
                            val dayIndex = col * 7 + row
                            val intensity = if (dayIndex < heatmapData.size) heatmapData[dayIndex] else 0
                            
                            val boxColor = if (intensity == 0) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            } else {
                                // Light/Dark mapping based on theme
                                // For expenses, perhaps primary-based colors
                                // 1: light, 4: dark
                                val baseColor = if (analysisType == "EXPENSE") Color(0xFFC2185B) else Color(0xFF2E7D32)
                                val alpha = 0.2f + (0.2f * intensity)
                                baseColor.copy(alpha = alpha)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(boxColor)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("少", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(4.dp))
                for (i in 0..4) {
                    val boxColor = if (i == 0) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    } else {
                        val baseColor = if (analysisType == "EXPENSE") Color(0xFFC2185B) else Color(0xFF2E7D32)
                        val alpha = 0.2f + (0.2f * i)
                        baseColor.copy(alpha = alpha)
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(boxColor)
                            .padding(horizontal = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
                Spacer(modifier = Modifier.width(2.dp))
                Text("多", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
