package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CategoryRegistry
import com.example.data.ParsedLedgerItem

@Composable
fun AIBookkeepingCard(
    aiInputText: String,
    isAiLoading: Boolean,
    aiParsedResult: ParsedLedgerItem?,
    aiErrorMessage: String,
    onAiInputTextChange: (String) -> Unit,
    onParseClick: () -> Unit,
    onConfirmSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF8E24AA), Color(0xFF1E88E5))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✨", fontSize = 16.sp, color = Color.White)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "AI 智能一句话记账",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Beta",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Text Input Field
            OutlinedTextField(
                value = aiInputText,
                onValueChange = onAiInputTextChange,
                placeholder = {
                    Text(
                        text = "写点什么，如“昨天麦当劳午餐花了35.5元”或“每月28号付ChatGPT会员140元”...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_bookkeeping_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                ),
                maxLines = 3,
                singleLine = false,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        keyboardController?.hide()
                        onParseClick()
                    }
                ),
                trailingIcon = {
                    if (aiInputText.isNotEmpty()) {
                        IconButton(onClick = { onAiInputTextChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "清除", tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "支持支出、收入和订阅记账",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        keyboardController?.hide()
                        onParseClick()
                    },
                    enabled = aiInputText.trim().isNotEmpty() && !isAiLoading,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("ai_parse_button")
                ) {
                    if (isAiLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("解析中...", style = MaterialTheme.typography.labelMedium)
                    } else {
                        Text("智能解析", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            // Error Display Message
            AnimatedVisibility(
                visible = aiErrorMessage.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .testTag("ai_error_card"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "错误",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = aiErrorMessage,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            if (aiErrorMessage.contains("配置你的 GEMINI_API_KEY") || aiErrorMessage.contains("GEMINI_API_KEY")) {
                                Text(
                                    text = "💡 设置指南：请打开本页右侧 AI Studio 控制面板，进入「Secrets」栏添加变量名为 GEMINI_API_KEY 的密钥（填入您的有效 Gemini API 秘钥代码），完成后返回即可畅享智能AI记账！",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Parsed Result Review block
            AnimatedVisibility(
                visible = aiParsedResult != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                if (aiParsedResult != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_result_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "💡 AI 帮您智能提炼了以下信息",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Item detail list grid
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("记录账目：", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(72.dp))
                                    Text(aiParsedResult.title.ifEmpty { "一句话记账" }, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("发生金额：", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(72.dp))
                                    Text(
                                        "¥ ${String.format("%.2f", aiParsedResult.amount)}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                        color = if (aiParsedResult.type == "INCOME") Color(0xFF2E7D32) else Color(0xFFC2185B)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("数据类型：", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(72.dp))
                                    val typeText = when (aiParsedResult.type) {
                                        "EXPENSE" -> "一般支出"
                                        "INCOME" -> "一般收入"
                                        "SUBSCRIPTION" -> "周期订阅 (${if (aiParsedResult.cycle == "MONTHLY") "按月" else "按年"})"
                                        else -> "未知账目"
                                    }
                                    val typeBg = when (aiParsedResult.type) {
                                        "EXPENSE" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                        "INCOME" -> Color(0xFFE8F5E9)
                                        else -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                                    }
                                    val typeColor = when (aiParsedResult.type) {
                                        "EXPENSE" -> MaterialTheme.colorScheme.error
                                        "INCOME" -> Color(0xFF2E7D32)
                                        else -> MaterialTheme.colorScheme.tertiary
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(typeBg)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(typeText, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = typeColor)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("支付账户：", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(72.dp))
                                    Text(aiParsedResult.account ?: "未提取出", style = MaterialTheme.typography.bodyMedium)
                                }

                                val category = CategoryRegistry.getCategoryById(aiParsedResult.category)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("所选分类：", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(72.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background((category?.color ?: Color.Gray).copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(category?.name ?: "未分类", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = category?.color ?: Color.Gray)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("发生日期：", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(72.dp))
                                    val offsetText = when (val offset = aiParsedResult.dateOffsetDays) {
                                        null, 0 -> "今天 (当前时间)"
                                        -1 -> "昨天"
                                        -2 -> "前天"
                                        else -> if (offset < 0) "${-offset} 天前" else "${offset} 天后"
                                    }
                                    Text(offsetText, style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Confirm & Cancel row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onCancel,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("ai_cancel_button")
                                ) {
                                    Text("放弃解析", style = MaterialTheme.typography.labelMedium)
                                }

                                Button(
                                    onClick = onConfirmSave,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2E7D32),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(40.dp)
                                        .testTag("ai_confirm_button")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "一键记账", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("一键确认记账", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Icon helper function overload support for integer size
@Composable
fun Icon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    tint: androidx.compose.ui.graphics.Color,
    size: Int
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(size.dp)
    )
}
