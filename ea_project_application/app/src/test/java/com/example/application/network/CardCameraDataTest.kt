package com.example.application.network

import com.example.application.device.*
import org.junit.Assert.*
import org.junit.Test
import java.time.YearMonth

class CardCameraDataTest {
    private val front = listOf("VISA", "4111 1111 1111 1111", "MARIO ROSSI", "VALID THRU 02/27")
    @Test fun extractsOnlyCardMetadataAndNeverTheSecurityCode() {
        val result = CardTextReader.read(front + listOf("CVV 123", "CODICE SICUREZZA"), YearMonth.of(2026,10))
        assertEquals("4111111111111111", result.pan)
        assertEquals("MARIO ROSSI", result.holder)
        assertEquals(YearMonth.of(2027,2), result.expiry)
        assertFalse(result.toString().contains("4111"))
        assertFalse(result.toString().contains("123"))
    }
    @Test fun adjacentNumericFieldsAreNotCombinedOrSilentlyTruncated() {
        assertNull(CardTextReader.read(listOf("4111 1111 1111 1111 123")).pan)
        assertNull(CardTextReader.read(listOf("4111 1111 1111 1111 02/27")).pan)
    }
    @Test fun expiryIsNotConfusedWithIssueDateAndAmbiguityIsLeftForTheUser() {
        val result = CardTextReader.read(listOf("VALID FROM 02/26", "VALID THRU 02/29"), YearMonth.of(2026,1))
        assertEquals(YearMonth.of(2029,2), result.expiry)
        assertNull(CardTextReader.read(listOf("02/28", "02/29"), YearMonth.of(2026,1)).expiry)
        assertNull(CardTextReader.read(listOf("4111111111111112")).pan)
    }
    @Test fun autoSaveRequiresRepeatedEvidenceAndClearsWhenTheCardChanges() {
        val session = CardScanSession { YearMonth.of(2026,10) }
        assertNull(session.accept(front))
        assertNull(session.accept(emptyList()))
        assertNull(session.accept(emptyList()))
        assertNull(session.accept(front))
        val result = session.accept(front)!!
        assertEquals("1111", result.last4)
        assertEquals("MARIO ROSSI", result.holder)
        assertNull(session.accept(listOf("5555555555554444")))
        assertNull(session.snapshot()!!.holder)
        session.clear()
        assertNull(session.snapshot())
    }
    @Test fun frontAndRearCanCompleteTheSameExplicitScan() {
        val session = CardScanSession { YearMonth.of(2026,10) }
        repeat(3) { assertNull(session.accept(listOf("4111 1111 1111 1111", "MARIO ROSSI"))) }
        repeat(2) { assertNull(session.accept(listOf("VALID THRU 02/27", "CVV 123"))) }
        val result = session.accept(listOf("VALID THRU 02/27", "CVV 123"))!!
        assertEquals(2, result.month)
        assertEquals(2027, result.year)
        assertEquals("MARIO ROSSI", result.holder)
    }
    @Test fun strengthRespectsPolicyAndRecognizesCommonAndStrongPasswords() {
        assertEquals(PasswordStrength.WEAK, PasswordStrength.of("password123!"))
        assertEquals(PasswordStrength.WEAK, PasswordStrength.of("short"))
        assertEquals(PasswordStrength.FAIR, PasswordStrength.of("Mare28!x"))
        assertEquals(PasswordStrength.STRONG, PasswordStrength.of("Cammino28!Alpi"))
        assertEquals(PasswordStrength.VERY_STRONG, PasswordStrength.of("Sentieri!Luna2027#"))
    }
}
