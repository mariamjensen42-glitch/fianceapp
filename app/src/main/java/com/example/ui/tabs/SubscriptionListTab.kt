package com.example.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CategoryRegistry
import com.example.data.Subscription
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SubscriptionListTab(
    subscriptions: List<Subscription>,
    onDelete: (Int) -> Unit,
    onRenew: (Subscription) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalMonthly = remember(subscriptions) {
        subscriptions.sumOf {
            if (it.cycle == "MONTHLY") it.amount else it.amount / 12.0
        }
    }

    val todayStart = remember(subscriptions) {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val urgentCount = remember(subscriptions, todayStart) {
        subscriptions.count { sub ->
            val billingTime = Calendar.getInstance().apply {
                timeInMillis = sub.nextBillingDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val diffMs = billingTime - todayStart
            val daysLeft = (diffMs / (24 * 60 * 60 * 1000L)).toInt()
            daysLeft <= 3
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("估算每月固定订阅支出", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
                Spacer(modifier = Modifier.height(4.dp))
                Text("¥ ${String.format("%.2f", totalMonthly)}", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }

        if (urgentCount > 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "提醒",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "订阅续费倒计时提醒",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "您有 ${urgentCount} 个订阅项目即将到期，请注意卡内余额或点击右侧一键记账续费！",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        if (subscriptions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无订阅记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(subscriptions, key = { "sub_${it.id}" }) { item ->
                    val category = CategoryRegistry.getCategoryById(item.category)
                    
                    val daysLeft = remember(item.nextBillingDate, todayStart) {
                        val billingTime = Calendar.getInstance().apply {
                            timeInMillis = item.nextBillingDate
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        val diffMs = billingTime - todayStart
                        (diffMs / (24 * 60 * 60 * 1000L)).toInt()
                    }

                    val nextBillingFormat = remember(item.nextBillingDate) {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        sdf.format(Date(item.nextBillingDate))
                    }

                    val badgeColor: Color
                    val badgeTextColor: Color
                    val badgeText: String
                    val borderStroke = when {
                        daysLeft < 0 -> {
                            badgeColor = MaterialTheme.colorScheme.errorContainer
                            badgeTextColor = MaterialTheme.colorScheme.onErrorContainer
                            badgeText = "⚠️ 逾期 ${-daysLeft} 天"
                            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        }
                        daysLeft == 0 -> {
                            badgeColor = Color(0xFFFFCDD2)
                            badgeTextColor = Color(0xFFC62828)
                            badgeText = "🔥 今天扣款"
                            androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFC62828))
                        }
                        daysLeft == 1 -> {
                            badgeColor = Color(0xFFFFECB3)
                            badgeTextColor = Color(0xFFFF8F00)
                            badgeText = "⏳ 明天扣款"
                            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF8F00))
                        }
                        daysLeft <= 3 -> {
                            badgeColor = Color(0xFFFFE082)
                            badgeTextColor = Color(0xFFF57F17)
                            badgeText = "⏰ 还有 ${daysLeft} 天"
                            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF57F17).copy(alpha = 0.6f))
                        }
                        else -> {
                            badgeColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            badgeTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                            badgeText = "🗓️ 还有 ${daysLeft} 天"
                            null
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = borderStroke
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(category?.color?.copy(alpha = 0.15f) ?: Color.Gray.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        category?.icon ?: Icons.Default.Info,
                                        contentDescription = null,
                                        tint = category?.color ?: Color.Gray,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        item.title,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            if (item.cycle == "MONTHLY") "按月收费订阅" else "按年收费订阅",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "•",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            category?.name ?: "未分类",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = category?.color ?: Color.Gray
                                        )
                                    }
                                }
                                
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "¥ ${String.format("%.2f", item.amount)}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        if (item.cycle == "MONTHLY") "/月" else "/年",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(badgeColor)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "$badgeText ($nextBillingFormat)",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = badgeTextColor
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onRenew(item) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "续费记账",
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "记账并延期",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    FilledIconButton(
                                        onClick = { onDelete(item.id) },
                                        modifier = Modifier.size(34.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "删除订阅",
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
