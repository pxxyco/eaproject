package com.example.application.device

import java.time.YearMonth

internal object CardExpiry {
    fun fourDigitYear(year: Int): Int = if (year in 0..99) 2000 + year else year
    fun formatInput(value: String): String {
        val digits = value.filter { it in '0'..'9' }.take(4)
        return if (digits.length <= 2) digits else digits.take(2) + "/" + digits.drop(2)
    }
    fun parse(value: String): YearMonth? {
        val formatted = if (value.matches(Regex("[0-9]{4}"))) value.take(2) + "/" + value.takeLast(2) else value
        val match = Regex("^\\s*(\\d{1,2})\\s*[/.-]\\s*(\\d{2}|\\d{4})\\s*$").matchEntire(formatted) ?: return null
        return runCatching { YearMonth.of(fourDigitYear(match.groupValues[2].toInt()), match.groupValues[1].toInt()) }.getOrNull()
    }
    fun error(date: YearMonth?, today: YearMonth = YearMonth.now()): String? = when {
        date == null -> "Inserisci la scadenza nel formato MM/AA."
        date.isBefore(today) -> "La carta risulta scaduta: controlla mese e anno."
        date.year > today.year + 20 -> "Controlla l’anno di scadenza."
        else -> null
    }
}
