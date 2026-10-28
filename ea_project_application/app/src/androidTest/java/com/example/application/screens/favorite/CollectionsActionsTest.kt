package com.example.application.screens.favorite

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.application.dto.*
import com.example.application.network.MyTripRepository
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CollectionsActionsTest {
    @get:Rule val compose = createComposeRule()
    private val target = MyTripRepository.HubTarget.Custom(FavoriteList(3, "Musei"))
    @Test fun loginButtonInvokesNavigation() {
        var called = false
        compose.setContent { MaterialTheme { LoginRequired(onLoginClick = { called = true }) } }
        compose.onNodeWithText("Accedi").performClick()
        compose.runOnIdle { assertTrue(called) }
    }
    @Test fun removingActivityRequiresConfirmationAndUsesOfferId() {
        var removed: Long? = null
        compose.setContent { MaterialTheme {
            CollectionScreen(target, {}, load = { CollectionItemsDTO(activities = listOf(ActivityHomeDTO(activityId = 7, title = "Museo"))) },
                remove = { _, id, trip -> assertFalse(trip); removed = id })
        } }
        compose.onNodeWithText("Rimuovi dalla lista").performClick()
        compose.runOnIdle { assertNull(removed) }
        compose.onNodeWithText("Conferma").performClick()
        compose.runOnIdle { assertEquals(7L, removed) }
    }
    @Test fun deletingListReturnsToHubOnlyAfterSuccess() {
        var deleted: Long? = null
        var back = false
        compose.setContent { MaterialTheme {
            CollectionScreen(target, { back = true }, load = { CollectionItemsDTO() }, deleteList = { deleted = it })
        } }
        compose.onNodeWithText("Elimina lista").performClick()
        compose.onNodeWithText("Conferma").performClick()
        compose.runOnIdle { assertEquals(3L, deleted); assertTrue(back) }
    }
    @Test fun cancellingBookingUsesBookingIdRatherThanActivityId() {
        var removed: Long? = null
        compose.setContent { MaterialTheme {
            CollectionScreen(MyTripRepository.HubTarget.Bookings, {}, load = { CollectionItemsDTO(bookings = listOf(
                BookingItemDTO(bookingId = 42, activityId = 7, bookingStatus = "CONFIRMED", itemDate = "2099-01-01T12:00:00")
            )) }, cancelBooking = { id -> removed = id })
        } }
        compose.onNodeWithText("Annulla prenotazione").performClick()
        compose.onNodeWithText("Conferma").performClick()
        compose.runOnIdle { assertEquals(42L, removed) }
    }
}
