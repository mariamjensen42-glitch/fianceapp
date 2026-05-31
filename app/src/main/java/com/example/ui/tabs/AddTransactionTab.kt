package com.example.ui.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Category
import com.example.data.TransactionType
import com.example.data.CurrencyConfig
import com.example.ui.LedgerViewModel
import com.example.ui.components.CurrencyRateSelector
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTransactionTab(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategory by remember { mutableStateOf(Category.FOOD) }

    var selectedCurrency by remember { mutableStateOf("CNY") }
    var exchangeRateText by remember { mutableStateOf("1.0") }

    var formError by remember { mutableStateOf<String?>(null) }
    var addSuccess by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val quickTemplates by viewModel.quickTemplates.collectAsState()
    var selectedTemplateForAction by remember { mutableStateOf<com.example.data.QuickTemplate?>(null) }
    var showSaveAsTemplateDialog by remember { mutableStateOf(false) }

    // Align category when type changes
    LaunchedEffect(selectedType) {
        selectedCategory = if (selectedType == TransactionType.EXPENSE) Category.FOOD else Category.SALARY
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
            .testTag("add_transaction_tab")
    ) {
        // --- 1. Quick templates deck ---
        if (quickTemplates.isNotEmpty()) {
            Text(
                text = "高频场景极速记账 (Quick Log Templates)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "点击可触发：一键直接入账（记录至系统），或载入数据修改",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("quick_templates_row")
            ) {
                items(quickTemplates.size) { index ->
                    val template = quickTemplates[index]
                    val cat = try { Category.valueOf(template.category) } catch (e: Exception) { Category.fromName(template.category) }
                    val emoji = when (cat) {
                        Category.FOOD -> "🍔"
                        Category.SHOPPING -> "🛒"
                        Category.TRANSPORT -> "🚇"
                        Category.ENTERTAINMENT -> "🎮"
                        Category.BILLS -> "🧾"
                        Category.SALARY -> "💰"
                        Category.INVESTMENT -> "📈"
                        Category.OTHER -> "📝"
                    }

                    Card(
                        modifier = Modifier
                            .width(135.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedTemplateForAction = template }
                            .testTag("quick_template_item_${template.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(cat.color.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 16.sp)
                                }
                                if (template.usageCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${template.usageCount}次",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = template.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            val prefix = if (template.type == "EXPENSE") "-" else "+"
                            val priceColor = if (template.type == "EXPENSE") com.example.ui.theme.ColorExpense else com.example.ui.theme.ColorIncome
                            val formattedPrice = CurrencyConfig.formatPrice(template.amount, template.currency)
                            Text(
                                text = "$prefix $formattedPrice",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = priceColor
                            )

                            if (template.note.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = template.note,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        // --- 2. Manual logging header and input forms ---
        Text(
            text = "手动收支记账 (Manual Log entry)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Expense vs Income tabs selector
        TabRow(
            selectedTabIndex = if (selectedType == TransactionType.EXPENSE) 0 else 1,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .testTag("add_tx_type_tabs")
        ) {
            Tab(
                selected = selectedType == TransactionType.EXPENSE,
                onClick = { selectedType = TransactionType.EXPENSE },
                modifier = Modifier.testTag("tab_expense")
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("支出 (Expense)", fontWeight = FontWeight.Bold)
                }
            }
            Tab(
                selected = selectedType == TransactionType.INCOME,
                onClick = { selectedType = TransactionType.INCOME },
                modifier = Modifier.testTag("tab_income")
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("收入 (Income)", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Amount Input Row
        OutlinedTextField(
            value = amountText,
            onValueChange = {
                if (it.isEmpty() || it.toDoubleOrNull() != null || it.endsWith(".")) {
                    amountText = it
                    formError = null
                }
            },
            label = { Text("交易金额 (Amount)") },
            placeholder = { Text("0.00") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_tx_amount_field"),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Currency and Rate Selector Component
        CurrencyRateSelector(
            amountText = amountText,
            selectedCurrency = selectedCurrency,
            onCurrencyChange = { selectedCurrency = it },
            exchangeRateText = exchangeRateText,
            onExchangeRateChange = { exchangeRateText = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Choose category title
        Text(
            text = "选择账单分类 (Category)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Category Cards Grid FlowRow
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Category.values()
                .filter { it.defaultType == selectedType || it == Category.OTHER }
                .forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Card(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedCategory = cat }
                            .testTag("select_category_${cat.name}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) cat.color.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            cat.color
                        ) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(cat.color)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cat.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) cat.color else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Note Input Card
        OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it },
            label = { Text("说明备注 (Note)") },
            placeholder = { Text("记下一笔账，留住细节...") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_tx_note_field"),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Alerts visibility blocks
        AnimatedVisibility(visible = formError != null, enter = fadeIn(), exit = fadeOut()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formError ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        AnimatedVisibility(visible = addSuccess, enter = fadeIn(), exit = fadeOut()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "账目已成功录入系统存储！",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Add Transaction Button
        Button(
            onClick = {
                val amt = amountText.toDoubleOrNull()
                val rate = exchangeRateText.toDoubleOrNull() ?: 1.0
                if (amt == null || amt <= 0) {
                    formError = "请输入大于 0 的有效数值金额。"
                } else if (selectedCurrency != "CNY" && rate <= 0) {
                    formError = "请输入大于 0 的有效外汇折算汇率系数。"
                } else {
                    viewModel.addTransaction(
                        amount = amt,
                        type = selectedType,
                        category = selectedCategory,
                        note = noteText,
                        timestamp = System.currentTimeMillis(),
                        currency = selectedCurrency,
                        exchangeRate = rate
                    )
                    addSuccess = true
                    amountText = ""
                    noteText = ""
                    selectedCurrency = "CNY"
                    exchangeRateText = "1.0"
                    scope.launch {
                        delay(2500)
                    }
                }
            },
            enabled = amountText.isNotBlank(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_tx_submit_btn")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Submit add"
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "确认并保存账目",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // --- 3. Save as Template helper ---
        val isSaveTemplateCurrentlyEnabled = amountText.toDoubleOrNull() != null && amountText.toDouble() > 0
        if (isSaveTemplateCurrentlyEnabled) {
            Spacer(modifier = Modifier.height(10.dp))
            androidx.compose.material3.OutlinedButton(
                onClick = {
                    showSaveAsTemplateDialog = true
                },
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_tx_save_template_btn")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Save template",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "将当前数据保存为极速模板",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- 4. Modals and Dialog overlays for templates ---
        if (selectedTemplateForAction != null) {
            val template = selectedTemplateForAction!!
            val cat = try { Category.valueOf(template.category) } catch (e: Exception) { Category.fromName(template.category) }
            val isExpense = template.type == "EXPENSE"
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { selectedTemplateForAction = null },
                title = {
                    Text(
                        text = "模版记账选项",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "模板：${template.name}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("收支类型", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        if (isExpense) "支出 (Expense)" else "收入 (Income)",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isExpense) com.example.ui.theme.ColorExpense else com.example.ui.theme.ColorIncome
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("账单分类", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(cat.displayName, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("记账金额", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    val formattedOriginal = CurrencyConfig.formatPrice(template.amount, template.currency)
                                    if (template.currency == "CNY") {
                                        Text(formattedOriginal, fontWeight = FontWeight.ExtraBold)
                                    } else {
                                        val converted = CurrencyConfig.formatToCNY(template.amount, template.exchangeRate)
                                        Text("$formattedOriginal ($converted)", fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("说明备注", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(template.note.ifBlank { "无" }, fontWeight = FontWeight.Normal)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.useQuickTemplate(template)
                                selectedTemplateForAction = null
                                addSuccess = true
                                scope.launch {
                                    delay(2000)
                                    addSuccess = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("1键直接入账 (Instantly Log)", fontWeight = FontWeight.Bold)
                        }

                        androidx.compose.material3.FilledTonalButton(
                            onClick = {
                                amountText = template.amount.toString()
                                selectedType = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME
                                selectedCategory = cat
                                noteText = template.note
                                selectedCurrency = template.currency
                                exchangeRateText = template.exchangeRate.toString()
                                selectedTemplateForAction = null
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("载入到下方表单 (Load into form)", fontWeight = FontWeight.Bold)
                        }

                        androidx.compose.material3.TextButton(
                            onClick = {
                                viewModel.deleteQuickTemplate(template)
                                selectedTemplateForAction = null
                            },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("删除此模板", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { selectedTemplateForAction = null }
                    ) {
                        Text("关闭")
                    }
                }
            )
        }

        if (showSaveAsTemplateDialog) {
            val suggestedName = if (noteText.isNotBlank()) noteText else "常用 ${selectedCategory.displayName}"
            var tempName by remember { mutableStateOf(suggestedName) }
            var tempError by remember { mutableStateOf<String?>(null) }

            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showSaveAsTemplateDialog = false },
                title = { Text("保存为快速模板", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val originalVal = CurrencyConfig.formatPrice(amountText.toDoubleOrNull() ?: 0.0, selectedCurrency)
                        val rateHelpText = if (selectedCurrency != "CNY") " (汇率折算后 ≈ ¥${String.format("%.2f", (amountText.toDoubleOrNull() ?: 0.0) * (exchangeRateText.toDoubleOrNull() ?: 1.0))})" else ""
                        Text(
                            text = "模板将会保存当前的金额 ($originalVal) 和分类 (${selectedCategory.displayName})$rateHelpText。请输入模板名称：",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = tempName,
                            onValueChange = {
                                tempName = it
                                if (it.isNotBlank()) tempError = null
                            },
                            label = { Text("模板名称") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            isError = tempError != null
                        )
                        if (tempError != null) {
                            Text(
                                text = tempError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (tempName.isBlank()) {
                                tempError = "名称不能为空"
                            } else {
                                val amt = amountText.toDoubleOrNull() ?: 0.0
                                viewModel.addQuickTemplate(
                                    name = tempName,
                                    amount = amt,
                                    type = selectedType,
                                    category = selectedCategory,
                                    note = noteText,
                                    currency = selectedCurrency,
                                    exchangeRate = exchangeRateText.toDoubleOrNull() ?: 1.0
                                )
                                showSaveAsTemplateDialog = false
                            }
                        }
                    ) {
                        Text("确认保存")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { showSaveAsTemplateDialog = false }
                    ) {
                        Text("取消")
                    }
                }
            )
        }
    }
}
