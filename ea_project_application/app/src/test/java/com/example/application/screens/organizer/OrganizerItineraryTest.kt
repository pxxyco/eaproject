package com.example.application.screens.organizer

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class OrganizerItineraryTest {
    private fun stage(day: Int, title: String) = JsonObject().apply {
        addProperty("day", day); addProperty("title", title); addProperty("category", "VISIT")
    }
    @Test fun orderingIsPerDayAndPreservesDesktopMetadata() {
        val existing = JsonParser.parseString("""{"stageId":7,"day":1,"title":"Museo","category":"VISIT","latitude":39.0,"photoUrl":"/photo/tappa.jpg","startTime":[9,30],"createdAt":[2027,1,1,0,0]}""").asJsonObject
        val result = itineraryPayload(listOf(stage(2, "Spiaggia"), existing, stage(1, "Centro")), "2027-06-01", "2027-06-03")
        assertEquals("Museo", result[0].asJsonObject.text("title"))
        assertEquals(7, result[0].asJsonObject.number("stageId"))
        assertEquals("09:30", result[0].asJsonObject.text("startTime"))
        assertEquals("/photo/tappa.jpg", result[0].asJsonObject.text("photoUrl"))
        assertEquals(2, result[1].asJsonObject.number("orderInDay"))
        assertEquals(1, result[2].asJsonObject.number("orderInDay"))
        assertFalse(result[0].asJsonObject.has("createdAt"))
        assertTrue(existing.has("createdAt"))
    }
    @Test fun rejectsStagesOutsideTripDatesAndSupportsClearingItinerary() {
        assertThrows(IllegalArgumentException::class.java) {
            itineraryPayload(listOf(stage(4, "Fuori intervallo")), "2027-06-01", "2027-06-03")
        }
        assertEquals(0, itineraryPayload(emptyList(), "2027-06-01", "2027-06-03").size())
    }
    @Test fun validatesCoordinatesAndRetainsUneditedFields() {
        val original = stage(1, "Museo").apply { addProperty("stageId", 12); addProperty("notes", "Accesso laterale") }
        val fields = mapOf("title" to "Museo nuovo", "day" to "2", "latitude" to "39,3", "longitude" to "16,2")
        val result = stagePayload(original, fields, "VISIT")
        assertEquals(12, result.number("stageId"))
        assertEquals("Accesso laterale", result.text("notes"))
        assertEquals("39.3", result.text("latitude"))
        assertThrows(IllegalArgumentException::class.java) { stagePayload(original, fields + ("latitude" to "91"), "VISIT") }
        assertThrows(IllegalArgumentException::class.java) { stagePayload(original, fields + ("longitude" to ""), "VISIT") }
    }
    @Test fun filtersOffersWithoutChangingKpiSourceData() {
        val offer = OrganizerOffer(true, JsonObject().apply {
            addProperty("title", "Tour Calabria"); addProperty("destinationCity", "Cosenza"); addProperty("status", "DRAFT")
        })
        assertTrue(offer.matches(" cosenza ", "DRAFT"))
        assertFalse(offer.matches("Calabria", "PUBLISHED"))
        assertTrue(offer.matches("calabria", "ALL"))
        assertFalse(offerStatuses(false).any { it.first == "DRAFT" })
    }
}
