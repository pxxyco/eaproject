package com.example.application.dto

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class MyTripDTOTest {
    private val gson = Gson()
    @Test fun bookingKeepsBookingIdSeparateFromOfferId() {
        val booking = gson.fromJson("""{"bookingId":42,"activityId":7,"bookingType":"ACTIVITY","bookingStatus":"PENDING"}""", BookingItemDTO::class.java)
        assertEquals(42L, booking.bookingId)
        assertEquals(7L, booking.activityId)
    }
    @Test fun customListActivitiesReadBackendAssociationFields() {
        val activity = gson.fromJson("""{"listId":3,"activityId":7,"title":"Museo","city":"Roma","addedAt":[2026,10,26]}""", ActivityHomeDTO::class.java)
        assertEquals(7L, activity.activityId)
        assertEquals("Museo", activity.title)
    }
    @Test fun customListTripsReadBackendAssociationFields() {
        val trip = gson.fromJson("""{"listId":3,"tripId":9,"title":"Tour","location":"Roma"}""", TripHomeDTO::class.java)
        assertEquals(9L, trip.tripId)
        assertEquals("Roma", trip.location)
    }
}
