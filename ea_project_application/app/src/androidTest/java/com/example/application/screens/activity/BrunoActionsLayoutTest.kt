package com.example.application.screens.activity

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.application.dto.*
import com.example.application.network.UserSession
import com.example.application.screens.activity.booking.BookingSheet
import com.example.application.ui.theme.ApplicationTheme
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class BrunoActionsLayoutTest {
 @get:Rule val compose = createComposeRule()
 @After fun clearSession() { UserSession.clear() }
 @Test fun guestActionsUseLoginAndModernLayoutKeepsBookingBar() {
  UserSession.clear()
  var logins = 0
  val detail = ExperienceDetail.Activity(ActivityDetailDTO(activityId = 7, title = "Museo", availableSeats = 5, price = 10.0))
  compose.setContent { ApplicationTheme { ExperienceDetailContent(detail, onLoginRequired = { logins++ }) } }
  compose.onNodeWithContentDescription("Aggiungi ai preferiti").performClick()
  compose.onNodeWithContentDescription("Aggiungi a una lista").performClick()
  compose.onNodeWithText("Accedi").performClick()
  compose.onNodeWithTag("experience_booking_bar").assertIsDisplayed()
  compose.runOnIdle { assertEquals(3, logins) }
 }
 @Test fun paidReservationIsNotPresentedAsAlreadyConfirmed() {
  var requests = 0
  compose.setContent { ApplicationTheme {
   BookingSheet("Museo", 10.0, 5, {}, { requests++; BookingItemDTO(bookingId = 42, bookingStatus = "PENDING") })
  } }
  compose.onNodeWithText("Conferma").performClick()
  compose.onNodeWithText("In attesa di pagamento").assertIsDisplayed()
  compose.onNodeWithText("Conferma").assertDoesNotExist()
  compose.runOnIdle { assertEquals(1, requests) }
 }
}
