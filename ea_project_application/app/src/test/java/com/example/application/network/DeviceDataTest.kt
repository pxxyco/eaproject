package com.example.application.network

import com.example.application.device.CardExpiry
import com.example.application.device.CalendarEvent
import org.junit.Assert.*
import org.junit.Test
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.LocalDate

class DeviceDataTest {
    @Test fun expiryAcceptsPrintedAndFullYearFormatsAndCurrentMonth() {
        val today = YearMonth.of(2026, 10)
        assertEquals(CardExpiry.parse("12/28"), CardExpiry.parse("12/2028"))
        assertEquals(YearMonth.of(2028, 12), CardExpiry.parse(CardExpiry.formatInput("1228")))
        assertNull(CardExpiry.error(CardExpiry.parse("10/26"), today))
        assertNotNull(CardExpiry.error(CardExpiry.parse("09/26"), today))
        assertNotNull(CardExpiry.error(CardExpiry.parse("13/28"), today))
        assertNotNull(CardExpiry.error(CardExpiry.parse("12/2"), today))
        assertEquals(2028, CardInput("Visa", "1111", "Mario Rossi", 12, 28).normalized().expiryYear)
    }
    @Test fun tripsUseInclusiveDatesAndAnExclusiveUtcCalendarEnd() {
        val event = CalendarEvent.from("Roma", "Italia", "2026-11-01", "2026-11-03")!!
        assertTrue(event.allDay)
        assertEquals(LocalDate.of(2026,11,4).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), event.end)
        assertEquals(3 * 86_400_000L, event.end - event.start)
    }
    @Test fun activitiesMatchBackendRomeTimezoneAcrossDaylightSaving() {
        val local = CalendarEvent.from("Visita", "Roma", "2026-10-25T10:00:00", "2026-10-25T11:30:00")!!
        val utc = CalendarEvent.from("Visita", "Roma", "2026-10-25T09:00:00Z", "2026-10-25T10:30:00Z")!!
        assertFalse(local.allDay)
        assertEquals(utc.start, local.start)
        assertEquals(utc.end, local.end)
    }
    @Test fun invalidCalendarDataNeverProducesAnIntentPayload() {
        assertNull(CalendarEvent.from("Viaggio", null, "invalid", null))
        assertNull(CalendarEvent.from("Viaggio", null, "2026-12-10", "2026-12-09"))
        assertNull(CalendarEvent.from("Attività", null, "2026-12-10T12:00:00", "2026-12-10T11:00:00"))
    }
}
