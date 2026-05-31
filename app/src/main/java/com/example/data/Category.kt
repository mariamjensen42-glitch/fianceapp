package com.example.data

import androidx.compose.ui.graphics.Color

enum class TransactionType {
    EXPENSE, INCOME
}

enum class Category(
    val displayName: String,
    val colorHex: Long,
    val defaultType: TransactionType
) {
    FOOD("餐饮", 0xFFF59E0B, TransactionType.EXPENSE),
    SHOPPING("购物", 0xFF3B82F6, TransactionType.EXPENSE),
    TRANSPORT("交通", 0xFF10B981, TransactionType.EXPENSE),
    ENTERTAINMENT("娱乐", 0xFFEC4899, TransactionType.EXPENSE),
    BILLS("固定杂费", 0xFF6366F1, TransactionType.EXPENSE),
    SALARY("工资性收入", 0xFF10B981, TransactionType.INCOME),
    INVESTMENT("投资收益", 0xFF8B5CF6, TransactionType.INCOME),
    OTHER("其他账目", 0xFF64748B, TransactionType.EXPENSE);

    val color: Color
        get() = Color(colorHex)

    companion object {
        fun fromName(name: String): Category {
            return values().firstOrNull { 
                it.name.equals(name, ignoreCase = true) || 
                it.displayName == name || 
                name.contains(it.displayName) || 
                it.displayName.contains(name) 
            } ?: OTHER
        }
    }
}
