package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Info
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class LedgerStats(
    val totalIncome: Double,
    val totalExpense: Double,
    val netBalance: Double
)

data class CategoryBreakdown(
    val categoryId: String,
    val amount: Double,
    val percentage: Float
)

data class DailyTrendPoint(
    val dateLabel: String,
    val income: Double,
    val expense: Double
)

class LedgerViewModel(private val repository: TransactionRepository) : ViewModel() {

    // Main transactions flow
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

    // Compute balance statistics
    val stats: StateFlow<LedgerStats> = transactions.map { list ->
        val income = list.filter { it.type == "INCOME" }.sumOf { it.amount }
        val expense = list.filter { it.type == "EXPENSE" && it.reimbursementStatus != "REIMBURSED" }.sumOf { it.amount }
        LedgerStats(
            totalIncome = income,
            totalExpense = expense,
            netBalance = income - expense
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LedgerStats(0.0, 0.0, 0.0)
    )

    // Compute category breakdown for Expense
    val expenseBreakdown: StateFlow<List<CategoryBreakdown>> = transactions.map { list ->
        val expenses = list.filter { it.type == "EXPENSE" && it.reimbursementStatus != "REIMBURSED" }
        val totalExp = expenses.sumOf { it.amount }
        if (totalExp == 0.0) return@map emptyList()

        expenses.groupBy { it.category }
            .map { (catId, items) ->
                val sum = items.sumOf { it.amount }
                CategoryBreakdown(
                    categoryId = catId,
                    amount = sum,
                    percentage = (sum / totalExp).toFloat()
                )
            }
            .sortedByDescending { it.amount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Compute category breakdown for Income
    val incomeBreakdown: StateFlow<List<CategoryBreakdown>> = transactions.map { list ->
        val incomes = list.filter { it.type == "INCOME" }
        val totalInc = incomes.sumOf { it.amount }
        if (totalInc == 0.0) return@map emptyList()

        incomes.groupBy { it.category }
            .map { (catId, items) ->
                val sum = items.sumOf { it.amount }
                CategoryBreakdown(
                    categoryId = catId,
                    amount = sum,
                    percentage = (sum / totalInc).toFloat()
                )
            }
            .sortedByDescending { it.amount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Compute last 7 days daily trend of income and expenses chronologically
    val dailyTrend: StateFlow<List<DailyTrendPoint>> = transactions.map { list ->
        val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        
        // Build last 7 days labels
        val last7Days = (0..6).map { i ->
            val cal = calendar.clone() as Calendar
            cal.add(Calendar.DAY_OF_YEAR, -i)
            sdf.format(cal.time)
        }.reversed()

        // Group transactions by date
        val groupedByDate = list.groupBy { sdf.format(Date(it.timestamp)) }

        last7Days.map { dateLabel ->
            val dayTxs = groupedByDate[dateLabel] ?: emptyList()
            val inc = dayTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
            val exp = dayTxs.filter { it.type == "EXPENSE" && it.reimbursementStatus != "REIMBURSED" }.sumOf { it.amount }
            DailyTrendPoint(
                dateLabel = dateLabel,
                income = inc,
                expense = exp
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Compute active tag list from all transactions
    val activeTags: StateFlow<List<String>> = transactions.map { list ->
        list.flatMap { tx ->
            tx.tags.split(Regex("\\s+"))
                .map { it.trim() }
                .filter { it.startsWith("#") && it.length > 1 }
        }.distinct().sorted()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dynamic categories flows loading custom categories from Room
    val allExpenseCategories: StateFlow<List<CategoryDef>> = repository.allCustomCategories.map { customList ->
        val list = CategoryRegistry.expenseCategories.toMutableList()
        customList.filter { it.isExpense }.forEach { custom ->
            list.add(
                CategoryDef(
                    id = "Custom_${custom.id}",
                    name = custom.name,
                    icon = Icons.Default.Info, // Default styled icon
                    color = Color(0xFF9C27B0), // Purple for custom
                    type = "EXPENSE"
                )
            )
        }
        list
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = CategoryRegistry.expenseCategories)

    val allIncomeCategories: StateFlow<List<CategoryDef>> = repository.allCustomCategories.map { customList ->
        val list = CategoryRegistry.incomeCategories.toMutableList()
        customList.filter { !it.isExpense }.forEach { custom ->
            list.add(
                CategoryDef(
                    id = "Custom_${custom.id}",
                    name = custom.name,
                    icon = Icons.Default.Info,
                    color = Color(0xFF009688), // Teal for custom
                    type = "INCOME"
                )
            )
        }
        list
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = CategoryRegistry.incomeCategories)

    // Budgets flow mapping database entities
    val budgets: StateFlow<List<Budget>> = repository.allBudgets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalBudget: StateFlow<Double?> = budgets.map { list ->
        list.firstOrNull { it.id == "total" }?.amount
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = null)

    val categoryBudgets: StateFlow<Map<String, Double>> = budgets.map { list ->
        list.filter { it.id.startsWith("category_") }
            .associate { it.id.removePrefix("category_") to it.amount }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyMap())

    // Monthly total expenses for the current system month
    val currentMonthExpenseTotal: StateFlow<Double> = transactions.map { list ->
        val cal = Calendar.getInstance()
        val nowMonth = cal.get(Calendar.MONTH)
        val nowYear = cal.get(Calendar.YEAR)
        
        list.filter { 
            if (it.type != "EXPENSE" || it.reimbursementStatus == "REIMBURSED") {
                false
            } else {
                val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                c.get(Calendar.MONTH) == nowMonth && c.get(Calendar.YEAR) == nowYear
            }
        }.sumOf { it.amount }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0.0)

    // Category specific monthly expenses for current system month
    val currentMonthCategoryExpenses: StateFlow<Map<String, Double>> = transactions.map { list ->
        val cal = Calendar.getInstance()
        val nowMonth = cal.get(Calendar.MONTH)
        val nowYear = cal.get(Calendar.YEAR)
        
        list.filter { 
            if (it.type != "EXPENSE" || it.reimbursementStatus == "REIMBURSED") {
                false
            } else {
                val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                c.get(Calendar.MONTH) == nowMonth && c.get(Calendar.YEAR) == nowYear
            }
        }
        .groupBy { it.category }
        .mapValues { (_, txs) -> txs.sumOf { it.amount } }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyMap())

    // Reimbursement aggregate calculations
    val pendingReimbursementAmount: StateFlow<Double> = transactions.map { list ->
        list.filter { it.type == "EXPENSE" && it.isReimbursable && it.reimbursementStatus == "PENDING" }.sumOf { it.amount }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0.0)

    val reimbursedAmount: StateFlow<Double> = transactions.map { list ->
        list.filter { it.type == "EXPENSE" && it.isReimbursable && it.reimbursementStatus == "REIMBURSED" }.sumOf { it.amount }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0.0)

    fun toggleReimbursementStatus(transaction: Transaction) {
        viewModelScope.launch {
            if (transaction.isReimbursable) {
                val newStatus = when (transaction.reimbursementStatus) {
                    "PENDING" -> "REIMBURSED"
                    "REIMBURSED" -> "PENDING"
                    else -> "PENDING"
                }
                repository.update(transaction.copy(reimbursementStatus = newStatus))
            }
        }
    }

    fun toggleIsReimbursable(transaction: Transaction) {
        viewModelScope.launch {
            val isReimbursable = !transaction.isReimbursable
            val newStatus = if (isReimbursable) "PENDING" else "NONE"
            repository.update(transaction.copy(isReimbursable = isReimbursable, reimbursementStatus = newStatus))
        }
    }

    fun updateBudget(id: String, amount: Double) {
        viewModelScope.launch {
            if (amount <= 0) {
                repository.deleteBudget(id)
            } else {
                repository.insertBudget(Budget(id, amount))
            }
        }
    }

    fun getCategoryDef(id: String): CategoryDef? {
        val staticCat = CategoryRegistry.getCategoryById(id)
        if (staticCat != null) return staticCat
        
        val expCat = allExpenseCategories.value.firstOrNull { it.id == id }
        if (expCat != null) return expCat

        val incCat = allIncomeCategories.value.firstOrNull { it.id == id }
        if (incCat != null) return incCat

        return null
    }

    var customCategoryNameInput by mutableStateOf("")

    fun addCustomCategory(isExpense: Boolean) {
        val name = customCategoryNameInput.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            repository.insertCustomCategory(CustomCategory(name = name, isExpense = isExpense))
            customCategoryNameInput = ""
        }
    }

    // UI Input states for the ADD screen
    var amountInput by mutableStateOf("")
    var titleInput by mutableStateOf("")
    var typeInput by mutableStateOf("EXPENSE") // "EXPENSE", "INCOME", "SUBSCRIPTION"
    var categoryInput by mutableStateOf("Food") // Category ID
    var cycleInput by mutableStateOf("MONTHLY") // "MONTHLY" or "YEARLY"
    var notesInput by mutableStateOf("")
    var tagsInput by mutableStateOf("")
    var accountInput by mutableStateOf("微信钱包") // Default account
    var timestampInput by mutableStateOf(System.currentTimeMillis())
    var isReimbursableInput by mutableStateOf(false)
    var reimbursementStatusInput by mutableStateOf("PENDING") // "PENDING", "REIMBURSED"

    // AI smart bookkeeping state fields
    var aiInputText by mutableStateOf("")
    var isAiLoading by mutableStateOf(false)
    var aiParsedResult by mutableStateOf<ParsedLedgerItem?>(null)
    var aiErrorMessage by mutableStateOf("")

    // AI image parsing state fields
    var isAiImageLoading by mutableStateOf(false)
    var aiImageParsedResult by mutableStateOf<ParsedLedgerItem?>(null)
    var aiImageErrorMessage by mutableStateOf("")

    // AI financial report states
    var aiReportContent by mutableStateOf("")
    var isGeneratingReport by mutableStateOf(false)
    var reportErrorMessage by mutableStateOf("")

    // UI navigation tabs in single-screen
    var currentTab by mutableStateOf(0) // 0: Transactions, 1: Subscriptions, 2: Add New, 3: Analytics

    fun updateSelectedType(type: String) {
        typeInput = type
        categoryInput = if (type == "EXPENSE" || type == "SUBSCRIPTION") {
            allExpenseCategories.value.firstOrNull()?.id ?: "Food"
        } else {
            allIncomeCategories.value.firstOrNull()?.id ?: "Salary"
        }
    }

    fun formatTags(rawTags: String, title: String): String {
        val tagList = mutableSetOf<String>()
        
        // Parse from raw tag input
        rawTags.split(Regex("[,，\\s]+")).map { it.trim() }.filter { it.isNotEmpty() }.forEach { tag ->
            val cleaned = if (tag.startsWith("#")) tag else "#$tag"
            tagList.add(cleaned)
        }
        
        // Extract hashtags from title
        Regex("#\\S+").findAll(title).forEach { match ->
            tagList.add(match.value)
        }
        
        return tagList.joinToString(" ")
    }

    fun saveTransaction(): Boolean {
        val amt = amountInput.toDoubleOrNull() ?: return false
        if (amt <= 0) return false
        val finalTitle = titleInput.trim().ifEmpty { 
            CategoryRegistry.getCategoryById(categoryInput)?.name ?: "记账记录"
        }

        val formattedTags = formatTags(tagsInput, finalTitle)

        viewModelScope.launch {
            if (typeInput == "SUBSCRIPTION") {
                val sub = Subscription(
                    title = finalTitle,
                    amount = amt,
                    cycle = cycleInput,
                    category = categoryInput,
                    nextBillingDate = timestampInput,
                    notes = notesInput.trim()
                )
                repository.insertSubscription(sub)
                resetForm()
                currentTab = 1 // Subscriptions tab
            } else {
                val tx = Transaction(
                    title = finalTitle,
                    amount = amt,
                    type = typeInput,
                    category = categoryInput,
                    timestamp = timestampInput,
                    notes = notesInput.trim(),
                    tags = formattedTags,
                    account = accountInput,
                    isReimbursable = if (typeInput == "EXPENSE") isReimbursableInput else false,
                    reimbursementStatus = if (typeInput == "EXPENSE" && isReimbursableInput) reimbursementStatusInput else "NONE"
                )
                repository.insert(tx)
                resetForm()
                currentTab = 0 // Transactions tab
            }
        }
        return true
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun deleteSubscription(id: Int) {
        viewModelScope.launch {
            repository.deleteSubscriptionById(id)
        }
    }

    fun renewSubscription(subscription: Subscription) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = subscription.nextBillingDate
            if (subscription.cycle == "MONTHLY") {
                calendar.add(Calendar.MONTH, 1)
            } else {
                calendar.add(Calendar.YEAR, 1)
            }
            val renewedSub = subscription.copy(nextBillingDate = calendar.timeInMillis)
            repository.insertSubscription(renewedSub)

            val tx = Transaction(
                title = "订阅续费: ${subscription.title}",
                amount = subscription.amount,
                type = "EXPENSE",
                category = subscription.category,
                timestamp = System.currentTimeMillis(),
                notes = "由订阅计划「${subscription.title}」续期记账自动生成"
            )
            repository.insert(tx)
        }
    }

    fun resetForm() {
        amountInput = ""
        titleInput = ""
        typeInput = "EXPENSE"
        categoryInput = "Food"
        cycleInput = "MONTHLY"
        notesInput = ""
        tagsInput = ""
        accountInput = "微信钱包"
        timestampInput = System.currentTimeMillis()
        isReimbursableInput = false
        reimbursementStatusInput = "PENDING"
    }

    // Interactive helper: Fill in demo transactions so user enjoys visual stats right off the bat!
    fun loadSampleData() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val dayMs = 24 * 60 * 60 * 1000L

            val list = listOf(
                Transaction(title = "晚餐火锅", amount = 120.0, type = "EXPENSE", category = "Food", timestamp = now, tags = "#端午旅行 #美食", account = "微信钱包"),
                Transaction(title = "地铁出行", amount = 5.0, type = "EXPENSE", category = "Transport", timestamp = now - 2 * 3600 * 1000L, tags = "#端午旅行", account = "支付宝"),
                Transaction(title = "每月发薪", amount = 8500.0, type = "INCOME", category = "Salary", timestamp = now - 1 * dayMs, account = "储蓄卡"),
                Transaction(title = "超市采购", amount = 145.5, type = "EXPENSE", category = "Food", timestamp = now - 1 * dayMs, tags = "#家庭常备", account = "微信钱包"),
                Transaction(title = "理财收益", amount = 88.0, type = "INCOME", category = "Investment", timestamp = now - 2 * dayMs, account = "支付宝"),
                Transaction(title = "购买新衣", amount = 299.0, type = "EXPENSE", category = "Shopping", timestamp = now - 2 * dayMs, tags = "#端午旅行 #夏装购买", account = "信用卡"),
                Transaction(title = "看电影", amount = 70.0, type = "EXPENSE", category = "Entertainment", timestamp = now - 3 * dayMs, account = "微信钱包"),
                Transaction(title = "项目奖金", amount = 1500.0, type = "INCOME", category = "Bonus", timestamp = now - 4 * dayMs, account = "储蓄卡"),
                Transaction(title = "房租水电", amount = 1800.0, type = "EXPENSE", category = "Housing", timestamp = now - 5 * dayMs, account = "储蓄卡"),
                Transaction(title = "打车办事", amount = 35.0, type = "EXPENSE", category = "Transport", timestamp = now - 6 * dayMs, tags = "#工作报销", account = "支付宝", isReimbursable = true, reimbursementStatus = "PENDING")
            )

            val subs = listOf(
                Subscription(title = "ChatGPT AI 会员", amount = 140.0, cycle = "MONTHLY", category = "Digital", nextBillingDate = now + 14 * dayMs),
                Subscription(title = "视频流媒体年度订阅", amount = 198.0, cycle = "YEARLY", category = "Entertainment", nextBillingDate = now + 120 * dayMs),
                Subscription(title = "云存储扩展", amount = 28.0, cycle = "MONTHLY", category = "Digital", nextBillingDate = now + 5 * dayMs)
            )

            for (tx in list) {
                repository.insert(tx)
            }
            for (sub in subs) {
                repository.insertSubscription(sub)
            }
            repository.insertBudget(Budget("total", 3000.0))
            repository.insertBudget(Budget("category_Food", 1000.0))
            repository.insertBudget(Budget("category_Shopping", 500.0))
        }
    }

    fun parseInputWithAi(userInput: String) {
        val trimmedInput = userInput.trim()
        if (trimmedInput.isEmpty()) return
        
        aiErrorMessage = ""
        aiParsedResult = null
        isAiLoading = true

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            aiErrorMessage = "请在右侧面板配置你的 GEMINI_API_KEY"
            isAiLoading = false
            return
        }

        val sysInstructionText = """
            你是一个精确的中文个人记账助手。请分析用户的输入，提取记账信息并转换为严格标准的JSON数据对象，不包含任何外部格式装饰。
            当前记账系统支持的分类ID（及其对应关系）：
            支出类分类：
            - Food : 餐饮 (如吃饭,点外卖,买菜,星巴克等)
            - Shopping : 购物 (如买衣服,买鞋,天猫淘宝,买包包,生活用品等)
            - Transport : 交通 (如地铁,打车,加油,共享单车,公交车等)
            - Digital : 数字订阅 (如ChatGPT,网易云会员,爱奇艺,iCloud,B站大会员等)
            - Entertainment : 娱乐 (如看电影,唱歌,玩游戏,密室逃脱等)
            - Housing : 居住 (如房租,房贷,水电费,物业费等)
            - Others_Exp : 其他支出

            收入类分类：
            - Salary : 工资
            - Bonus : 奖金 (如年终奖,季度奖,项目奖金等)
            - Investment : 理财 (如股票收益,理财利息,基金收益等)
            - Others_Inc : 其他收入

            你的输出必须是可以用 JSON 解析器直接解析的规范 JSON 对象，它必须涵盖以下所有字段：
            1. "type": 字符串。值只能是 "EXPENSE" (一般支出), "INCOME" (一般收入), 或 "SUBSCRIPTION" (如果是数字会员、自动延期扣款等周期订阅)。
            2. "amount": 双精度浮点数。具体的金额（如果用户口语表达没有提供具体金额或无法确定，设为 0.0，且忽略货币符号如元/RMB）。
            3. "title": 字符串。记账的主要项目/备注名称（如："外卖午餐"、"地铁通勤"、"ChatGPT Plus 订阅"、"5月份工资" 等）。
            4. "category": 字符串。必须精确对应上述分类ID之一（Food、Shopping、Transport、Digital、Entertainment、Housing、Others_Exp、Salary、Bonus、Investment、Others_Inc）。如果无法界定，支出类记为 "Others_Exp"、收入类记为 "Others_Inc"。
            5. "cycle": 字符串。如果 "type" 是 "SUBSCRIPTION"，此处必须设为 "MONTHLY" (按月订阅) 或 "YEARLY" (按年订阅)，否则设为 null 或 omitted。
            6. "dateOffsetDays": 整数。基于今天，发生的相对偏移天数。
               例如：今天=0，昨天=-1，前天=-2，3天前=-3，4天前=-4，以此类推。
            7. "notes": 字符串。存储用户输入的原始短语，便于备注。
            8. "account": 字符串。支付账户或资产源，限定选择 "微信钱包", "支付宝", "储蓄卡", "信用卡", "现金"。如果无法推断默认填 "微信钱包"。

            切记：你只能返回纯 JSON，没有任何 markdown 符号包装（绝不能带有 ```json 和 ```）。
        """.trimIndent()

        viewModelScope.launch {
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = trimmedInput)
                        )
                    )
                ),
                systemInstruction = GeminiContent(
                    parts = listOf(
                        GeminiPart(text = sysInstructionText)
                    )
                ),
                generationConfig = GeminiConfig(
                    responseMimeType = "application/json",
                    temperature = 0.1f
                )
            )

            try {
                val response = GeminiRetrofitClient.service.generateContent(apiKey, request)
                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (rawText != null) {
                    val parsedItem = try {
                        GeminiRetrofitClient.parsedLedgerItemAdapter.fromJson(rawText)
                    } catch (e: Exception) {
                        val cleaned = rawText.trim()
                            .removePrefix("```json")
                            .removePrefix("```")
                            .removeSuffix("```")
                            .trim()
                        GeminiRetrofitClient.parsedLedgerItemAdapter.fromJson(cleaned)
                    }

                    if (parsedItem != null) {
                        aiParsedResult = parsedItem
                    } else {
                        aiErrorMessage = "智能解析遇到阻碍，请检查输入"
                    }
                } else {
                    aiErrorMessage = "智能助手在打盹，请稍后再试"
                }
            } catch (e: Exception) {
                aiErrorMessage = "网络连接故障: ${e.localizedMessage ?: "未知网络错误"}"
            } finally {
                isAiLoading = false
            }
        }
    }

    private fun android.graphics.Bitmap.toBase64(): String {
        val outputStream = java.io.ByteArrayOutputStream()
        compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
        return android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)
    }

    fun parseImageWithAi(bitmap: android.graphics.Bitmap) {
        aiImageErrorMessage = ""
        aiImageParsedResult = null
        isAiImageLoading = true

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            aiImageErrorMessage = "请配置 GEMINI_API_KEY"
            isAiImageLoading = false
            return
        }

        val sysInstructionText = """
            你是一个精确的中文个人记账助手。请分析用户上传的截图（可能是账单、小票、支付成功截图等），提取记账信息并转换为严格标准的JSON数据对象。
            当前记账系统支持的分类：Food（餐饮）, Shopping（购物）, Transport（交通）, Digital（数字订阅）, Entertainment（娱乐）, Housing（居住）, Others_Exp（其他支出）, Salary（工资）, Bonus（奖金）, Investment（理财）, Others_Inc（其他收入）。
            
            输出必须为纯JSON，包含：
            "type" ("EXPENSE", "INCOME", "SUBSCRIPTION")
            "amount" (双精度浮点数，交易金额)
            "title" (交易说明/商户名)
            "category" (上述分类之一)
            "dateOffsetDays" (基于今天的相对天数，通常截图是今天发生的设为0)
            "notes" (任何额外备注，或者截图上的时间信息)
            "account" (支付账户，如"微信钱包", "支付宝", "储蓄卡", "信用卡", "现金"，推断不出就填 "微信钱包")

            切记：只返回纯 json 对象内容，不带 ```json 或任何包装。
        """.trimIndent()

        viewModelScope.launch {
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = "请提取这张账单截图中的记账信息"),
                            GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = bitmap.toBase64()))
                        )
                    )
                ),
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = sysInstructionText))
                ),
                generationConfig = GeminiConfig(
                    responseMimeType = "application/json",
                    temperature = 0.1f
                )
            )

            try {
                val response = GeminiRetrofitClient.service.generateContent(apiKey, request)
                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (rawText != null) {
                    val parsedItem = try {
                        val cleaned = rawText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                        GeminiRetrofitClient.parsedLedgerItemAdapter.fromJson(cleaned)
                    } catch (e: Exception) {
                        null
                    }

                    if (parsedItem != null) {
                        aiImageParsedResult = parsedItem
                    } else {
                        aiImageErrorMessage = "无法解析账单截图中的文字"
                    }
                } else {
                    aiImageErrorMessage = "模型没有返回任何内容"
                }
            } catch (e: Exception) {
                aiImageErrorMessage = "网络连接故障: ${e.localizedMessage}"
            } finally {
                isAiImageLoading = false
            }
        }
    }

    fun confirmAndSaveAiImageResult() {
        val result = aiImageParsedResult ?: return
        viewModelScope.launch {
            val targetTime = if (result.dateOffsetDays != null && result.dateOffsetDays != 0) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, result.dateOffsetDays)
                cal.timeInMillis
            } else {
                System.currentTimeMillis()
            }

            if (result.type == "SUBSCRIPTION") {
                val sub = Subscription(
                    title = result.title.ifEmpty { "截图记账" },
                    amount = result.amount,
                    cycle = result.cycle ?: "MONTHLY",
                    category = result.category,
                    nextBillingDate = targetTime,
                    notes = result.notes ?: ""
                )
                repository.insertSubscription(sub)
                currentTab = 1
            } else {
                val tx = Transaction(
                    title = result.title.ifEmpty { "截图记账" },
                    amount = result.amount,
                    type = result.type,
                    category = result.category,
                    timestamp = targetTime,
                    notes = result.notes ?: "",
                    account = result.account ?: "微信钱包"
                )
                repository.insert(tx)
                currentTab = 0
            }

            aiImageParsedResult = null
        }
    }

    fun cancelAiImageResult() {
        aiImageParsedResult = null
    }

    fun confirmAndSaveAiResult() {
        val result = aiParsedResult ?: return
        viewModelScope.launch {
            val targetTime = if (result.dateOffsetDays != null && result.dateOffsetDays != 0) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, result.dateOffsetDays)
                cal.timeInMillis
            } else {
                System.currentTimeMillis()
            }

            if (result.type == "SUBSCRIPTION") {
                val sub = Subscription(
                    title = result.title.ifEmpty { "一句话记账" },
                    amount = result.amount,
                    cycle = result.cycle ?: "MONTHLY",
                    category = result.category,
                    nextBillingDate = targetTime,
                    notes = result.notes ?: ""
                )
                repository.insertSubscription(sub)
                currentTab = 1 // Switch to Subscriptions
            } else {
                val tx = Transaction(
                    title = result.title.ifEmpty { "一句话记账" },
                    amount = result.amount,
                    type = result.type,
                    category = result.category,
                    timestamp = targetTime,
                    notes = result.notes ?: "",
                    account = result.account ?: "微信钱包"
                )
                repository.insert(tx)
                currentTab = 0 // Switch to Transactions
            }

            aiInputText = ""
            aiParsedResult = null
        }
    }

    fun cancelAiResult() {
        aiParsedResult = null
    }

    fun generateFinancialReport() {
        val currentTxs = transactions.value
        val currentSubs = subscriptions.value
        
        if (currentTxs.isEmpty() && currentSubs.isEmpty()) {
            reportErrorMessage = "目前没有记账数据，请先记上几笔吧！"
            return
        }

        reportErrorMessage = ""
        isGeneratingReport = true
        aiReportContent = ""

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            reportErrorMessage = "请在右侧面板配置你的 GEMINI_API_KEY"
            isGeneratingReport = false
            return
        }

        // Prepare context
        val txsText = currentTxs.take(30).joinToString("\n") { tx ->
            val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(tx.timestamp))
            "- [${tx.type}] $dateStr ${tx.title} (${tx.category}): ¥${tx.amount} ${tx.notes ?: ""}"
        }

        val subsText = currentSubs.joinToString("\n") { sub ->
            "- [订阅] ${sub.title} (${sub.category}): 周期费用 ¥${sub.amount} (${sub.cycle}) ${sub.notes ?: ""}"
        }

        val sysInstructionText = """
            你是一个专业且风趣智能的个人财务规划、理财与审计专家。
            请分析用户的记账数据，给出一份高质量、结构清晰、极具实操省钱建议的《AI 财务透视与省钱报告》。
            
            报告应该包含：
            1. 📊 【收支大盘与健康度诊断】：客观评价当前的收支和结余状态，评出财务健康星级（如: ⭐⭐⭐）。
            2. 🔍 【消费黑洞漏洞透视】：指出哪些支出项或者类别占比最高，甚至可能是不必要的冲动消费。
            3. 💡 【极客精细省钱妙招】：针对该用户的特定消费习惯，提出3点非常有针对性、具体且好落地的省钱可行建议。
            4. 🌟 【金石理财寄语】：一句温暖、富有远见且坚定的理财金句。

            语言风格：专业、睿智、风趣、富有亲和力，多用可视化表情符号装饰，排版优美，结构清晰，使用 markdown 格式输出。
        """.trimIndent()

        val promptText = """
            这是我最近的账目数据：
            
            [普通账单记录]
            $txsText
            
            [自动续期订阅记录]
            $subsText
            
            请为我生成专属的《AI 财务透视与省钱报告》：
        """.trimIndent()

        viewModelScope.launch {
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = promptText)
                        )
                    )
                ),
                systemInstruction = GeminiContent(
                    parts = listOf(
                        GeminiPart(text = sysInstructionText)
                    )
                ),
                generationConfig = GeminiConfig(
                    temperature = 0.7f
                )
            )

            try {
                val response = GeminiRetrofitClient.service.generateContent(apiKey, request)
                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (rawText != null) {
                    aiReportContent = rawText
                } else {
                    reportErrorMessage = "AI 正在思考，但未返回内容，请稍后再试"
                }
            } catch (e: Exception) {
                reportErrorMessage = "网络连接故障: ${e.localizedMessage ?: "未知错误"}"
            } finally {
                isGeneratingReport = false
            }
        }
    }
}

class LedgerViewModelFactory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LedgerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LedgerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
