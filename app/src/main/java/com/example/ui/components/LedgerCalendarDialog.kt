package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.DateRangeFilter

@Composable
fun LedgerCalendarDialog(
    currentFilter: DateRangeFilter,
    onFilterSelected: (DateRangeFilter, Long?, Long?) -> Unit,
    onDismissRequest: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf(currentFilter) }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("ledger_calendar_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "选择日期范围进行过滤",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                val filterOptions = listOf(
                    DateRangeFilter.ALL to "全部账单 (All)",
                    DateRangeFilter.TODAY to "今天 (Today)",
                    DateRangeFilter.THIS_WEEK to "本周 (This Week)",
                    DateRangeFilter.THIS_MONTH to "本月 (This Month)",
                    DateRangeFilter.CUSTOM to "自定义区间 (Custom, 近一个月)"
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterOptions.forEach { (filter, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFilter = filter }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (selectedFilter == DateRangeFilter.CUSTOM) {
                                // Default last 30 days for CUSTOM filter for simplicity
                                val end = System.currentTimeMillis()
                                val start = end - 30L * 24 * 60 * 60 * 1000
                                onFilterSelected(DateRangeFilter.CUSTOM, start, end)
                            } else {
                                onFilterSelected(selectedFilter, null, null)
                            }
                            onDismissRequest()
                        },
                        modifier = Modifier.testTag("dialog_confirm_button")
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}
