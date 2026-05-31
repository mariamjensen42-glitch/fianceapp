package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.AIBookkeepingCard
import com.example.ui.components.DashboardHeader
import com.example.ui.components.LedgerCalendarDialog
import com.example.ui.components.TopUserHeader
import com.example.ui.tabs.AddTransactionTab
import com.example.ui.tabs.AnalyticsTab
import com.example.ui.tabs.SettingsTab
import com.example.ui.tabs.SubscriptionListTab
import com.example.ui.tabs.TransactionListTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerApp(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    // Collect active flow values
    val allTxs by viewModel.transactions.collectAsState()
    val filteredTxs by viewModel.filteredTransactions.collectAsState()
    val dateFilterVal by viewModel.dateFilter.collectAsState()
    val isParsingAI by viewModel.isParsingAI.collectAsState()
    val aiParsingError by viewModel.aiParsingError.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    var showCalendarDialog by remember { mutableStateOf(false) }

    val dateFilterLabel = when (dateFilterVal) {
        DateRangeFilter.ALL -> "全部日期 (All)"
        DateRangeFilter.TODAY -> "今天 (Today)"
        DateRangeFilter.THIS_WEEK -> "本周 (This Week)"
        DateRangeFilter.THIS_MONTH -> "本月 (This Month)"
        DateRangeFilter.CUSTOM -> "自定义区间 (Custom)"
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("ledger_scaffold"),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("ledger_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = "Tab list") },
                    label = { Text("明细 (List)", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_tab_list")
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    icon = { Icon(Icons.Default.AddCircleOutline, contentDescription = "Tab add") },
                    label = { Text("记账 (Add)", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_tab_add")
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    icon = { Icon(Icons.Default.AddCard, contentDescription = "Tab subs") },
                    label = { Text("周期 (Subs)", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_tab_subs")
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 3,
                    onClick = { selectedTabIndex = 3 },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = "Tab stats") },
                    label = { Text("图表 (Chart)", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_tab_analytics")
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 4,
                    onClick = { selectedTabIndex = 4 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Tab settings") },
                    label = { Text("设置 (Settings)", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
        ) {
            // Welcome Greeting Banner
            TopUserHeader()

            // Main Contents switching tab
            when (selectedTabIndex) {
                0 -> {
                    // Dashboard banner inside list to show aggregated stats nicely!
                    DashboardHeader(
                        transactions = filteredTxs,
                        dateRangeLabel = dateFilterLabel,
                        onFilterClick = { showCalendarDialog = true },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    TransactionListTab(
                        viewModel = viewModel,
                        modifier = Modifier.weight(1f)
                    )
                }

                1 -> {
                    // Unified regular Add + AI Bookkeeping screen
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        AIBookkeepingCard(
                            isParsing = isParsingAI,
                            parsingError = aiParsingError,
                            onParseText = { text, onDone ->
                                viewModel.parseAndAddTransactionsAI(text, onDone)
                            },
                            modifier = Modifier.padding(bottom = 18.dp)
                        )

                        AddTransactionTab(
                            viewModel = viewModel,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                2 -> {
                    SubscriptionListTab(viewModel = viewModel)
                }

                3 -> {
                    AnalyticsTab(viewModel = viewModel)
                }

                4 -> {
                    SettingsTab(viewModel = viewModel)
                }
            }
        }

        // Modal triggers
        if (showCalendarDialog) {
            LedgerCalendarDialog(
                currentFilter = dateFilterVal,
                onFilterSelected = { filter, start, end ->
                    viewModel.dateFilter.value = filter
                    viewModel.customStartDate.value = start
                    viewModel.customEndDate.value = end
                },
                onDismissRequest = { showCalendarDialog = false }
            )
        }
    }
}
