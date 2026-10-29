package com.example.application.network

internal enum class PasswordStrength(val label: String, val level: Int) {
    WEAK("Debole", 1), FAIR("Discreta", 2), STRONG("Forte", 3), VERY_STRONG("Molto forte", 4);
    companion object {
        fun of(value: String): PasswordStrength {
            val lower = value.lowercase(java.util.Locale.ROOT)
            val common = listOf("password", "qwerty", "123456", "abcdef", "letmein").any { lower.contains(it) }
            if (!PasswordPolicy.isValid(value) || common || value.toSet().size < 5) return WEAK
            val classes = listOf(value.any(Char::isLowerCase), value.any(Char::isUpperCase), value.any(Char::isDigit),
                value.any { !it.isLetterOrDigit() && !it.isWhitespace() }).count { it }
            return when {
                value.length >= 16 && classes >= 3 && value.toSet().size >= 9 -> VERY_STRONG
                value.length >= 12 && classes >= 3 && value.toSet().size >= 7 -> STRONG
                else -> FAIR
            }
        }
    }
}
