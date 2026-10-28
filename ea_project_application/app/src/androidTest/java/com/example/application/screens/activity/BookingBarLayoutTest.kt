package com.example.application.screens.activity

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.example.application.AppDestinations
import com.example.application.dto.ActivityDetailDTO
import com.example.application.dto.ExperienceDetail
import com.example.application.ui.background.BackgroundHelper
import com.example.application.ui.navigation.FloatingNavigationLayout
import com.example.application.ui.theme.ApplicationTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class BookingBarLayoutTest {
    @get:Rule val compose = createComposeRule()
    private val detail = ExperienceDetail.Activity(ActivityDetailDTO(
        activityId = 7, title = "Passeggiata nel centro storico", city = "Cosenza",
        price = 25.0, availableSeats = 4,
        description = "Una passeggiata tra luoghi, storie e tradizioni della città. ".repeat(30),
        notes = "Ultima informazione della scheda"
    ))

    private fun page(dark: Boolean, scale: Float = 1f, onBook: () -> Unit = {}) {
        compose.setContent { ApplicationTheme(darkTheme = dark) {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                BackgroundHelper {
                    FloatingNavigationLayout(
                        listOf(AppDestinations.HOME, AppDestinations.`MY-TRIP`, AppDestinations.PROFILE),
                        AppDestinations.HOME, {}, fadeContent = false
                    ) { modifier -> Box(modifier.fillMaxSize()) { ExperienceDetailContent(detail, onBook) } }
                }
            }
        } }
    }

    @Test fun bookingActionStaysAboveNavigationWhileTheLastContentRemainsReachable() {
        var bookings = 0
        page(dark = false, onBook = { bookings++ })
        val bar = compose.onNodeWithTag("experience_booking_bar").fetchSemanticsNode().boundsInRoot
        val nav = compose.onNodeWithTag("floating_navigation").fetchSemanticsNode().boundsInRoot
        assertEquals(bar.left, nav.left, 1f)
        assertEquals(bar.right, nav.right, 1f)
        assertTrue("The booking action must sit above the floating navigation", bar.bottom < nav.top)
        compose.onNodeWithText("Prenota").performClick()
        compose.runOnIdle { assertEquals(1, bookings) }
        compose.onNodeWithText("Ultima informazione della scheda").performScrollTo()
        // Reveal the full end of the list, beyond the strip considered visible by performScrollTo.
        compose.onNodeWithTag("experience_detail_scroll").performSemanticsAction(SemanticsActions.ScrollBy) { scroll ->
            scroll(0f, 10_000f)
        }
        compose.waitForIdle()
        compose.onNodeWithText("Ultima informazione della scheda").assertIsDisplayed()
        screenshot("activity-booking-light")
        val lastText = compose.onNodeWithText("Ultima informazione della scheda").fetchSemanticsNode().boundsInRoot
        assertTrue("The end of the page must scroll clear of the fixed booking action: text=$lastText, bar=$bar, viewport=${compose.onNodeWithTag("experience_detail_scroll").fetchSemanticsNode().boundsInRoot}", lastText.bottom <= bar.top)
        assertEquals(bar, compose.onNodeWithTag("experience_booking_bar").fetchSemanticsNode().boundsInRoot)
    }

    @Test fun largeTextKeepsTheActionInsideTheCapsuleInDarkTheme() {
        page(dark = true, scale = 1.6f)
        compose.onNodeWithText("€ 25,00").assertIsDisplayed()
        compose.onNodeWithText("Prenota").assertIsDisplayed()
        val bar = compose.onNodeWithTag("experience_booking_bar").fetchSemanticsNode().boundsInRoot
        val button = compose.onNodeWithText("Prenota").fetchSemanticsNode().boundsInRoot
        val nav = compose.onNodeWithTag("floating_navigation").fetchSemanticsNode().boundsInRoot
        assertTrue(button.bottom <= bar.bottom && button.left >= bar.left && button.right <= bar.right)
        assertTrue(bar.bottom < nav.top)
        screenshot("activity-booking-dark-large-text")
    }

    @Test fun backToolbarStaysAboveThePhotoAndPillsSwitchTheActualSections() {
        var backClicks = 0
        compose.setContent { ApplicationTheme(darkTheme = false) {
            BackgroundHelper {
                FloatingNavigationLayout(
                    listOf(AppDestinations.HOME, AppDestinations.`MY-TRIP`, AppDestinations.PROFILE),
                    AppDestinations.HOME, {}, fadeContent = false
                ) { modifier -> ExperienceDetailScreen(detail.ref, { backClicks++ }, modifier, load = { detail }) }
            }
        } }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("experience_cover").fetchSemanticsNodes().isNotEmpty() }
        val back = compose.onNodeWithText("Indietro").fetchSemanticsNode().boundsInRoot
        val cover = compose.onNodeWithTag("experience_cover").fetchSemanticsNode().boundsInRoot
        assertTrue("Back must remain outside the cover photo", back.bottom <= cover.top)
        compose.onNodeWithText("Itinerario").performScrollTo().performClick()
        compose.onNodeWithText("Luogo").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Itinerario").assertIsSelected()
        compose.onNodeWithText("Foto").performClick()
        compose.onNodeWithText("Nessuna foto disponibile.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Foto").assertIsSelected()
        screenshot("activity-modern-sections")
        compose.onNodeWithText("Indietro").performClick()
        compose.runOnIdle { assertEquals(1, backClicks) }
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(150)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null), "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
}
