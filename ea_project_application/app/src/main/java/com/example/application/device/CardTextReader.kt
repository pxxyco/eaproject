package com.example.application.device

import java.time.YearMonth
import java.util.Locale

/** Conservative extraction of printed metadata. OCR text and PAN never enter a request, file or log. */
internal object CardTextReader {
    internal data class Reading(val pan: String?, val holder: String?, val expiry: YearMonth?) {
        // Deliberately do not expose a generated toString containing the number.
        override fun toString() = "CardReading(redacted)"
    }
    private val panPatterns = listOf(
        Regex("(?<![0-9])(?:[0-9]{4}[ -]){3}[0-9]{4}(?![0-9])"),
        Regex("(?<![0-9])[0-9]{4}[ -][0-9]{6}[ -][0-9]{5}(?![0-9])"),
        Regex("(?<![0-9])[0-9]{13,19}(?![0-9])"))
    private val datePattern = Regex("(?<![0-9])([0-1][0-9])\\s*[/.-]\\s*([0-9]{4}|[0-9]{2})(?![0-9])")
    private val excluded = setOf("VISA", "MASTERCARD", "MASTER", "CARD", "AMERICAN", "EXPRESS", "CREDIT", "DEBIT", "BANK", "BANCA", "VALID", "THRU", "THROUGH", "GOOD", "UNTIL", "FROM", "SCADENZA", "EMISSIONE", "SIGNATURE", "AUTHORIZED", "FIRMA", "TITOLARE", "CARDHOLDER", "NAME", "SECURITY", "CODE", "CUSTOMER", "SERVICE", "SUPPORT", "PLATINUM", "GOLD", "WORLD", "INTERNATIONAL", "PREPAID", "NOT", "TRANSFERABLE", "ELECTRONIC", "USE", "ONLY", "CONTACTLESS", "EUROPE", "ITALIA", "CODICE", "SICUREZZA", "VALIDA", "FINO", "AL", "CUSTOMER", "CARE", "INTESA", "SANPAOLO", "UNICREDIT", "POSTE", "ITALIANE", "SERVIZIO", "CLIENTI")
    fun read(lines: List<String>, now: YearMonth = YearMonth.now()): Reading {
        val pans = lines.flatMap { line -> panPatterns.flatMap { pattern ->
            pattern.findAll(line).mapNotNull { match ->
                // Never mistake a prefix of a longer number, or an adjacent CVV/date, for a complete PAN.
                if (line.removeRange(match.range).any(Char::isDigit)) null else match.value.filter(Char::isDigit)
            }.toList()
        } }
            .filter(CardNumbers::valid).distinct()
        val dates = lines.flatMap { datePattern.findAll(it).mapNotNull { match -> CardExpiry.parse(match.value) }.toList() }
            .filter { CardExpiry.error(it, now) == null }.distinct()
        val labeled = lines.flatMapIndexed { index, line ->
            if (Regex("(?i)(THRU|THROUGH|UNTIL|SCADENZA|EXPIR)").containsMatchIn(line)) listOf(line) + lines.drop(index + 1).take(1) else emptyList()
        }.flatMap { datePattern.findAll(it).mapNotNull { match -> CardExpiry.parse(match.value) }.toList() }
            .filter { CardExpiry.error(it, now) == null }.distinct()
        val names = lines.map { it.trim().replace(Regex("\\s+"), " ") }.filter { line ->
            val words = line.uppercase(Locale.ROOT).split(" ")
            line.length in 5..80 && words.size in 2..4 && words.none { it in excluded } &&
                line.matches(Regex("[\\p{L}][\\p{L}'’.-]*( [\\p{L}][\\p{L}'’.-]*){1,3}"))
        }.distinctBy { it.uppercase(Locale.ROOT) }
        return Reading(pans.singleOrNull(), names.singleOrNull(), labeled.singleOrNull() ?: dates.singleOrNull())
    }
}

/** Requires consistent frames. Front and back may be combined only within the same explicit scan. */
internal class CardScanSession(private val today: () -> YearMonth = YearMonth::now) {
    private var pan: String? = null
    private var holder: String? = null
    private var expiry: YearMonth? = null
    private var panFrames = 0
    private var holderFrames = 0
    private var expiryFrames = 0
    fun accept(lines: List<String>): ScannedCard? {
        val reading = CardTextReader.read(lines, today())
        if (reading.pan != null && pan != null && reading.pan != pan) clear()
        reading.pan?.let { panFrames = if (pan == it) panFrames + 1 else 1; pan = it }
        reading.holder?.let {
            if (holder == null || reading.pan != null || holder == it) {
                holderFrames = if (holder == it) holderFrames + 1 else 1; holder = it
            }
        }
        reading.expiry?.let { expiryFrames = if (expiry == it) expiryFrames + 1 else 1; expiry = it }
        return snapshot()?.takeIf { panFrames >= 3 && holderFrames >= 3 && expiryFrames >= 3 }
    }
    fun snapshot(): ScannedCard? = pan?.let { ScannedCard(CardNumbers.brand(it) ?: "Altro", it.takeLast(4), holder, expiry?.monthValue, expiry?.year) }
    fun clear() { pan = null; holder = null; expiry = null; panFrames = 0; holderFrames = 0; expiryFrames = 0 }
}
