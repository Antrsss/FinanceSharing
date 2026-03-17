package com.example.financesharing.domain.model

enum class Currency(
    val code: String,
    val symbol: String
) {
    USD(code = "USD", symbol = "$"),
    EUR(code = "EUR", symbol = "€"),
    RUB(code = "RUB", symbol = "₽"),
    BYN(code = "BYN", symbol = "Br");

    override fun toString(): String = code

    companion object {
        fun fromCode(code: String?): Currency {
            if (code.isNullOrBlank()) return RUB
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: RUB
        }
    }
}

