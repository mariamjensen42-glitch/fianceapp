package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Transaction
import com.example.ui.theme.ColorExpense
import com.example.ui.theme.ColorIncome
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TrendFlowChart(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    // Collect the last 7 days with transactions, or default last 7 calendar days
    val expenses = transactions.filter { it.type == "EXPENSE" }

    val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
    val groupedDailyExpenses = expenses
        .groupBy { sdf.format(Date(it.timestamp)) }
        .map { (dateStr, txs) -> dateStr to txs.sumOf { it.amount * it.exchangeRate } }
        .sortedBy { it.first } // Chronological order
        .takeLast(7) // Last 7 active days

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("trend_flow_chart_card"),
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
                text = "近期消费流向趋势 (Trend Line)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (groupedDailyExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无充足的每日支出走势 📉",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                val maxVal = groupedDailyExpenses.maxOfOrNull { it.second } ?: 1.0
                val adjustedMax = if (maxVal == 0.0) 100.0 else maxVal * 1.15

                val chartColor = ColorExpense // Primary red for spending flows
                val gradientBrush = Brush.verticalGradient(
                    colors = listOf(
                        chartColor.copy(alpha = 0.3f),
                        chartColor.copy(alpha = 0.0f)
                    )
                )

                // Themed Canvas Graph
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(horizontal = 8.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val spacing = width / (groupedDailyExpenses.size - 1).coerceAtLeast(1)

                    val points = groupedDailyExpenses.mapIndexed { index, (_, value) ->
                        val x = index * spacing
                        val y = height - ((value / adjustedMax) * height).toFloat()
                        x to y
                    }

                    // 1. Draw area gradient under line path
                    if (points.isNotEmpty()) {
                        val path = Path().apply {
                            moveTo(points.first().first, height)
                            points.forEach { (x, y) ->
                                lineTo(x, y)
                            }
                            lineTo(points.last().first, height)
                            close()
                        }
                        drawPath(path = path, brush = gradientBrush)
                    }

                    // 2. Draw outer bold stroke path
                    if (points.isNotEmpty()) {
                        val strokePath = Path().apply {
                            moveTo(points.first().first, points.first().second)
                            points.drop(1).forEach { (x, y) ->
                                lineTo(x, y)
                            }
                        }
                        drawPath(
                            path = strokePath,
                            color = chartColor,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    // 3. Draw circle dots on values
                    points.forEach { (x, y) ->
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(x, y)
                        )
                        drawCircle(
                            color = chartColor,
                            radius = 2.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(x, y)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Labels line under canvas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    groupedDailyExpenses.forEachIndexed { idx, (dateStr, value) ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format("¥%.0f", value),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
