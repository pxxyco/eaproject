package com.example.application.screens.account

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.application.MainNavigation
import com.example.application.dto.UserDTO
import com.example.application.screens.common.publishScreenLoading
import com.example.application.ui.background.BackgroundHelper
import com.example.application.ui.effects.LocalNavigationTail
import com.example.application.ui.theme.ApplicationTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class NavigationPresentationTest {
    @get:Rule val compose = createComposeRule()
    private val user = UserDTO(id = 71, username = "mario", fullName = "Mario Rossi", role = "TRAVELER")

    @Test fun lightNavigationHasNoBrightStripBehindLabels() {
        compose.setContent { ApplicationTheme(darkTheme = false) {
            Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF777777))) {
                com.example.application.ui.navigation.FloatingNavigation(
                    listOf(com.example.application.AppDestinations.HOME, com.example.application.AppDestinations.`MY-TRIP`, com.example.application.AppDestinations.PROFILE),
                    com.example.application.AppDestinations.PROFILE, {},
                    Modifier.align(androidx.compose.ui.Alignment.BottomCenter).padding(bottom = 100.dp))
            }
        } }
        val label = compose.onNodeWithText("Esplora").fetchSemanticsNode().boundsInRoot
        val density = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
        awaitNativeFrame()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        try {
            // Compare empty space beside the label against the same capsule fill above it.
            val x = (label.right + 12 * density).toInt()
            val atLabel = bitmap.getPixel(x, label.center.y.toInt())
            val aboveLabel = bitmap.getPixel(x, (label.top - 5 * density).toInt())
            assertTrue("The glass fill must not contain a brighter rectangle behind labels",
                kotlin.math.abs(android.graphics.Color.red(atLabel) - android.graphics.Color.red(aboveLabel)) < 8)
        } finally { bitmap.recycle() }
    }

    @Test fun contentAboveTheFadeIsActuallyRenderedOpaque() {
        compose.setContent { ApplicationTheme {
            MainNavigation(user, {}, homeContent = { modifier ->
                Box(modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Red).testTag("painted_page"))
            })
        } }
        compose.waitForIdle()
        awaitNativeFrame()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        try {
            val color = bitmap.getPixel(bitmap.width / 2, bitmap.height / 3)
            assertTrue("Page content must remain visible above the navigation fade", android.graphics.Color.red(color) > 220)
            assertTrue(android.graphics.Color.green(color) < 40)
        } finally { bitmap.recycle() }
    }

    @Test fun progressiveBlurShaderCompilesOnSupportedAndroid() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val blur = com.example.application.ui.effects.BottomBlur()
            blur.uniforms(1000f, 150f, 20f)
            assertNotNull(blur.effect)
        }
    }

    @Test fun homeLoadingCoversTheEntireNavigationWindowAndClears() {
        var loading by mutableStateOf(true)
        compose.setContent { ApplicationTheme {
            Box(Modifier.fillMaxSize().testTag("navigation_root")) {
                MainNavigation(user, {}, homeContent = { modifier ->
                    publishScreenLoading(if (loading) "Caricamento di prova" else null)
                    Text("Esplora di prova", modifier)
                })
            }
        } }
        val root = compose.onNodeWithTag("navigation_root").fetchSemanticsNode().boundsInRoot
        val overlay = compose.onNodeWithTag("screen_loading_overlay").fetchSemanticsNode().boundsInRoot
        assertEquals(root.left, overlay.left, .5f)
        assertEquals(root.top, overlay.top, .5f)
        assertEquals(root.width, overlay.width, .5f)
        assertEquals(root.height, overlay.height, .5f)
        screenshot("explore-fullscreen-loading")
        compose.runOnIdle { loading = false }
        compose.onNodeWithTag("screen_loading_overlay").assertDoesNotExist()
        compose.onNodeWithText("Account").performClick()
        compose.onNodeWithText("Il tuo spazio").assertIsDisplayed()
    }

    @Test fun floatingBarRemainsFixedWhileCardsScrollUnderItsFade() {
        compose.setContent { ApplicationTheme(darkTheme = false) {
            BackgroundHelper { MainNavigation(user, {}, homeContent = { modifier ->
                Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("scrolling_cards")
                    .padding(horizontal = 20.dp).padding(bottom = LocalNavigationTail.current), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    repeat(12) { index -> Surface(shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                        Column(Modifier.fillMaxWidth().padding(24.dp)) {
                            Text("Esperienza $index", style = MaterialTheme.typography.titleLarge)
                            Text("Contenuto per controllare sfocatura, contorni e sfumatura sotto la barra.")
                        }
                    } }
                }
            }) }
        } }
        compose.waitUntil(30_000) { com.example.application.service.media.WallpaperService.readyImage() != null }
        val before = compose.onNodeWithText("Account").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("scrolling_cards").performTouchInput { swipeUp() }
        compose.waitForIdle()
        assertEquals(before, compose.onNodeWithText("Account").fetchSemanticsNode().boundsInRoot)
        screenshot("navigation-fade-light")
    }

    private fun awaitNativeFrame() {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        // Semantics can be idle before SurfaceFlinger presents the frame read by UiAutomation.
        android.os.SystemClock.sleep(150)
    }

    private fun screenshot(name: String) {
        awaitNativeFrame()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null), "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
}
