package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.tabs.AddTransactionTab
import com.example.ui.tabs.AnalyticsTab
import com.example.ui.tabs.SubscriptionListTab
import com.example.ui.tabs.TransactionListTab

@Composable
fun LedgerApp(viewModel: LedgerViewModel, modifier: Modifier = Modifier) {
    val currentTab = viewModel.currentTab
    val transactions by viewModel.transactions.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val expenseBreakdown by viewModel.expenseBreakdown.collectAsState()
    val incomeBreakdown by viewModel.incomeBreakdown.collectAsState()
    val dailyTrend by viewModel.dailyTrend.collectAsState()

    // Budget collecting states
    val totalBudget by viewModel.totalBudget.collectAsState()
    val categoryBudgets by viewModel.categoryBudgets.collectAsState()
    val currentMonthExpenseTotal by viewModel.currentMonthExpenseTotal.collectAsState()
    val currentMonthCategoryExpenses by viewModel.currentMonthCategoryExpenses.collectAsState()
    val allExpenseCategories by viewModel.allExpenseCategories.collectAsState()

    // Reimbursement tracker states
    val pendingReimbursementAmount by viewModel.pendingReimbursementAmount.collectAsState()
    val reimbursedAmount by viewModel.reimbursedAmount.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (currentTab != 2) {
                FloatingActionButton(
                    onClick = { viewModel.currentTab = 2 },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_add")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "记一笔")
                }
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("ledger_bottom_navigation"),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { viewModel.currentTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "资产") },
                    label = { Text("资产") },
                    modifier = Modifier.testTag("tab_transactions")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { viewModel.currentTab = 1 },
                    icon = { Icon(Icons.Default.Refresh, contentDescription = "订阅") },
                    label = { Text("订阅") },
                    modifier = Modifier.testTag("tab_subscriptions")
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { viewModel.currentTab = 3 },
                    icon = { Icon(Icons.Default.Star, contentDescription = "统计") },
                    label = { Text("统计") },
                    modifier = Modifier.testTag("tab_analytics")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen content switching smoothly
            when (currentTab) {
                0 -> TransactionListTab(
                    stats = stats,
                    transactions = transactions,
                    onDelete = { viewModel.deleteTransaction(it) },
                    onLoadSample = { viewModel.loadSampleData() },
                    aiInputText = viewModel.aiInputText,
                    isAiLoading = viewModel.isAiLoading,
                    aiParsedResult = viewModel.aiParsedResult,
                    aiErrorMessage = viewModel.aiErrorMessage,
                    onAiInputTextChange = { viewModel.aiInputText = it },
                    onParseClick = { viewModel.parseInputWithAi(viewModel.aiInputText) },
                    onConfirmSave = { viewModel.confirmAndSaveAiResult() },
                    onCancel = { viewModel.cancelAiResult() },
                    getCategoryDef = { viewModel.getCategoryDef(it) },
                    totalBudget = totalBudget,
                    categoryBudgets = categoryBudgets,
                    currentMonthExpenseTotal = currentMonthExpenseTotal,
                    currentMonthCategoryExpenses = currentMonthCategoryExpenses,
                    allExpenseCategories = allExpenseCategories,
                    onUpdateBudget = { id, amt -> viewModel.updateBudget(id, amt) },
                    pendingReimbursementAmount = pendingReimbursementAmount,
                    reimbursedAmount = reimbursedAmount,
                    onToggleReimbursementStatus = { viewModel.toggleReimbursementStatus(it) },
                    onToggleIsReimbursable = { viewModel.toggleIsReimbursable(it) }
                )
                1 -> SubscriptionListTab(
                    subscriptions = viewModel.subscriptions.collectAsState().value,
                    onDelete = { viewModel.deleteSubscription(it) },
                    onRenew = { viewModel.renewSubscription(it) }
                )
                2 -> AddTransactionTab(viewModel = viewModel)
                3 -> AnalyticsTab(
                    transactions = transactions,
                    expenseBreakdown = expenseBreakdown,
                    incomeBreakdown = incomeBreakdown,
                    dailyTrend = dailyTrend,
                    aiReportContent = viewModel.aiReportContent,
                    isGeneratingReport = viewModel.isGeneratingReport,
                    reportErrorMessage = viewModel.reportErrorMessage,
                    onGenerateReport = { viewModel.generateFinancialReport() }
                )
            }
        }
    }
}
