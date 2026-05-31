package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CurrencyConfig
import com.example.data.SupportedCurrency

@Composable
fun CurrencyRateSelector(
    amountText: String,
    selectedCurrency: String,
    onCurrencyChange: (String) -> Unit,
    exchangeRateText: String,
    onExchangeRateChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyList = CurrencyConfig.list

    // Flag emojis for visual flair
    fun getFlagEmoji(code: String): String {
        return when (code) {
            "CNY" -> "🇨🇳"
            "USD" -> "🇺🇸"
            "EUR" -> "🇪🇺"
            "JPY" -> "🇯🇵"
            "HKD" -> "🇭🇰"
            "GBP" -> "🇬🇧"
            else -> "🏳️"
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "记账币种 (Transaction Currency)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        // Horizontal scrolling list of Currency options
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(currencyList.size) { index ->
                val currency = currencyList[index]
                val isSelected = currency.code == selectedCurrency
                
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onCurrencyChange(currency.code)
                        // Auto-fill default exchange rate
                        onExchangeRateChange(currency.defaultRate.toString())
                    },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(getFlagEmoji(currency.code), fontSize = 16.sp)
                            Text(
                                text = "${currency.code} (${currency.symbol})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        // Show exchange rate input if selected currency is NOT CNY
        AnimatedVisibility(visible = selectedCurrency != "CNY") {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "外汇折算汇率系数",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "1 ${selectedCurrency} = ${exchangeRateText} CNY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        // Coefficient OutlinedTextField
                        OutlinedTextField(
                            value = exchangeRateText,
                            onValueChange = { onExchangeRateChange(it) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            label = { Text("折合汇率") },
                            modifier = Modifier.width(110.dp),
                            shape = RoundedCornerShape(10.dp),
                            maxLines = 1,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }

                    // Calculation Preview
                    val originalAmt = amountText.toDoubleOrNull() ?: 0.0
                    val rate = exchangeRateText.toDoubleOrNull() ?: 0.0
                    val convertedCNY = originalAmt * rate
                    if (originalAmt > 0 && rate > 0) {
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "系统自动折合 (Converted):",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "≈ ¥${String.format("%.2f", convertedCNY)} 人民币",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
