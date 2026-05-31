package com.example.data

data class SupportedCurrency(
    val code: String,       // e.g. "CNY", "USD", "EUR"
    val symbol: String,     // e.g. "¥", "$", "€"
    val defaultRate: Double // e.g. 1.0, 7.24, 7.85
)

object CurrencyConfig {
    val list = listOf(
        SupportedCurrency("CNY", "¥", 1.0),
        SupportedCurrency("USD", "$", 7.25),
        SupportedCurrency("EUR", "€", 7.85),
        SupportedCurrency("JPY", "円", 0.046),
        SupportedCurrency("HKD", "HK$", 0.93),
        SupportedCurrency("GBP", "£", 9.20)
    )

    fun getByCode(code: String): SupportedCurrency {
        return list.find { it.code.equalsIgnoreCase(code) } ?: SupportedCurrency(code, code, 1.0)
    }

    private fun String.equalsIgnoreCase(other: String): Boolean {
        return this.equals(other, ignoreCase = true)
    }

    /**
     * Helper to format double values to 2 decimal places with currency symbol prefix.
     */
    fun formatPrice(amount: Double, code: String): String {
        val currency = getByCode(code)
        val formattedAmount = String.format("%.2f", amount)
        return when (currency.code) {
            "JPY" -> "${formattedAmount}${currency.symbol}"
            else -> "${currency.symbol}${formattedAmount}"
        }
    }

    /**
     * Helper to format converted currency amount in CNY easily.
     */
    fun formatToCNY(amountInOriginalCurrency: Double, exchangeRate: Double): String {
        val cnyValue = amountInOriginalCurrency * exchangeRate
        return "¥${String.format("%.2f", cnyValue)}"
    }
}
