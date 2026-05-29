package com.example.data

import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryDef(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val type: String // "EXPENSE" or "INCOME"
)

object CategoryRegistry {
    val expenseCategories = listOf(
        CategoryDef("Food", "餐饮", Icons.Default.Favorite, Color(0xFFC85A32), "EXPENSE"),
        CategoryDef("Shopping", "购物", Icons.Default.ShoppingCart, Color(0xFFA58D5F), "EXPENSE"),
        CategoryDef("Transport", "交通", Icons.AutoMirrored.Filled.List, Color(0xFF386A20), "EXPENSE"),
        CategoryDef("Digital", "数字订阅", Icons.Default.PlayArrow, Color(0xFF5C6BC0), "EXPENSE"),
        CategoryDef("Entertainment", "娱乐", Icons.Default.Star, Color(0xFF8C5C82), "EXPENSE"),
        CategoryDef("Housing", "居住", Icons.Default.Home, Color(0xFF865D36), "EXPENSE"),
        CategoryDef("Others_Exp", "其他支出", Icons.Default.Info, Color(0xFF4C7D8A), "EXPENSE")
    )

    val incomeCategories = listOf(
        CategoryDef("Salary", "工资", Icons.Default.AccountCircle, Color(0xFF4E773E), "INCOME"),
        CategoryDef("Bonus", "奖金", Icons.Default.Star, Color(0xFFD49E2A), "INCOME"),
        CategoryDef("Investment", "理财", Icons.Default.Settings, Color(0xFF427A7B), "INCOME"),
        CategoryDef("Others_Inc", "其他收入", Icons.Default.Info, Color(0xFF788374), "INCOME")
    )

    fun getCategoryById(id: String): CategoryDef? {
        return (expenseCategories + incomeCategories).firstOrNull { it.id == id }
    }
}
