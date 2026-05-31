package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Structured Data Classes for Parsing ---

@Serializable
data class ParsedTransaction(
    val amount: Double,
    val type: String, // "EXPENSE" or "INCOME"
    val category: String, // "FOOD", "SHOPPING", "TRANSPORT", "ENTERTAINMENT", "BILLS", "SALARY", "INVESTMENT", "OTHER"
    val note: String
)

@Serializable
data class ParsedTransactionList(
    val transactions: List<ParsedTransaction>
)

// --- Request/Response Data Classes matching Gemini REST API ---

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: kotlinx.serialization.json.JsonObject? = null,
    val temperature: Float? = null
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: Content? = null
)

@Serializable
data class GeminiErrorResponse(
    val error: GeminiError? = null
)

@Serializable
data class GeminiError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

// --- Retrofit Interface ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

// --- Service Implementation ---

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonInstance = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val api: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(jsonInstance.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }

    private fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }

    /**
     * Parse error response from Gemini API error body
     */
    private fun parseErrorBody(errorBodyString: String?): String? {
        if (errorBodyString.isNullOrEmpty()) return null
        return try {
            val errorResponse = jsonInstance.decodeFromString<GeminiErrorResponse>(errorBodyString)
            errorResponse.error?.message
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if API Key is set to avoid blind failures.
     */
    fun isApiKeyAvailable(): Boolean {
        val key = getApiKey()
        return key.isNotEmpty() && key != "YOUR_GEMINI_API_KEY_HERE" && !key.startsWith("YOUR_")
    }

    /**
     * Parses natural language spending/earning text into structured transactions.
     * Uses Gemini Structured Outputs JSON schema to guarantee validity.
     */
    suspend fun parseUnstructuredText(text: String): List<ParsedTransaction> = withContext(Dispatchers.IO) {
        if (!isApiKeyAvailable()) {
            Log.w(TAG, "Gemini API key is not configured.")
            return@withContext emptyList()
        }

        // Define the JSON schema for ParsedTransactionList as a JsonObject
        val transactionSchema = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("transactions") {
                    put("type", "ARRAY")
                    putJsonObject("items") {
                        put("type", "OBJECT")
                        putJsonObject("properties") {
                            putJsonObject("amount") {
                                put("type", "NUMBER")
                                put("description", "金额，如果是支出或收入，都是正数。")
                            }
                            putJsonObject("type") {
                                put("type", "STRING")
                                putJsonArray("enum") {
                                    add(Json.parseToJsonElement("\"EXPENSE\""))
                                    add(Json.parseToJsonElement("\"INCOME\""))
                                }
                                put("description", "分类别：支出是EXPENSE，收入是INCOME。")
                            }
                            putJsonObject("category") {
                                put("type", "STRING")
                                putJsonArray("enum") {
                                    add(Json.parseToJsonElement("\"FOOD\""))
                                    add(Json.parseToJsonElement("\"SHOPPING\""))
                                    add(Json.parseToJsonElement("\"TRANSPORT\""))
                                    add(Json.parseToJsonElement("\"ENTERTAINMENT\""))
                                    add(Json.parseToJsonElement("\"BILLS\""))
                                    add(Json.parseToJsonElement("\"SALARY\""))
                                    add(Json.parseToJsonElement("\"INVESTMENT\""))
                                    add(Json.parseToJsonElement("\"OTHER\""))
                                }
                                put("description", "记账类别枚举，选择最匹配的一项")
                            }
                            putJsonObject("note") {
                                put("type", "STRING")
                                put("description", "账目备注描述，比如‘买零食’‘打车去公司’‘发基本工资’。")
                            }
                        }
                        putJsonArray("required") {
                            add(Json.parseToJsonElement("\"amount\""))
                            add(Json.parseToJsonElement("\"type\""))
                            add(Json.parseToJsonElement("\"category\""))
                            add(Json.parseToJsonElement("\"note\""))
                        }
                    }
                }
            }
            putJsonArray("required") {
                add(Json.parseToJsonElement("\"transactions\""))
            }
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(
                            text = """
                            请分析提取以下关于收支记账的自由文本，并将其解析为规范的JSON账目列表：
                            
                            $text
                            """.trimIndent()
                        )
                    )
                )
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                responseSchema = transactionSchema,
                temperature = 0.1f
            ),
            systemInstruction = Content(
                parts = listOf(
                    Part(
                        text = "你是一个精确的财务记账助手，专门从口语或零散的简短句子中提取支出与收入账目。只需返回遵守指定JSON Schema的交易对象列表。"
                    )
                )
            )
        )

        try {
            val response = api.generateContent(getApiKey(), request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!jsonText.isNullOrEmpty()) {
                val parsed = jsonInstance.decodeFromString<ParsedTransactionList>(jsonText)
                return@withContext parsed.transactions
            }
            emptyList()
        } catch (e: Exception) {
            val rawErrorMsg = if (e is retrofit2.HttpException) {
                try {
                    val errorBody = e.response()?.errorBody()?.string()
                    parseErrorBody(errorBody) ?: e.localizedMessage
                } catch (ex: Exception) {
                    e.localizedMessage
                }
            } else {
                e.localizedMessage
            }
            Log.e(TAG, "Error parsing unstructured text via Gemini: $rawErrorMsg", e)
            emptyList()
        }
    }

    /**
     * Generates analytical suggestions and money management tips based on current spend records.
     */
    suspend fun generateFinancialAdvice(
        transactions: List<Transaction>,
        subscriptions: List<Subscription>
    ): String = withContext(Dispatchers.IO) {
        if (!isApiKeyAvailable()) {
            return@withContext "⚠️ AI 记账分析需要有效的 Gemini API 密钥。如果您已在此平台成功设置密钥，该分析将会自动运行并更新。\n\n提示：您可以点击【设置】查看当前状态，或使用底部的 [AI 助记] 进行离线常规理财辅导。"
        }

        val expenses = transactions.filter { it.type == "EXPENSE" }
        val incomes = transactions.filter { it.type == "INCOME" }
        val totalExpenses = expenses.sumOf { it.amount }
        val totalIncomes = incomes.sumOf { it.amount }
        val totalSubscriptions = subscriptions.filter { it.isActive }.sumOf {
            if (it.billingCycle == "MONTHLY") it.amount else (it.amount / 12.0)
        }

        val historyText = expenses.take(30).joinToString("\n") {
            "- ${it.category}：支出 ${it.amount} 元 (备注: ${it.note})"
        }
        val subText = subscriptions.joinToString("\n") {
            "- ${it.title}：${it.amount}/月 (周期: ${it.billingCycle}, 状态: ${if (it.isActive) "活跃" else "静止"})"
        }

        val prompt = """
            请根据以下记账数据，为用户生成一份150-200字以内的极简、实用且条理清晰的专属财务健康分析与省钱建议：
            
            1. 本期总收入：$totalIncomes 元
            2. 本期总支出：$totalExpenses 元
            3. 周期订阅开销（折合每月）：$totalSubscriptions 元/月
            
            近期支出明细：
            $historyText
            
            活跃周期性订阅：
            $subText
            
            请：
            - 用中文书写。
            - 以鼓舞人心且专业客观的语气分析。
            - 针对最频繁或最大额的支出类别（如餐饮、购物、娱乐等）给出一到两个切实、具体的改进建议。
            - 评估订阅项目开销占比是否过大。
            - 避免空洞套话，直奔主题。
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.5f),
            systemInstruction = Content(
                parts = listOf(
                    Part(
                        text = "你是一个贴心而理性的财务规划专家。你的语言亲切、紧凑且充满洞察力。"
                    )
                )
            )
        )

        try {
            val response = api.generateContent(getApiKey(), request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "您的财务状况非常平稳。开启您的第一笔记账来让 AI 分析出有价值的理财建议吧！"
        } catch (e: Exception) {
            Log.e(TAG, "Error generating financial advice", e)
            val rawErrorMsg = if (e is retrofit2.HttpException) {
                try {
                    val errorBody = e.response()?.errorBody()?.string()
                    parseErrorBody(errorBody) ?: e.localizedMessage
                } catch (ex: Exception) {
                    e.localizedMessage
                }
            } else {
                e.localizedMessage
            }

            val friendlyMsg = when {
                rawErrorMsg.contains("location is not supported", ignoreCase = true) ||
                rawErrorMsg.contains("location not supported", ignoreCase = true) ||
                rawErrorMsg.contains("not supported in your country", ignoreCase = true) -> {
                    "当前所在国家/地区暂不支持 Google Gemini 智能服务，或 API 密钥受区域访问限制 ($rawErrorMsg)"
                }
                rawErrorMsg.contains("API key not valid", ignoreCase = true) ||
                rawErrorMsg.contains("API key is blocked", ignoreCase = true) ||
                rawErrorMsg.contains("API key is invalid", ignoreCase = true) -> {
                    "您的 API 密钥已被禁用、无效或输入有误，请在 AI Studio Secrets 面板重新设置 ($rawErrorMsg)"
                }
                rawErrorMsg.contains("quota", ignoreCase = true) ||
                rawErrorMsg.contains("limit exceeded", ignoreCase = true) -> {
                    "当前 Gemini API 配额超出限制，请稍后再试 ($rawErrorMsg)"
                }
                else -> rawErrorMsg
            }

            "AI 分析功能暂时不可用 ($friendlyMsg)。请检查您的网络连接、API 密钥配置或稍后再试。"
        }
    }
}
