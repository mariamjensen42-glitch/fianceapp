package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DailyTrendPoint

@Composable
fun TrendFlowChart(
    isEmpty: Boolean,
    dailyTrend: List<DailyTrendPoint>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "近7日资金流动走向",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("收入", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFE91E63)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("支出", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Render dynamic Canvas Trend Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                if (isEmpty) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "添加账单后自动生成收支走向图",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        // Draw subtle horizontal grid lines (Y-axis gridlines)
                        val gridLinesCount = 4
                        val rowStep = height / gridLinesCount
                        for (j in 1 until gridLinesCount) {
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.3f),
                                start = Offset(0f, j * rowStep),
                                end = Offset(width, j * rowStep),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        val pointCount = dailyTrend.size
                        if (pointCount >= 2) {
                            val colStep = width / (pointCount - 1)
                            
                            // Compute scaling factors safely
                            val maxVal = maxOf(
                                dailyTrend.maxOfOrNull { maxOf(it.income, it.expense) } ?: 100.0,
                                100.0
                            )

                            val incomeLinePath = Path()
                            val expenseLinePath = Path()

                            val incomeAreaPath = Path()
                            val expenseAreaPath = Path()

                            val incY0 = height - (dailyTrend[0].income / maxVal * height * 0.75).toFloat() - 10f
                            val expY0 = height - (dailyTrend[0].expense / maxVal * height * 0.75).toFloat() - 10f

                            incomeLinePath.moveTo(0f, incY0)
                            expenseLinePath.moveTo(0f, expY0)

                            incomeAreaPath.moveTo(0f, height)
                            incomeAreaPath.lineTo(0f, incY0)

                            expenseAreaPath.moveTo(0f, height)
                            expenseAreaPath.lineTo(0f, expY0)

                            for (k in 1 until pointCount) {
                                val x = k * colStep
                                val incY = height - (dailyTrend[k].income / maxVal * height * 0.75).toFloat() - 10f
                                val expY = height - (dailyTrend[k].expense / maxVal * height * 0.75).toFloat() - 10f

                                incomeLinePath.lineTo(x, incY)
                                expenseLinePath.lineTo(x, expY)

                                incomeAreaPath.lineTo(x, incY)
                                expenseAreaPath.lineTo(x, expY)
                            }

                            incomeAreaPath.lineTo(width, height)
                            incomeAreaPath.close()

                            expenseAreaPath.lineTo(width, height)
                            expenseAreaPath.close()

                            // Draw shaded gradient areas first
                            drawPath(
                                path = incomeAreaPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF4CAF50).copy(alpha = 0.15f), Color.Transparent)
                                )
                            )
                            drawPath(
                                path = expenseAreaPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFFE91E63).copy(alpha = 0.15f), Color.Transparent)
                                )
                            )

                            // Draw solid lines
                            drawPath(
                                path = incomeLinePath,
                                color = Color(0xFF4CAF50),
                                style = Stroke(width = 2.5.dp.toPx())
                            )
                            drawPath(
                                path = expenseLinePath,
                                color = Color(0xFFE91E63),
                                style = Stroke(width = 2.5.dp.toPx())
                            )

                            // Draw indicator dots
                            for (k in 0 until pointCount) {
                                val x = k * colStep
                                val incY = height - (dailyTrend[k].income / maxVal * height * 0.75).toFloat() - 10f
                                val expY = height - (dailyTrend[k].expense / maxVal * height * 0.75).toFloat() - 10f

                                if (dailyTrend[k].income > 0) {
                                    drawCircle(Color(0xFF4CAF50), radius = 3.5.dp.toPx(), center = Offset(x, incY))
                                    drawCircle(Color.White, radius = 1.5.dp.toPx(), center = Offset(x, incY))
                                }
                                if (dailyTrend[k].expense > 0) {
                                    drawCircle(Color(0xFFE91E63), radius = 3.5.dp.toPx(), center = Offset(x, expY))
                                    drawCircle(Color.White, radius = 1.5.dp.toPx(), center = Offset(x, expY))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Safe axis label row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dailyTrend.forEach { point ->
                    Text(
                        text = point.dateLabel,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
