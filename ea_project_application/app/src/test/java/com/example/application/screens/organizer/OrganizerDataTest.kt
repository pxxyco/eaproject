package com.example.application.screens.organizer

import com.google.gson.JsonObject
import org.junit.Assert.*
import org.junit.Test

class OrganizerDataTest {
    private fun fields() = mapOf("title" to "Tour", "description" to "Descrizione", "destinationCity" to "Roma", "destinationCountry" to "Italia", "startDate" to "2027-06-10", "endDate" to "2027-06-12", "totalPrice" to "12,50", "maxSeats" to "12")

    @Test fun updatePreservesBookingsAndUneditedFields() {
        val data = JsonObject().apply {
            addProperty("tripId", 7); addProperty("maxSeats", 10); addProperty("availableSeats", 4)
            addProperty("coverPhotoUrl", "/cover.jpg"); addProperty("status", "PUBLISHED")
        }
        val result = offerPayload(OrganizerOffer(true, data), fields())
        assertEquals(6, result.number("availableSeats"))
        assertEquals("/cover.jpg", result.text("coverPhotoUrl"))
        assertEquals("12.50", result.text("totalPrice"))
        assertEquals(4, data.number("availableSeats"))
    }

    @Test fun cannotReduceCapacityBelowBookedSeats() {
        val data = JsonObject().apply { addProperty("maxSeats", 10); addProperty("availableSeats", 2) }
        assertThrows(IllegalArgumentException::class.java) {
            offerPayload(OrganizerOffer(true, data), fields() + ("maxSeats" to "7"))
        }
    }

    @Test fun rejectsInvalidDateRangeAndPrice() {
        val offer = OrganizerOffer(true, JsonObject())
        assertThrows(IllegalArgumentException::class.java) { offerPayload(offer, fields() + ("endDate" to "2027-06-01")) }
        assertThrows(IllegalArgumentException::class.java) { offerPayload(offer, fields() + ("totalPrice" to "-1")) }
    }

    @Test fun newOfferPublishesAllSeats() {
        val result = offerPayload(OrganizerOffer(true, JsonObject()), fields())
        assertEquals(12, result.number("availableSeats"))
        assertEquals("PUBLISHED", result.text("status"))
    }

    @Test fun acceptsJacksonDateArraysAndPreservesActivityTime() {
        val data = com.google.gson.JsonParser.parseString("""{
            "startDate":[2027,6,1,14,30],"endDate":[2027,6,1,16,0],
            "createdAt":[2027,5,1,9,0,1],"activityId":7,"maxSeats":10,"availableSeats":6,
            "latitude":39.3,"longitude":16.2,"status":"PUBLISHED"
        }""").asJsonObject
        assertEquals("2027-06-01T14:30", data.text("startDate"))
        assertEquals("01/06/2027", dateLabel(data.text("startDate")))
        val form = mapOf("title" to "Visita", "description" to "", "city" to "Roma",
            "startDate" to data.text("startDate"), "endDate" to data.text("endDate"),
            "price" to "0", "maxSeats" to "12", "durationMinutes" to "90")
        val payload = offerPayload(OrganizerOffer(false, data), form)
        assertEquals("39.3", payload.text("latitude"))
        assertEquals(8, payload.number("availableSeats"))
        assertFalse(payload.has("createdAt"))
    }

    @Test fun mobileUpdateDoesNotOverwriteDesktopItinerary() {
        val data = com.google.gson.JsonParser.parseString("""{"tripId":7,"maxSeats":10,"availableSeats":10,"stages":[{"stageId":9}]}""").asJsonObject
        assertFalse(offerPayload(OrganizerOffer(true, data), fields()).has("stages"))
    }
}
