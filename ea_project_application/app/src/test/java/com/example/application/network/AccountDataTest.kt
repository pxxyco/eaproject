package com.example.application.network

import com.example.application.screens.common.PhotoTooLargeException
import com.example.application.screens.common.readPhotoBytes
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
import java.time.YearMonth

class AccountDataTest {
    @Test fun profileNormalizationIsIndependentOfDeviceLocale() {
        val before = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr-TR"))
            val input = ProfileInput(" ITINERANTE ", " INFO@EXAMPLE.IT ", "  Mario   Rossi ")
            assertNull(input.error())
            assertEquals(ProfileInput("itinerante", "info@example.it", "Mario Rossi"), input.normalized())
        } finally { java.util.Locale.setDefault(before) }
    }
    @Test fun cardExpiryAcceptsCurrentMonthButRejectsPastAndInvalidDates() {
        val now = YearMonth.of(2026, 10)
        val card = CardInput("Visa", "1234", "Mario Rossi", 10, 2026)
        assertNull(card.error(now))
        listOf(card.copy(expiryMonth = 9), card.copy(expiryMonth = 13), card.copy(expiryYear = 2047),
            card.copy(last4 = "1234567812345678"), card.copy(holder = " ")).forEach { assertNotNull(it.error(now)) }
    }
    @Test fun preferencePayloadDoesNotOverwriteOrganizerOrPayoutData() {
        val json = Gson().toJsonTree(AccountPreferences(false, true, false)).asJsonObject
        assertEquals(setOf("chatAlerts", "bookingAlerts", "personalized"), json.keySet())
        val response = Gson().fromJson("""{"chatAlerts":false,"bookingAlerts":true,"personalized":false,"iban":"private"}""", AccountPreferences::class.java)
        assertFalse(response.chatAlerts)
        assertTrue(response.bookingAlerts)
    }
    @Test fun photoReaderHonorsSmallerProfileLimitIncludingUnknownSizeStreams() {
        assertEquals(8, readPhotoBytes(ByteArray(8).inputStream(), 8).size)
        assertThrows(PhotoTooLargeException::class.java) { readPhotoBytes(ByteArray(9).inputStream(), 8) }
    }
    @Test fun ticketPayloadKeepsTextAndExcludesServerOwnedFields() {
        val input = TicketInput("Account", "Accesso", "Non riesco ad accedere\nDal telefono")
        assertNull(input.error())
        val json = Gson().toJsonTree(input).asJsonObject
        assertEquals(setOf("category", "subject", "message"), json.keySet())
        assertEquals(input.message, json["message"].asString)
        assertNotNull(input.copy(message = "x".repeat(4001)).error())
    }
}
