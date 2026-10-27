package com.example.application.device

/** Only these card details may leave the NFC reader or be sent to the account API. */
internal data class ScannedCard(val brand: String, val last4: String, val holder: String?, val month: Int?, val year: Int?)

internal object CardNumbers {
    fun brand(number: String): String? {
        val digits = number.filter(Char::isDigit)
        val two = digits.take(2).toIntOrNull() ?: 0
        val four = digits.take(4).toIntOrNull() ?: 0
        return when {
            digits.startsWith("4") -> "Visa"
            two in 51..55 || four in 2221..2720 -> "Mastercard"
            two == 34 || two == 37 -> "Amex"
            digits.length >= 6 -> "Altro"
            else -> null
        }
    }
    fun valid(number: String): Boolean {
        if (!number.matches(Regex("[0-9]{13,19}")) || number.toSet().size == 1) return false
        val expected = when (brand(number)) { "Amex" -> number.length == 15; "Mastercard" -> number.length == 16; "Visa" -> number.length in listOf(13, 16, 19); else -> true }
        var sum = 0
        number.reversed().forEachIndexed { index, c ->
            var n = c.digitToInt()
            if (index % 2 == 1) { n *= 2; if (n > 9) n -= 9 }
            sum += n
        }
        return expected && sum % 10 == 0
    }
}
