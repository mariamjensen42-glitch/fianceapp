package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Category
import com.example.data.LedgerRepository
import com.example.data.Subscription
import com.example.data.Transaction
import com.example.data.TransactionType
import com.example.data.GeminiService
import com.example.data.QuickTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class DateRangeFilter {
    ALL, TODAY, THIS_WEEK, THIS_MONTH, CUSTOM
}

class LedgerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: LedgerRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = LedgerRepository(db)

        // Seed initial high-frequency quick templates if empty
        viewModelScope.launch {
            try {
                val existing = repository.allTemplates.first()
                if (existing.isEmpty()) {
                    val defaults = listOf(
                        QuickTemplate(name = "日常买早餐", amount = 15.0, type = "EXPENSE", category = "FOOD", note = "一日之计在于晨"),
                        QuickTemplate(name = "乘地铁通勤", amount = 4.0, type = "EXPENSE", category = "TRANSPORT", note = "日常通勤"),
                        QuickTemplate(name = "续命热咖啡", amount = 22.0, type = "EXPENSE", category = "ENTERTAINMENT", note = "咖啡提神"),
                        QuickTemplate(name = "吃工作午餐", amount = 30.0, type = "EXPENSE", category = "FOOD", note = "工作日午餐"),
                        QuickTemplate(name = "超市买百货", amount = 50.0, type = "EXPENSE", category = "SHOPPING", note = "日常补货"),
                        QuickTemplate(name = "投资发红利", amount = 100.0, type = "INCOME", category = "INVESTMENT", note = "理财红利")
                    )
                    defaults.forEach { repository.insertTemplate(it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Reactively observe from database
    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val subscriptions: StateFlow<List<Subscription>> = repository.allSubscriptions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val quickTemplates: StateFlow<List<QuickTemplate>> = repository.allTemplates
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI Filters
    val dateFilter = MutableStateFlow(DateRangeFilter.THIS_MONTH)
    val customStartDate = MutableStateFlow<Long?>(null)
    val customEndDate = MutableStateFlow<Long?>(null)
    val selectedCategoryFilter = MutableStateFlow<Category?>(null)
    val searchQuery = MutableStateFlow("")

    // Filtered Transactions State
    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        transactions,
        dateFilter,
        customStartDate,
        customEndDate,
        selectedCategoryFilter,
        searchQuery
    ) { args: Array<Any?> ->
        val txList = args[0] as List<Transaction>
        val dateRange = args[1] as DateRangeFilter
        val start = args[2] as? Long
        val end = args[3] as? Long
        val cat = args[4] as? Category
        val search = args[5] as String

        txList.filter { tx ->
            // 1. Text Search Filter
            val matchesSearch = search.isEmpty() || 
                tx.note.contains(search, ignoreCase = true) || 
                tx.category.contains(search, ignoreCase = true)

            // 2. Category Filter
            val matchesCategory = cat == null || tx.category == cat.name || tx.category == cat.displayName

            // 3. Date Filter
            val matchesDate = when (dateRange) {
                DateRangeFilter.ALL -> true
                DateRangeFilter.TODAY -> isSameDay(tx.timestamp, System.currentTimeMillis())
                DateRangeFilter.THIS_WEEK -> isThisWeek(tx.timestamp)
                DateRangeFilter.THIS_MONTH -> isThisMonth(tx.timestamp)
                DateRangeFilter.CUSTOM -> {
                    val s = start ?: 0L
                    val e = end ?: Long.MAX_VALUE
                    tx.timestamp in s..e
                }
            }

            matchesSearch && matchesCategory && matchesDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- AI Assist States ---
    val isParsingAI = MutableStateFlow(false)
    val aiParsingError = MutableStateFlow<String?>(null)

    val isAnalyzingAI = MutableStateFlow(false)
    val aiAdvice = MutableStateFlow<String?>(null)

    init {
        // Trigger initial advice generate when transactions load
        viewModelScope.launch {
            transactions.collect { list ->
                if (list.isNotEmpty() && aiAdvice.value == null) {
                    generateAIFinancialAdvice()
                }
            }
        }
    }

    // --- Database Operations ---

    fun addTransaction(amount: Double, type: TransactionType, category: Category, note: String, timestamp: Long, currency: String = "CNY", exchangeRate: Double = 1.0) {
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    amount = amount,
                    type = type.name,
                    category = category.name,
                    note = note,
                    timestamp = timestamp,
                    currency = currency,
                    exchangeRate = exchangeRate
                )
            )
            // Regenerate AI feedback on major changes
            generateAIFinancialAdvice()
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
            generateAIFinancialAdvice()
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            generateAIFinancialAdvice()
        }
    }

    fun deleteTransactionById(id: Long) {
        viewModelScope.launch {
            repository.deleteTransactionById(id)
            generateAIFinancialAdvice()
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            repository.clearAllTransactions()
            aiAdvice.value = null
        }
    }

    // Subscriptions
    fun addSubscription(title: String, amount: Double, category: Category, billingCycle: String, nextBillingDate: Long, currency: String = "CNY", exchangeRate: Double = 1.0) {
        viewModelScope.launch {
            repository.insertSubscription(
                Subscription(
                    title = title,
                    amount = amount,
                    category = category.name,
                    billingCycle = billingCycle,
                    nextBillingDate = nextBillingDate,
                    isActive = true,
                    currency = currency,
                    exchangeRate = exchangeRate
                )
            )
        }
    }

    fun updateSubscription(subscription: Subscription) {
        viewModelScope.launch {
            repository.updateSubscription(subscription)
        }
    }

    fun deleteSubscriptionById(id: Long) {
        viewModelScope.launch {
            repository.deleteSubscriptionById(id)
        }
    }

    // --- Quick Template Operations ---

    fun addQuickTemplate(name: String, amount: Double, type: TransactionType, category: Category, note: String, currency: String = "CNY", exchangeRate: Double = 1.0) {
        viewModelScope.launch {
            repository.insertTemplate(
                QuickTemplate(
                    name = name,
                    amount = amount,
                    type = type.name,
                    category = category.name,
                    note = note,
                    currency = currency,
                    exchangeRate = exchangeRate
                )
            )
        }
    }

    fun deleteQuickTemplate(template: QuickTemplate) {
        viewModelScope.launch {
            repository.deleteTemplate(template)
        }
    }

    fun useQuickTemplate(template: QuickTemplate) {
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    amount = template.amount,
                    type = template.type,
                    category = template.category,
                    note = template.note,
                    timestamp = System.currentTimeMillis(),
                    currency = template.currency,
                    exchangeRate = template.exchangeRate
                )
            )
            repository.updateTemplate(
                template.copy(usageCount = template.usageCount + 1)
            )
            generateAIFinancialAdvice()
        }
    }

    // --- AI Operations ---

    /**
     * Pastes raw block of text from users, parses automatically with Gemini REST JSON outputs,
     * and inserts the results into SQLite database in a single sweep.
     */
    fun parseAndAddTransactionsAI(inputText: String, onComplete: (Int) -> Unit) {
        if (inputText.isBlank()) return
        viewModelScope.launch {
            isParsingAI.value = true
            aiParsingError.value = null
            try {
                val parsedList = GeminiService.parseUnstructuredText(inputText)
                if (parsedList.isNotEmpty()) {
                    parsedList.forEach { parsed ->
                        repository.insertTransaction(
                            Transaction(
                                amount = parsed.amount,
                                type = parsed.type,
                                category = parsed.category,
                                note = parsed.note,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                    onComplete(parsedList.size)
                    generateAIFinancialAdvice()
                } else {
                    if (GeminiService.isApiKeyAvailable()) {
                        aiParsingError.value = "未能在输入中解析出账单，请描述得更直白一些吧！"
                    } else {
                        aiParsingError.value = "AI 服务由于缺少 API 密钥不可用，支持添加常规数据。"
                    }
                    onComplete(0)
                }
            } catch (e: Exception) {
                aiParsingError.value = e.localizedMessage
                onComplete(0)
            } finally {
                isParsingAI.value = false
            }
        }
    }

    /**
     * Triggers financial advisory recommendations.
     */
    fun generateAIFinancialAdvice() {
        viewModelScope.launch {
            isAnalyzingAI.value = true
            try {
                val currentTxs = transactions.value
                val currentSubs = subscriptions.value
                val advice = GeminiService.generateFinancialAdvice(currentTxs, currentSubs)
                aiAdvice.value = advice
            } catch (e: Exception) {
                aiAdvice.value = "生成财务分析遇到错误：${e.localizedMessage}"
            } finally {
                isAnalyzingAI.value = false
            }
        }
    }

    // --- Date Helpers ---

    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isThisWeek(time: Long): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = time }
        val now = Calendar.getInstance()
        return cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                cal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR)
    }

    private fun isThisMonth(time: Long): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = time }
        val now = Calendar.getInstance()
        return cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                cal.get(Calendar.MONTH) == now.get(Calendar.MONTH)
    }
}
