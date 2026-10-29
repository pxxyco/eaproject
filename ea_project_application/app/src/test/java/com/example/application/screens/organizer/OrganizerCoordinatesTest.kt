package com.example.application.screens.organizer

import com.google.gson.JsonObject
import org.junit.Assert.*
import org.junit.Test

class OrganizerCoordinatesTest {
    @Test fun coordinatesAcceptDecimalCommaAndClearTogether() {
        val data = JsonObject()
        applyCoordinates(data, "39,3", "16,2")
        assertEquals(39.3, data.get("latitude").asDouble, 0.0)
        applyCoordinates(data, "", "")
        assertTrue(data.get("latitude").isJsonNull)
        assertTrue(data.get("longitude").isJsonNull)
    }
    @Test fun partialNonFiniteAndOutOfRangeCoordinatesAreRejected() {
        listOf("39" to "", "" to "16", "91" to "16", "39" to "181", "NaN" to "16", "39" to "Infinity").forEach { (lat, lon) ->
            assertThrows(IllegalArgumentException::class.java) { applyCoordinates(JsonObject(), lat, lon) }
        }
    }
    @Test fun activityPayloadSendsNumericCoordinatesAndSupportsEditing() {
        val data = JsonObject().apply { addProperty("latitude", 39.3); addProperty("longitude", 16.2) }
        val fields = mapOf("title" to "Museo", "city" to "Roma", "price" to "10", "maxSeats" to "10",
            "startDate" to "2027-06-01T09:00", "endDate" to "2027-06-01T11:00", "durationMinutes" to "120",
            "latitude" to "40,5", "longitude" to "17,5")
        val payload = offerPayload(OrganizerOffer(false, data), fields)
        assertEquals(40.5, payload.get("latitude").asDouble, 0.0)
        assertTrue(payload.get("latitude").asJsonPrimitive.isNumber)
        val cleared = offerPayload(OrganizerOffer(false, data), fields + mapOf("latitude" to "", "longitude" to ""))
        assertTrue(cleared.get("latitude").isJsonNull)
    }
    @Test fun stagePayloadRetainsIdWhenCoordinatesAreChangedOrCleared() {
        val original = JsonObject().apply { addProperty("stageId", 7) }
        val fields = mapOf("title" to "Museo", "day" to "1", "latitude" to "39,3", "longitude" to "16,2")
        val saved = stagePayload(original, fields, "VISIT")
        assertEquals(7, saved.number("stageId"))
        assertEquals(39.3, saved.get("latitude").asDouble, 0.0)
        val cleared = stagePayload(saved, fields + mapOf("latitude" to "", "longitude" to ""), "VISIT")
        assertTrue(cleared.get("latitude").isJsonNull)
    }
}
