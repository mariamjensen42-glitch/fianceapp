package com.example.ui.tabs

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.draw.scale
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.Subscription
import com.example.data.CurrencyConfig
import com.example.ui.LedgerViewModel
import com.example.ui.components.CurrencyRateSelector
import com.example.ui.theme.ColorExpense
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubscriptionListTab(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val subs by viewModel.subscriptions.collectAsState()
    val scope = rememberCoroutineScope()

    // Adding form state
    var titleText by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Category.BILLS) }
    var selectedCycle by remember { mutableStateOf("MONTHLY") } // "MONTHLY" or "ANNUALLY"

    var selectedCurrency by remember { mutableStateOf("CNY") }
    var exchangeRateText by remember { mutableStateOf("1.0") }

    var addSubSuccess by remember { mutableStateOf(false) }

    // Calculate dynamic monthly costs in CNY using exchange rates
    val activeSubs = subs.filter { it.isActive }
    val monthlySubsTotalCost = activeSubs.sumOf {
        val inCNY = it.amount * it.exchangeRate
        if (it.billingCycle == "MONTHLY") inCNY else (inCNY / 12.0)
    }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
            .testTag("subscription_list_tab")
    ) {
        // Summary Widget Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = "周期付费",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "周期订阅月损 (Monthly Autopays)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("¥%,.2f/月", monthlySubsTotalCost),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("monthly_autopays_total")
                    )
                }
            }
        }

        Text(
            text = "我的周期性订阅列表",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (subs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "您目前还没有配置周期订阅服务 🏖️",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                subs.forEach { sub ->
                    SubscriptionItemCard(
                        subscription = sub,
                        onActiveToggle = { active ->
                            viewModel.updateSubscription(sub.copy(isActive = active))
                        },
                        onDeleteClick = {
                            viewModel.deleteSubscriptionById(sub.id)
                        },
                        formattedDate = sdf.format(Date(sub.nextBillingDate))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Trigger New Subscription inline Form Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_subscription_form"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EventNote,
                        contentDescription = "New sub",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "新增周期协议记录",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("服务名称 (如 Netflix, 健身房, 房租)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_sub_title_field"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("扣费款额 (Amount)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_sub_amount_field"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Currency and conversion rate selector inside the Subscription Addition form
                CurrencyRateSelector(
                    amountText = amountText,
                    selectedCurrency = selectedCurrency,
                    onCurrencyChange = { selectedCurrency = it },
                    exchangeRateText = exchangeRateText,
                    onExchangeRateChange = { exchangeRateText = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Cycle selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "扣费周期:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedCycle == "MONTHLY") MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { selectedCycle = "MONTHLY" }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("cycle_monthly_tab")
                        ) {
                            Text(
                                "月付",
                                color = if (selectedCycle == "MONTHLY") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedCycle == "ANNUALLY") MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { selectedCycle = "ANNUALLY" }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("cycle_annually_tab")
                        ) {
                            Text(
                                "年付",
                                color = if (selectedCycle == "ANNUALLY") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Group Categories Selector
                Text(
                    text = "所属类别:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Category.values()
                        .filter { it.defaultType == com.example.data.TransactionType.EXPENSE }
                        .forEach { cat ->
                            val isSelected = selectedCategory == cat
                            Card(
                                modifier = Modifier
                                    .padding(vertical = 2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedCategory = cat }
                                    .testTag("sub_select_category_${cat.name}"),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) cat.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = cat.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) cat.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(visible = addSubSuccess) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Added sub success",
                            tint = Color(0xFF10B981)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "协议扣款服务已记录并启动！",
                            color = Color(0xFF10B981),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull()
                        val rate = exchangeRateText.toDoubleOrNull() ?: 1.0
                        if (amt != null && amt > 0 && titleText.isNotBlank()) {
                            viewModel.addSubscription(
                                title = titleText,
                                amount = amt,
                                category = selectedCategory,
                                billingCycle = selectedCycle,
                                nextBillingDate = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000, // default 30 days
                                currency = selectedCurrency,
                                exchangeRate = rate
                            )
                            titleText = ""
                            amountText = ""
                            selectedCurrency = "CNY"
                            exchangeRateText = "1.0"
                            addSubSuccess = true
                            scope.launch {
                                delay(2000)
                                addSubSuccess = false
                            }
                        }
                    },
                    enabled = titleText.isNotBlank() && amountText.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_sub_submit_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Confirm sub submit"
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("录入和追踪扣款", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionItemCard(
    subscription: Subscription,
    onActiveToggle: (Boolean) -> Unit,
    onDeleteClick: () -> Unit,
    formattedDate: String
) {
    val categoryObj = Category.fromName(subscription.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subscription_item_${subscription.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (subscription.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(categoryObj.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    tint = categoryObj.color
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = subscription.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (subscription.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Text(
                    text = if (subscription.billingCycle == "MONTHLY") "约付账目 (月付)" else "年费支出 (年付)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "下期缴扣时间: $formattedDate",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                val formattedPrice = CurrencyConfig.formatPrice(subscription.amount, subscription.currency)
                Text(
                    text = formattedPrice,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (subscription.isActive) ColorExpense else Color.Gray,
                    modifier = Modifier.testTag("sub_cost_amount_${subscription.id}")
                )

                if (subscription.currency != "CNY") {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "≈ ${CurrencyConfig.formatToCNY(subscription.amount, subscription.exchangeRate)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = subscription.isActive,
                        onCheckedChange = onActiveToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = categoryObj.color,
                            checkedTrackColor = categoryObj.color.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .scale(0.8f)
                            .testTag("sub_toggle_${subscription.id}")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("delete_sub_btn_${subscription.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "删除订阅",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}


