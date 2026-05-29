package com.example.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.foundation.clickable
import com.example.data.*
import com.example.ui.LedgerStats
import com.example.ui.components.AIBookkeepingCard
import com.example.ui.components.DashboardHeader
import com.example.ui.components.TopUserHeader
import com.example.ui.components.LedgerCalendarDialog
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionListTab(
    stats: LedgerStats,
    transactions: List<Transaction>,
    onDelete: (Int) -> Unit,
    onLoadSample: () -> Unit,
    aiInputText: String,
    isAiLoading: Boolean,
    aiParsedResult: ParsedLedgerItem?,
    aiErrorMessage: String,
    onAiInputTextChange: (String) -> Unit,
    onParseClick: () -> Unit,
    onConfirmSave: () -> Unit,
    onCancel: () -> Unit,
    getCategoryDef: (String) -> CategoryDef?,
    totalBudget: Double?,
    categoryBudgets: Map<String, Double>,
    currentMonthExpenseTotal: Double,
    currentMonthCategoryExpenses: Map<String, Double>,
    allExpenseCategories: List<CategoryDef>,
    onUpdateBudget: (String, Double) -> Unit,
    pendingReimbursementAmount: Double,
    reimbursedAmount: Double,
    onToggleReimbursementStatus: (Transaction) -> Unit,
    onToggleIsReimbursable: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "EXPENSE", "INCOME"
    var searchQuery by remember { mutableStateOf("") }
    var showCalendar by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }

    if (showCalendar) {
        LedgerCalendarDialog(
            transactions = transactions,
            onDismiss = { showCalendar = false },
            onDeleteTransaction = onDelete,
            totalBudget = totalBudget
        )
    }

    if (showBudgetDialog) {
        var tempTotalBudgetStr by remember { mutableStateOf(totalBudget?.toString() ?: "") }
        val categoryBudgetInputs = remember {
            mutableStateMapOf<String, String>().apply {
                categoryBudgets.forEach { (catId, amt) ->
                    put(catId, amt.toString())
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = {
                Text(
                    "🎯 设定月度消费预算",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = tempTotalBudgetStr,
                            onValueChange = { tempTotalBudgetStr = it },
                            label = { Text("本月总消费预算 (¥)") },
                            placeholder = { Text("例如 3000") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_total_budget")
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "按分类设定具体预算 (可选)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    items(allExpenseCategories) { category ->
                        val currentVal = categoryBudgetInputs[category.id] ?: ""
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(category.color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = category.name,
                                    tint = category.color,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = currentVal,
                                onValueChange = { categoryBudgetInputs[category.id] = it },
                                placeholder = { Text("未设定") },
                                singleLine = true,
                                modifier = Modifier.width(100.dp).testTag("input_category_budget_${category.id}")
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newTotal = tempTotalBudgetStr.toDoubleOrNull() ?: 0.0
                        onUpdateBudget("total", newTotal)

                        // Update categories
                        allExpenseCategories.forEach { category ->
                            val amt = categoryBudgetInputs[category.id]?.toDoubleOrNull() ?: 0.0
                            onUpdateBudget("category_${category.id}", amt)
                        }
                        showBudgetDialog = false
                    },
                    modifier = Modifier.testTag("save_budget_button")
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    val filteredTransactions = remember(transactions, selectedFilter, searchQuery) {
        val filteredByType = when (selectedFilter) {
            "EXPENSE" -> transactions.filter { it.type == "EXPENSE" }
            "INCOME" -> transactions.filter { it.type == "INCOME" }
            else -> transactions
        }
        if (searchQuery.isBlank()) {
            filteredByType
        } else {
            val q = searchQuery.trim().lowercase()
            filteredByType.filter { item ->
                item.title.lowercase().contains(q) ||
                item.notes.lowercase().contains(q) ||
                item.tags.lowercase().contains(q) ||
                item.category.lowercase().contains(q) ||
                (getCategoryDef(item.category)?.name?.lowercase()?.contains(q) == true)
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp) // Padding for FAB
    ) {
        item {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_bar"),
                placeholder = { Text("搜索账单、备注或标签...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )
        }
        item {
            TopUserHeader(onCalendarClick = { showCalendar = true })
        }
        item {
            DashboardHeader(stats = stats)
        }
        item {
            // Budget Progress Card
            val hasBudget = totalBudget != null && totalBudget > 0
            val spent = currentMonthExpenseTotal
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { showBudgetDialog = true }
                    .testTag("budget_progress_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (!hasBudget) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    } else if (spent > totalBudget) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                    } else if (spent >= totalBudget * 0.8) {
                        Color(0xFFFFF3E0).copy(alpha = 0.95f) // Warm amber warning
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (!hasBudget) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text(
                                    "🎯",
                                    fontSize = 24.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Column {
                                    Text(
                                        text = "开启本月消费理财预算",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "设定本月总消费预算，规避冲动消费",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            Button(
                                onClick = { showBudgetDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("set_budget_quick_btn")
                            ) {
                                Text("立即设定", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    } else {
                        val limit = totalBudget!!
                        val percentage = (spent / limit * 100).toInt()
                        val progressFraction = (spent / limit).toFloat().coerceIn(0f, 1f)
                        val isNearLimit = spent >= limit * 0.8
                        val isOverspent = spent > limit

                        val progressColor = if (isOverspent) {
                            Color(0xFFD32F2F)
                        } else if (isNearLimit) {
                            Color(0xFFE65100)
                        } else {
                            MaterialTheme.colorScheme.primary
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isOverspent) "⚠️ 已超支！" else if (isNearLimit) "⚠️ 冲动预警！已用80%+" else "📊 本月预算进度",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = if (isOverspent) Color(0xFFD32F2F) else if (isNearLimit) Color(0xFFE65100) else MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "设定 🎯 ¥${String.format("%.0f", limit)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "已消费 ¥${String.format("%,.1f", spent)}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$percentage%",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                color = progressColor
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = progressColor,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )

                        // List category budgets that are set
                        val activeCategoryBudgets = categoryBudgets.filter { it.value > 0 }
                        if (activeCategoryBudgets.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Text(
                                text = "分类预算明细",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                activeCategoryBudgets.forEach { (catId, catLimit) ->
                                    val catSpent = currentMonthCategoryExpenses[catId] ?: 0.0
                                    val catPerc = (catSpent / catLimit * 100).toInt()
                                    val catFrac = (catSpent / catLimit).toFloat().coerceIn(0f, 1f)
                                    val catCategory = getCategoryDef(catId)
                                    val catOver = catSpent > catLimit
                                    val catWarning = catSpent >= catLimit * 0.8 && !catOver

                                    val catColor = if (catOver) Color(0xFFD32F2F) else if (catWarning) Color(0xFFE65100) else MaterialTheme.colorScheme.primary

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(catCategory?.color?.copy(alpha = 0.15f) ?: Color.Gray.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = catCategory?.icon ?: Icons.Default.Info,
                                                contentDescription = catCategory?.name,
                                                tint = catCategory?.color ?: Color.Gray,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = catCategory?.name ?: catId,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "¥${String.format("%.0f", catSpent)} / ¥${String.format("%.0f", catLimit)} ($catPerc%)",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (catOver) Color(0xFFD32F2F) else if (catWarning) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                    
                                    LinearProgressIndicator(
                                        progress = { catFrac },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = catColor,
                                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            if (pendingReimbursementAmount > 0 || reimbursedAmount > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("reimbursement_summary_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "💼 报销及垫付资金追踪",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Text(
                                text = "报销核销后，自动剔除消费统计",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Column for pending
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "待收到 (垫付款中)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "¥${String.format("%,.2f", pendingReimbursementAmount)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = Color(0xFFE65100) // Warm Amber-Orange
                                )
                            }

                            // Column for cleared
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "已收到 (已核销)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "¥${String.format("%,.2f", reimbursedAmount)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = Color(0xFF2E7D32) // Fresh Green
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            AIBookkeepingCard(
                aiInputText = aiInputText,
                isAiLoading = isAiLoading,
                aiParsedResult = aiParsedResult,
                aiErrorMessage = aiErrorMessage,
                onAiInputTextChange = onAiInputTextChange,
                onParseClick = onParseClick,
                onConfirmSave = onConfirmSave,
                onCancel = onCancel
            )
        }
        item {
            // Filter pills row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("全部账单") },
                    modifier = Modifier.testTag("filter_all")
                )
                FilterChip(
                    selected = selectedFilter == "EXPENSE",
                    onClick = { selectedFilter = "EXPENSE" },
                    label = { Text("仅支出") },
                    modifier = Modifier.testTag("filter_expense")
                )
                FilterChip(
                    selected = selectedFilter == "INCOME",
                    onClick = { selectedFilter = "INCOME" },
                    label = { Text("仅收入") },
                    modifier = Modifier.testTag("filter_income")
                )
            }
        }

        if (filteredTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "无数据",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "目前没有任何账单明细哦",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "你可以点击下方「记一笔」添加账单，或者快速导入我们为您准备的演示数据体验图表功能！",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onLoadSample,
                            modifier = Modifier.testTag("load_sample_button")
                        ) {
                            Text("💡 导入演示数据")
                        }
                    }
                }
            }
        } else {
            items(filteredTransactions, key = { it.id }) { item ->
                val category = getCategoryDef(item.category)
                val formattedDate = remember(item.timestamp) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    sdf.format(Date(item.timestamp))
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                        .testTag("transaction_item_${item.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category Icon representation
                        Box(
                            modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(category?.color?.copy(alpha = 0.2f) ?: Color.Gray.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = category?.icon ?: Icons.Default.Info,
                                contentDescription = category?.name ?: "分类",
                                tint = category?.color ?: Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = category?.name ?: "未分类",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.account,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = formattedDate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }

                            // Show project tags visual badges
                            if (item.tags.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    item.tags.split(Regex("\\s+")).filter { it.isNotEmpty() }.forEach { tag ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = tag,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }

                            // Reimbursement specific item controls & badges
                            if (item.type == "EXPENSE") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!item.isReimbursable) {
                                        // Quick set to Reimbursable
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f))
                                                .clickable { onToggleIsReimbursable(item) }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                .testTag("action_toggle_reimbursable_${item.id}")
                                        ) {
                                            Text(
                                                text = "💼 设为报销",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    } else {
                                        // Pending badge
                                        if (item.reimbursementStatus == "PENDING") {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFFFF3E0))
                                                    .clickable { onToggleReimbursementStatus(item) }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    .testTag("action_claim_reimbursement_${item.id}")
                                            ) {
                                                Text(
                                                    text = "⏳ 待核销 ￥${String.format("%.1f", item.amount)}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = Color(0xFFE65100)
                                                )
                                            }
                                        } else {
                                            // Reimbursed badge
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFE8F5E9))
                                                    .clickable { onToggleReimbursementStatus(item) }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    .testTag("action_unclaim_reimbursement_${item.id}")
                                            ) {
                                                Text(
                                                    text = "✅ 已核销 ￥${String.format("%.1f", item.amount)}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color(0xFF2E7D32)
                                                )
                                            }
                                        }

                                        // Cancel tracking button
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                                                .clickable { onToggleIsReimbursable(item) }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                .testTag("action_cancel_reimbursement_${item.id}")
                                        ) {
                                            Text(
                                                text = "撤销",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Amount & Delete button
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val isReimbursed = item.type == "EXPENSE" && item.reimbursementStatus == "REIMBURSED"
                            val amountText = if (item.type == "EXPENSE") {
                                "- ¥ ${String.format("%.2f", item.amount)}"
                            } else {
                                "+ ¥ ${String.format("%.2f", item.amount)}"
                            }
                            val amountColor = if (isReimbursed) {
                                Color.Gray.copy(alpha = 0.7f)
                            } else if (item.type == "EXPENSE") {
                                Color(0xFFC2185B)
                            } else {
                                Color(0xFF2E7D32)
                            }
                            val textDecoration = if (isReimbursed) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None

                            Text(
                                text = amountText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = textDecoration
                                ),
                                color = amountColor
                            )
                            
                            IconButton(
                                onClick = { onDelete(item.id) },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("delete_transaction_${item.id}"),
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "删除账单",
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
