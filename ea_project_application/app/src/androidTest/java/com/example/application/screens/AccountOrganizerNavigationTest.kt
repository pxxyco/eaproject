package com.example.application.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.application.MainNavigation
import com.example.application.dto.UserDTO
import com.example.application.ui.theme.ApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountOrganizerNavigationTest {
    @get:Rule val compose = createComposeRule()
    private val organizer = UserDTO(id = -1, fullName = "Organizzatore Test", username = "organizer-test", role = "ORGANIZER")
    private val traveler = UserDTO(id = -2, fullName = "Viaggiatore Test", username = "traveler-test", role = "TRAVELER")

    @Test fun organizerDashboardIsSeparateFromAccountAndLogoutIsConfirmed() {
        var user by mutableStateOf<UserDTO?>(organizer)
        var logouts = 0
        compose.setContent {
            ApplicationTheme {
                MainNavigation(user, { logouts++; user = null }, homeContent = { Text("Home di prova", it) },
                    tripsContent = { Text("Viaggi di prova", it) }, dashboardContent = { _, modifier -> Text("Gestione organizzatore", modifier) })
            }
        }
        compose.onNodeWithText("Dashboard").performClick()
        compose.onNodeWithText("Gestione organizzatore").assertIsDisplayed()
        compose.onNodeWithText("Preferiti").assertDoesNotExist()
        compose.onNodeWithText("Account").performClick()
        compose.onNodeWithText("@organizer-test").assertIsDisplayed()
        compose.onNodeWithText("Gestione organizzatore").assertDoesNotExist()
        compose.onNodeWithText("Disconnettiti").performScrollTo().performClick()
        compose.onNodeWithText("Annulla").performClick()
        compose.runOnIdle { assertEquals(0, logouts) }
        compose.onNodeWithText("Disconnettiti").performScrollTo().performClick()
        compose.onNodeWithText("Disconnetti").performClick()
        compose.runOnIdle { assertEquals(1, logouts) }
        compose.onNodeWithText("Dashboard").assertDoesNotExist()
        compose.onNodeWithText("Preferiti").performClick()
        compose.onNodeWithText("Accedi per vedere le tue liste").assertIsDisplayed()
    }

    @Test fun guestCanExploreAndProtectedTabOffersLoginOrReturnToExplore() {
        compose.setContent {
            ApplicationTheme { MainNavigation(null, {}, homeContent = { Text("Home di prova", it) }) }
        }
        compose.onNodeWithText("Home di prova").assertIsDisplayed()
        compose.onNodeWithText("Dashboard").assertDoesNotExist()
        compose.onNodeWithText("Preferiti").performClick()
        compose.onNodeWithText("Accedi per vedere le tue liste").assertIsDisplayed()
        compose.onNodeWithText("Esplora").performClick()
        compose.onNodeWithText("Home di prova").assertIsDisplayed()
        compose.onNodeWithText("Preferiti").performClick()
        compose.onNodeWithText("Accedi").performClick()
        compose.onNodeWithText("I tuoi viaggi, in un unico posto").assertDoesNotExist()
        compose.onNodeWithText("Account").assertIsSelected()
    }

    @Test fun middleTabUpdatesWhenRoleChangesAndSessionExpires() {
        var user by mutableStateOf<UserDTO?>(organizer)
        compose.setContent {
            ApplicationTheme { MainNavigation(user, {}, homeContent = { Text("Home di prova", it) },
                tripsContent = { Text("Viaggi di prova", it) }, dashboardContent = { _, modifier -> Text("Gestione organizzatore", modifier) }) }
        }
        compose.onNodeWithText("Dashboard").performClick()
        compose.runOnIdle { user = traveler }
        compose.onNodeWithText("Viaggi di prova").assertIsDisplayed()
        compose.onNodeWithText("Gestione organizzatore").assertDoesNotExist()
        compose.runOnIdle { user = null }
        compose.onNodeWithText("Viaggi di prova").assertDoesNotExist()
        compose.onNodeWithText("Accedi per vedere le tue liste").assertIsDisplayed()
    }
}
