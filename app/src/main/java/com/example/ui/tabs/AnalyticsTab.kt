package com.example.ui.tabs

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Transaction
import com.example.ui.CategoryBreakdown
import com.example.ui.DailyTrendPoint
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.TrendFlowChart
import com.example.ui.components.TransactionHeatmap

@Composable
fun AnalyticsTab(
    transactions: List<Transaction>,
    expenseBreakdown: List<CategoryBreakdown>,
    incomeBreakdown: List<CategoryBreakdown>,
    dailyTrend: List<DailyTrendPoint>,
    aiReportContent: String,
    isGeneratingReport: Boolean,
    reportErrorMessage: String?,
    onGenerateReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    var analysisType by remember { mutableStateOf("EXPENSE") } // "EXPENSE" or "INCOME"

    val currentBreakdown = remember(analysisType, expenseBreakdown, incomeBreakdown) {
        if (analysisType == "EXPENSE") expenseBreakdown else incomeBreakdown
    }

    val totalSum = remember(currentBreakdown) {
        currentBreakdown.sumOf { it.amount }
    }

    // Dynamic Tag Statistics computations
    val tagBreakdown = remember(transactions, analysisType) {
        val breakdown = mutableMapOf<String, Double>()
        transactions.filter { it.type == analysisType && it.tags.isNotEmpty() }
            .forEach { tx ->
                val tagsList = tx.tags.split(Regex("\\s+")).filter { it.startsWith("#") && it.length > 1 }
                tagsList.forEach { tag ->
                    breakdown[tag] = (breakdown[tag] ?: 0.0) + tx.amount
                }
            }
        breakdown.toList().sortedByDescending { it.second }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    var showingExportDialog by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf("CSV") } // "CSV" or "JSON"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 80.dp)
    ) {
        // Toggle for Expense Analysis vs Income Analysis
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = { analysisType = "EXPENSE" },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (analysisType == "EXPENSE") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = if (analysisType == "EXPENSE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("toggle_analysis_expense")
            ) {
                Text("支出类别分析", fontWeight = FontWeight.Bold)
            }
            FilledTonalButton(
                onClick = { analysisType = "INCOME" },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (analysisType == "INCOME") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = if (analysisType == "INCOME") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("toggle_analysis_income")
            ) {
                Text("收入分类占比", fontWeight = FontWeight.Bold)
            }
        }

        // Category breakdown card
        CategoryDonutChart(
            analysisType = analysisType,
            currentBreakdown = currentBreakdown,
            totalSum = totalSum
        )

        // Trend graph card
        Spacer(modifier = Modifier.height(8.dp))
        TrendFlowChart(
            isEmpty = transactions.isEmpty(),
            dailyTrend = dailyTrend
        )

        // Activity Heatmap
        Spacer(modifier = Modifier.height(16.dp))
        TransactionHeatmap(
            transactions = transactions,
            analysisType = analysisType
        )

        // Dynmamic visual Tag analysis card if tags exist
        if (tagBreakdown.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tag_analysis_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (analysisType == "EXPENSE") "标签支出专项分析 (#项目)" else "标签收入专项分析 (#项目)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val maxAmount = tagBreakdown.firstOrNull()?.second ?: 1.0

                    tagBreakdown.forEach { (tag, amount) ->
                        val ratio = (amount / maxAmount).toFloat()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = tag,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                                Text(
                                    text = "¥ " + String.format("%.2f", amount),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                            )
                        }
                    }
                }
            }
        }

        // AI Financial Insights Card
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_report_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header with AI Spark visual
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "AI 财务透视与省钱报告",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isGeneratingReport) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "AI 正在分析您的消费行为并生成专属精细报告...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else if (!reportErrorMessage.isNullOrEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = reportErrorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onGenerateReport,
                            modifier = Modifier.align(Alignment.End).testTag("ai_report_retry_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("重试生成")
                        }
                    }
                } else if (aiReportContent.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "想要深度挖掘日常账单里隐藏的“消费黑洞”吗？让 AI 帮您深度透视，给您送上一份科学风趣的定制建议和金石记账寄语！",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onGenerateReport,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("generate_ai_report_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("生成 AI 记账健康与省钱报告", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        MarkdownText(
                            text = aiReportContent,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = onGenerateReport,
                                modifier = Modifier.testTag("regenerate_ai_report_btn")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("重新生成省钱报告")
                            }
                        }
                    }
                }
            }
        }

        // Standard CSV/JSON Data Export Card
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("export_data_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "账单明细数据导出",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "支持一键导出 CSV/JSON 标准明细数据，拷贝到剪贴板后可直接粘贴到电脑端 Excel、Numbers 中，方便进行深度个性化的记账与数据二次建模、统计分析。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            exportFormat = "CSV"
                            showingExportDialog = true
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("导出 CSV 明细", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Button(
                        onClick = {
                            exportFormat = "JSON"
                            showingExportDialog = true
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("导出 JSON 明细", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // Export viewer and copy dialog
        if (showingExportDialog) {
            val contentToCopy = remember(exportFormat, transactions) {
                if (exportFormat == "CSV") exportToCSV(transactions) else exportToJSON(transactions)
            }

            AlertDialog(
                onDismissRequest = { showingExportDialog = false },
                title = { Text(text = "$exportFormat 数据导出预览", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "总共 ${transactions.size} 条账目数据：",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = contentToCopy,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                ),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Ledger Export", contentToCopy)
                            clipboard.setPrimaryClip(clip)
                            showingExportDialog = false
                            android.widget.Toast.makeText(context, "$exportFormat 导出数据已复制到剪贴板！", android.widget.Toast.LENGTH_LONG).show()
                        }
                    ) {
                        Text("一键复制到剪贴板", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showingExportDialog = false }) {
                        Text("关闭")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

// Helpers for CSV / JSON string construction
fun exportToCSV(transactions: List<Transaction>): String {
    val sb = java.lang.StringBuilder()
    sb.append("ID,类型,描述,金额,分类,项目标签,时间\n")
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
    transactions.forEach { tx ->
        val typeLabel = if (tx.type == "EXPENSE") "支出" else "收入"
        val formattedDate = sdf.format(java.util.Date(tx.timestamp))
        val escapedTitle = tx.title.replace("\"", "\"\"")
        val escapedTags = tx.tags.replace("\"", "\"\"")
        sb.append("${tx.id},$typeLabel,\"$escapedTitle\",${tx.amount},\"${tx.category}\",\"$escapedTags\",\"$formattedDate\"\n")
    }
    return sb.toString()
}

fun exportToJSON(transactions: List<Transaction>): String {
    val sb = java.lang.StringBuilder()
    sb.append("[\n")
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
    transactions.forEachIndexed { idx, tx ->
        val typeLabel = if (tx.type == "EXPENSE") "支出" else "收入"
        val formattedDate = sdf.format(java.util.Date(tx.timestamp))
        sb.append("  {\n")
        sb.append("    \"id\": ${tx.id},\n")
        sb.append("    \"type\": \"${tx.type}\",\n")
        sb.append("    \"typeLabel\": \"$typeLabel\",\n")
        sb.append("    \"title\": \"${tx.title.replace("\"", "\\\"")}\",\n")
        sb.append("    \"amount\": ${tx.amount},\n")
        sb.append("    \"category\": \"${tx.category.replace("\"", "\\\"")}\",\n")
        sb.append("    \"tags\": \"${tx.tags.replace("\"", "\\\"")}\",\n")
        sb.append("    \"time\": \"$formattedDate\"\n")
        sb.append("  }")
        if (idx < transactions.size - 1) {
            sb.append(",")
        }
        sb.append("\n")
    }
    sb.append("]")
    return sb.toString()
}

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val lines = text.split("\n")
        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
            } else if (trimmed.startsWith("###")) {
                val headerText = trimmed.removePrefix("###").trim()
                Text(
                    text = headerText,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            } else if (trimmed.startsWith("##")) {
                val headerText = trimmed.removePrefix("##").trim()
                Text(
                    text = headerText,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
                )
            } else if (trimmed.startsWith("#")) {
                val headerText = trimmed.removePrefix("#").trim()
                Text(
                    text = headerText,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                )
            } else if (trimmed.startsWith("-") || trimmed.startsWith("*")) {
                val bulletText = trimmed.removePrefix("-").removePrefix("*").trim()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = parseBoldText(bulletText),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = parseBoldText(trimmed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun parseBoldText(input: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        val parts = input.split("**")
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                    append(part)
                }
            } else {
                append(part)
            }
        }
    }
}
