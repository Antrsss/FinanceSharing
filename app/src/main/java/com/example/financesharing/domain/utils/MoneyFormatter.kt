package com.example.financesharing.domain.utils

import com.example.financesharing.domain.model.Currency
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object MoneyFormatter {
    private val symbols = DecimalFormatSymbols(Locale.getDefault()).apply {
        decimalSeparator = '.'
        groupingSeparator = ' '
    }

    private val df = DecimalFormat("#,##0.00", symbols)

    /**
     * Amount is stored in minimal units (e.g. cents/kopeks).
     */
    fun formatMinor(amountMinor: Long, currency: Currency): String {
        val major = amountMinor.toDouble() / 100.0
        return "${df.format(major)} ${currency.symbol}"
    }
}

