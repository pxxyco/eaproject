package com.example.application.network

import com.example.application.device.CardNumbers
import com.example.application.device.ScannedCard
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class CardDetailsTest {
    @Test fun networksAreDetectedWithoutManualSelection() {
        assertEquals("Visa", CardNumbers.brand("4111111111111111"))
        assertEquals("Mastercard", CardNumbers.brand("5555555555554444"))
        assertEquals("Mastercard", CardNumbers.brand("2221000000000009"))
        assertEquals("Mastercard", CardNumbers.brand("2720990000000000"))
        assertEquals("Altro", CardNumbers.brand("2220000000000000"))
        assertEquals("Altro", CardNumbers.brand("2721000000000000"))
        assertEquals("Amex", CardNumbers.brand("378282246310005"))
        assertNull(CardNumbers.brand("2"))
    }
    @Test fun checksumAndNetworkLengthsRejectMistypedNumbers() {
        assertTrue(CardNumbers.valid("4111111111111111"))
        assertTrue(CardNumbers.valid("5555555555554444"))
        assertTrue(CardNumbers.valid("378282246310005"))
        assertFalse(CardNumbers.valid("4111111111111112"))
        assertFalse(CardNumbers.valid("4111"))
        assertFalse(CardNumbers.valid("41111111111111111111"))
    }
    @Test fun serverPayloadAndNfcResultOnlyContainMaskedMetadata() {
        val scan = ScannedCard("Visa", "1111", null, 12, 2030)
        val payload = CardInput(scan.brand, scan.last4, "Mario Rossi", scan.month!!, scan.year!!)
        val json = Gson().toJsonTree(payload).asJsonObject
        assertEquals(setOf("brand", "last4", "holder", "expiryMonth", "expiryYear"), json.keySet())
        assertFalse(json.has("number"))
        assertFalse(json.has("pan"))
        assertFalse(json.has("cvv"))
        assertFalse(Gson().toJson(scan).contains("4111111111111111"))
    }
}
