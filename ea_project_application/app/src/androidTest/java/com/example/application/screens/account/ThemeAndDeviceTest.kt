package com.example.application.screens.account

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.graphics.luminance
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.application.MainNavigation
import com.example.application.device.CalendarEvent
import com.example.application.device.calendarIntent
import com.example.application.dto.UserDTO
import com.example.application.network.*
import com.example.application.screens.home.RecommendationsSection
import com.example.application.ui.background.BackgroundHelper
import com.example.application.ui.theme.ApplicationTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemeAndDeviceTest {
    @get:Rule val compose = createComposeRule()
    private val user = UserDTO(id = 71, username = "mario", fullName = "Mario Rossi", role = "TRAVELER")

    @Test fun accountAndNavigationFollowThemeWithoutLosingTheirPage() {
        var dark by mutableStateOf(true)
        var foreground = 0f
        var background = 0f
        compose.setContent { ApplicationTheme(darkTheme = dark) {
            foreground = MaterialTheme.colorScheme.onBackground.luminance()
            background = MaterialTheme.colorScheme.background.luminance()
            BackgroundHelper { MainNavigation(user, {}, homeContent = { androidx.compose.material3.Text("Home di prova", it) }) }
        } }
        compose.onNodeWithText("Account").performClick()
        compose.waitUntil(30_000) { com.example.application.service.media.WallpaperService.readyImage() != null }
        compose.runOnIdle { assertTrue(foreground > background) }
        screenshot("account-theme-dark")
        compose.runOnIdle { dark = false }
        compose.onNodeWithText("Il tuo spazio").assertIsDisplayed()
        compose.runOnIdle { assertTrue(foreground < background) }
        screenshot("account-theme-light")
        compose.onNodeWithText("Il mio profilo").performScrollTo().performClick()
        compose.onNodeWithText("Nome completo").performScrollTo().assertIsDisplayed()
        screenshot("profile-theme-light")
    }

    @Test fun walletCardsUseTheSameLayoutWithRecognizableNetworks() {
        compose.setContent { ApplicationTheme(darkTheme = false) {
            BackgroundHelper {
                androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(20.dp)) {
                    AccountPanel("Le tue carte") {
                        WalletCard(SavedCard(1, "Visa", "1111", "Mario Rossi", 2, 2027), true, {})
                        WalletCard(SavedCard(2, "Mastercard", "4444", "Mario Rossi", 12, 2030), true, {})
                    }
                }
            }
        } }
        compose.onNodeWithText("VISA").assertIsDisplayed()
        compose.onNodeWithText("••••  ••••  1111").assertIsDisplayed()
        screenshot("wallet-card-layout")
    }

    @Test fun calendarIntentContainsOnlyUserConfirmedEventDataAndNeedsNoCalendarPermission() {
        val event = CalendarEvent.from("Viaggio a Roma", "Roma", "2026-11-01", "2026-11-02")!!
        val intent = calendarIntent(event)
        assertEquals(android.content.Intent.ACTION_INSERT, intent.action)
        assertEquals(android.provider.CalendarContract.Events.CONTENT_URI, intent.data)
        assertEquals("Viaggio a Roma", intent.getStringExtra(android.provider.CalendarContract.Events.TITLE))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        @Suppress("DEPRECATION")
        val permissions = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty().toList()
        listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR,
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.MANAGE_EXTERNAL_STORAGE).forEach { assertFalse("Unexpected broad permission: $it", it in permissions) }
    }

    @Test fun recommendationPreferenceInvalidatesTheShownPersonalizedFeed() {
        UserSession.setSession(user)
        var calls = 0
        compose.setContent { ApplicationTheme { RecommendationsSection(load = {
            calls++
            ApiResult.Success(Recommendations(calls == 1, emptyList(), emptyList()))
        }) } }
        compose.onNodeWithText("Scelte per te").assertExists()
        compose.runOnIdle { RecommendationRepository.invalidate() }
        compose.waitUntil(5_000) { calls == 2 }
        compose.onNodeWithText("Idee da scoprire").assertExists()
        compose.runOnIdle { UserSession.clear() }
        compose.onNodeWithText("Idee da scoprire").assertDoesNotExist()
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null), "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
}
