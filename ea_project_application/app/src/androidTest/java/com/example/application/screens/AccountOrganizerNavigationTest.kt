package com.example.application.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.application.dto.UserDTO
import com.example.application.network.UserSession
import com.example.application.ui.theme.ApplicationTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountOrganizerNavigationTest {
    @get:Rule val compose = createComposeRule()
    @After fun clearSession() { UserSession.clear() }

    @Test fun organizerOpensDashboardAndCanReturnFromAccountSettings() {
        UserSession.setSession(UserDTO(id = -1, fullName = "Organizzatore Test", username = "organizer-test", role = "ORGANIZER"))
        compose.setContent {
            ApplicationTheme { ProfileScreen(true, {}, {}) }
        }
        compose.onNodeWithText("Dashboard Organizzatore").assertIsDisplayed()
        val menu = compose.onNodeWithContentDescription("Apri menu account")
        val menuBounds = menu.fetchSemanticsNode().boundsInRoot
        val rootBounds = compose.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue("Account menu must be on the right", menuBounds.center.x > rootBounds.center.x)
        menu.performClick()
        compose.onNodeWithText("Impostazioni account").performClick()
        compose.onNodeWithText("Torna alla dashboard").performScrollTo().performClick()
        compose.onNodeWithText("Dashboard Organizzatore").assertIsDisplayed()
    }

    @Test fun travelerKeepsAccountProfileInsteadOfOrganizerDashboard() {
        UserSession.setSession(UserDTO(id = -2, fullName = "Viaggiatore Test", username = "traveler-test", role = "TRAVELER"))
        compose.setContent {
            ApplicationTheme { ProfileScreen(true, {}, {}) }
        }
        compose.onNodeWithText("@traveler-test").assertIsDisplayed()
        compose.onNodeWithText("Dashboard Organizzatore").assertDoesNotExist()
        compose.onNodeWithText("Account Viaggiatore").assertIsDisplayed()
    }
}
