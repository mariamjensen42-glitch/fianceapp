package com.example.ui.tabs

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LedgerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionTab(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val controller = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    val expenseCats by viewModel.allExpenseCategories.collectAsState()
    val incomeCats by viewModel.allIncomeCategories.collectAsState()
    val activeTags by viewModel.activeTags.collectAsState()

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            viewModel.parseImageWithAi(bitmap)
        }
    }

    if (viewModel.isAiImageLoading) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("智能解析账单中") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("AI正在读取您的截图...")
                }
            },
            confirmButton = {}
        )
    }

    if (viewModel.aiImageErrorMessage.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { viewModel.aiImageErrorMessage = "" },
            title = { Text("解析失败") },
            text = { Text(viewModel.aiImageErrorMessage) },
            confirmButton = {
                TextButton(onClick = { viewModel.aiImageErrorMessage = "" }) {
                    Text("确定")
                }
            }
        )
    }

    if (viewModel.aiImageParsedResult != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelAiImageResult() },
            title = { Text("AI 账单解析成功") },
            text = {
                val res = viewModel.aiImageParsedResult!!
                Column {
                    Text("已成功从截图提取记账信息：", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("类型: ${if(res.type == "INCOME") "收入" else "支出"}")
                    Text("金额: ¥${res.amount}")
                    Text("支付账户: ${res.account}")
                    Text("项目名: ${res.title}")
                    Text("推断分类: ${res.category}")
                    if (!res.notes.isNullOrEmpty()) {
                        Text("备注信息: ${res.notes}")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.confirmAndSaveAiImageResult() }) {
                    Text("确认入账")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelAiImageResult() }) {
                    Text("取消并重新填单")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Expense/Income/Subscription Type Selector
        TabRow(
            selectedTabIndex = when(viewModel.typeInput) {
                "EXPENSE" -> 0
                "INCOME" -> 1
                else -> 2
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp)),
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Tab(
                selected = viewModel.typeInput == "EXPENSE",
                onClick = { viewModel.updateSelectedType("EXPENSE") },
                text = { Text("支出", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("type_toggle_expense")
            )
            Tab(
                selected = viewModel.typeInput == "INCOME",
                onClick = { viewModel.updateSelectedType("INCOME") },
                text = { Text("收入", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("type_toggle_income")
            )
            Tab(
                selected = viewModel.typeInput == "SUBSCRIPTION",
                onClick = { viewModel.updateSelectedType("SUBSCRIPTION") },
                text = { Text("订阅", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("type_toggle_subscription")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI Screenshot shortcut
        OutlinedButton(
            onClick = {
                pickMedia.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("📷", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("上传截图，让AI帮忙记账", fontWeight = FontWeight.Bold)
        }

        if (viewModel.typeInput == "SUBSCRIPTION") {
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                FilterChip(
                    selected = viewModel.cycleInput == "MONTHLY",
                    onClick = { viewModel.cycleInput = "MONTHLY" },
                    label = { Text("按月订阅") }
                )
                Spacer(modifier = Modifier.width(16.dp))
                FilterChip(
                    selected = viewModel.cycleInput == "YEARLY",
                    onClick = { viewModel.cycleInput = "YEARLY" },
                    label = { Text("按年订阅") }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Large Amount input field
        OutlinedTextField(
            value = viewModel.amountInput,
            onValueChange = { input ->
                if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                    viewModel.amountInput = input
                }
            },
            label = { Text("金额") },
            placeholder = { Text("0.00") },
            leadingIcon = { Text("¥", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("amount_input"),
            colors = OutlinedTextFieldDefaults.colors()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Title text Input field
        OutlinedTextField(
            value = viewModel.titleInput,
            onValueChange = { viewModel.titleInput = it },
            label = { Text("账单明细 (如: 午餐, 打车, 发工资)") },
            placeholder = { Text("添加备注描述...") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("title_input"),
            colors = OutlinedTextFieldDefaults.colors()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Custom tags input field
        OutlinedTextField(
            value = viewModel.tagsInput,
            onValueChange = { viewModel.tagsInput = it },
            label = { Text("项目/标签 (多个用空格分隔)") },
            placeholder = { Text("如: #端午旅行 #工作报销") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tags_input"),
            colors = OutlinedTextFieldDefaults.colors()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Preset active tags suggestions
        if (activeTags.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "快捷标签: ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    activeTags.forEach { tag ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .clickable {
                                    val current = viewModel.tagsInput.trim()
                                    if (!current.contains(tag)) {
                                        viewModel.tagsInput = if (current.isEmpty()) tag else "$current $tag"
                                    }
                                }
                        ) {
                            Text(
                                text = tag,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Multi-Account (Assets) selector
        Text(
            text = "支付账户 / 资产源",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val accounts = listOf("微信钱包", "支付宝", "储蓄卡", "信用卡", "现金")
            accounts.forEach { accountItem ->
                val isSelected = viewModel.accountInput == accountItem
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.accountInput = accountItem },
                    label = { Text(accountItem) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        if (viewModel.typeInput == "EXPENSE") {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reimbursement_input_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💼", fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
                            Column {
                                Text(
                                    text = "需要报销",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "选中后将支持报销核销跟踪",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Switch(
                            checked = viewModel.isReimbursableInput,
                            onCheckedChange = { viewModel.isReimbursableInput = it },
                            modifier = Modifier.testTag("reimbursement_toggle_switch")
                        )
                    }

                    if (viewModel.isReimbursableInput) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "当前报销季度进度",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FilterChip(
                                selected = viewModel.reimbursementStatusInput == "PENDING",
                                onClick = { viewModel.reimbursementStatusInput = "PENDING" },
                                label = { Text("待报销 (垫付中)") },
                                modifier = Modifier.weight(1f).testTag("status_chip_pending")
                            )
                            FilterChip(
                                selected = viewModel.reimbursementStatusInput == "REIMBURSED",
                                onClick = { viewModel.reimbursementStatusInput = "REIMBURSED" },
                                label = { Text("已报销 (已收到打款)") },
                                modifier = Modifier.weight(1f).testTag("status_chip_reimbursed")
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Category grid header with inline custom addition
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "选择分类",
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = viewModel.customCategoryNameInput,
                        onValueChange = { viewModel.customCategoryNameInput = it },
                        placeholder = { Text("新增自定义分类...") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.addCustomCategory(isExpense = viewModel.typeInput == "EXPENSE") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("+ 新分类", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dynamic categories lists based on selected type
        val currentCategories = if (viewModel.typeInput == "EXPENSE" || viewModel.typeInput == "SUBSCRIPTION") {
            expenseCats
        } else {
            incomeCats
        }

        // Beautiful Grid representation of category options
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                val chunked = currentCategories.chunked(3)
                chunked.forEach { rowCategoryDefs ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        rowCategoryDefs.forEach { cat ->
                            val isSelected = viewModel.categoryInput == cat.id
                            val containerBackground = if (isSelected) cat.color else Color.Transparent
                            val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(containerBackground.copy(alpha = if (isSelected) 1f else 0.15f))
                                    .clickable { viewModel.categoryInput = cat.id }
                                    .padding(vertical = 12.dp, horizontal = 4.dp)
                                    .testTag("category_option_${cat.id}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White.copy(alpha = 0.25f) else cat.color.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = cat.name,
                                        tint = if (isSelected) Color.White else cat.color,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) Color.White else contentColor
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Save Button
        Button(
            onClick = {
                controller?.hide()
                viewModel.saveTransaction()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_button"),
            enabled = viewModel.amountInput.toDoubleOrNull() != null && viewModel.amountInput.toDouble() > 0,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = "保存")
            Spacer(modifier = Modifier.width(8.dp))
            Text("完成并保存账单", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
        }
    }
}
