package com.example.application.screens.account

import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import com.example.application.dto.UserDTO
import com.example.application.network.*
import com.example.application.ui.theme.ApplicationTheme
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class TravelerAccountTest {
    @get:Rule val compose = createComposeRule()
    private val user = UserDTO(id = 21, username = "mario", fullName = "Mario Rossi", email = "mario@example.it", role = "TRAVELER")
    private val gateway = FakeAccountGateway(user)
    private var logouts = 0
    private fun notificationPermission(grant: Boolean) {
        if (android.os.Build.VERSION.SDK_INT < 33) return
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val descriptor = instrumentation.uiAutomation.executeShellCommand(
            "pm ${if (grant) "grant" else "revoke"} ${instrumentation.targetContext.packageName} android.permission.POST_NOTIFICATIONS")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
    }

    private fun open() {
        UserSession.setSession(user)
        compose.setContent { ApplicationTheme {
            androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize().background(com.example.application.ui.theme.ScreenBackground)) {
                TravelerAccountScreen(UserSession.user ?: user, { logouts++ }, gateway = gateway)
            }
        } }
    }
    @After fun clearSession() { UserSession.clear() }

    @Test fun profileSavesBackendResponseAndKeepsTravelerInEditor() {
        open()
        screenshot("account-overview")
        compose.onNodeWithText("Il mio profilo").performScrollTo().performClick()
        compose.onNodeWithText("Nome completo").performScrollTo().performTextReplacement("Mario Verdi")
        compose.onNodeWithText("Salva modifiche").performScrollTo().performClick()
        compose.waitUntil(5000) { gateway.savedProfile != null }
        compose.runOnIdle { assertEquals("Mario Verdi", UserSession.user?.fullName) }
        compose.onNodeWithText("Nome completo").assertExists()
        compose.onNodeWithText("Il mio profilo").assertExists()
        compose.onNodeWithText("Una foto per farti riconoscere").performScrollTo()
        screenshot("account-profile")
    }

    @Test fun preferencesLoadOnlyWhenOpenedAndPersistUserChanges() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        notificationPermission(true)
        com.example.application.device.NotificationChannels.create(instrumentation.targetContext)
        open()
        compose.runOnIdle { assertEquals(0, gateway.preferenceLoads) }
        compose.onNodeWithText("Impostazioni e sicurezza").performScrollTo().performClick()
        compose.waitUntil(5000) { gateway.preferenceLoads == 1 }
        compose.onAllNodes(isToggleable())[0].performScrollTo().performClick()
        compose.onNodeWithText("Salva preferenze").performScrollTo().performClick()
        compose.waitUntil(5000) { gateway.savedPreferences != null }
        compose.runOnIdle { assertFalse(gateway.savedPreferences!!.bookingAlerts) }
        compose.onNodeWithText("Scegli cosa ricevere").performScrollTo()
        screenshot("account-security")
    }

    @Test fun failedCardDeletionKeepsCardThenSuccessfulRetryRemovesIt() {
        open()
        compose.onNodeWithText("Metodi di pagamento").performScrollTo().performClick()
        compose.waitUntil(5000) { gateway.cardLoads > 0 }
        compose.onNodeWithText("Rimuovi carta").performScrollTo().performClick()
        gateway.failDelete = true
        compose.onAllNodesWithText("Rimuovi").onLast().performClick()
        compose.waitUntil(5000) { gateway.deletions == 1 }
        compose.onNodeWithText("••••  ••••  1234").assertExists()
        gateway.failDelete = false
        compose.onNodeWithText("Rimuovi carta").performScrollTo().performClick()
        compose.onAllNodesWithText("Rimuovi").onLast().performClick()
        compose.waitUntil(5000) { gateway.deletions == 2 }
        compose.onNodeWithText("••••  ••••  1234").assertDoesNotExist()
    }

    @Test fun supportPagesReplacePreviousTicketsAndLogoutRequiresConfirmation() {
        open()
        compose.onNodeWithText("Centro assistenza").performScrollTo().performClick()
        compose.waitUntil(5000) { gateway.ticketPages.isNotEmpty() }
        compose.onNodeWithText("Successiva").performScrollTo().performClick()
        compose.waitUntil(5000) { gateway.ticketPages.contains(1) }
        compose.onNodeWithText("Richiesta precedente").performScrollTo().performClick()
        compose.onNodeWithText("Chiudi dettaglio").assertIsDisplayed()
        compose.onNode(hasText("Messaggio") and !hasSetTextAction()).assertIsDisplayed()
        compose.onNodeWithText("Chiudi dettaglio").performClick()
        compose.onNodeWithText("Richiesta 0").assertDoesNotExist()
        compose.onNodeWithText("‹  Account").performScrollTo().performClick()
        compose.onNodeWithText("Disconnettiti").performScrollTo().performClick()
        compose.onNodeWithText("Annulla").performClick()
        compose.runOnIdle { assertEquals(0, logouts); assertEquals(0, gateway.revocations) }
        compose.onNodeWithText("Disconnettiti").performScrollTo().performClick()
        compose.onNodeWithText("Disconnetti").performClick()
        compose.waitUntil(5000) { logouts == 1 }
        compose.runOnIdle { assertEquals(1, gateway.revocations) }
    }

    @Test fun cardNetworkIsDetectedAndOnlyMetadataIsSubmitted() {
        open()
        compose.onNodeWithText("Metodi di pagamento").performScrollTo().performClick()
        compose.waitUntil(5000) { gateway.cardLoads > 0 }
        compose.onNodeWithText("Numero carta").performScrollTo().performTextInput("4111111111111111")
        compose.onNodeWithText("Circuito: Visa").assertExists()
        compose.onNodeWithText("Intestatario").performScrollTo().performTextInput("Mario Rossi")
        compose.onNodeWithText("Scadenza (MM/AA)").performScrollTo().performTextInput("0")
        compose.onNodeWithText("Scadenza (MM/AA)").performTextInput("2")
        compose.onNodeWithText("Scadenza (MM/AA)").performTextInput("2")
        compose.onNodeWithText("Scadenza (MM/AA)").performTextInput("7")
        compose.onNodeWithText("Salva carta").performScrollTo().performClick()
        compose.waitUntil(5000) { gateway.savedCard != null }
        compose.runOnIdle {
            assertEquals("Visa", gateway.savedCard!!.brand)
            assertEquals("1111", gateway.savedCard!!.last4)
            assertEquals(2027, gateway.savedCard!!.expiryYear)
            assertEquals(2, gateway.savedCard!!.expiryMonth)
        }
        compose.onNodeWithText("••••  ••••  1111").assertExists()
    }

    @Test fun nfcDoesNotReadOrSaveAnythingBeforeConsent() {
        open()
        compose.onNodeWithText("Metodi di pagamento").performScrollTo().performClick()
        compose.waitUntil(5000) { gateway.cardLoads > 0 }
        compose.onNodeWithText("NFC").performScrollTo().performClick()
        compose.onNodeWithText("Leggi la carta con NFC").assertIsDisplayed()
        compose.runOnIdle { assertNull(gateway.savedCard) }
        compose.onNodeWithText("Annulla").performClick()
        compose.onNodeWithText("Leggi la carta con NFC").assertDoesNotExist()
    }

    @Test fun cameraRequiresConsentBeforeItCanReadOrSaveACard() {
        open()
        compose.onNodeWithText("Metodi di pagamento").performScrollTo().performClick()
        compose.waitUntil(5000) { gateway.cardLoads > 0 }
        compose.onNodeWithText("Camera").performScrollTo().performClick()
        compose.onNodeWithText("Scansiona la carta").assertIsDisplayed()
        compose.runOnIdle { assertNull(gateway.savedCard) }
        compose.onNodeWithText("Annulla").performClick()
        compose.onNodeWithText("Scansiona la carta").assertDoesNotExist()
    }

    @Test fun wallpaperIsSharedAcrossAccountNavigation() {
        UserSession.setSession(user)
        compose.setContent { ApplicationTheme {
            com.example.application.ui.background.BackgroundHelper {
                com.example.application.MainNavigation(user, {}, homeContent = { androidx.compose.material3.Text("Esplora di prova", it) })
            }
        } }
        compose.onNodeWithText("Account").performClick()
        compose.waitUntil(30_000) { com.example.application.service.media.WallpaperService.readyImage() != null }
        val wallpaper = com.example.application.service.media.WallpaperService.readyImage()
        screenshot("account-wallpaper-navigation")
        compose.onNodeWithText("Il mio profilo").performScrollTo().performClick()
        compose.onNodeWithText("Una foto per farti riconoscere").assertExists()
        compose.runOnIdle { assertSame(wallpaper, com.example.application.service.media.WallpaperService.readyImage()) }
        screenshot("account-wallpaper-profile")
    }

    @Test fun passwordMeterUpdatesAsTheNewPasswordChanges() {
        open()
        compose.onNodeWithText("Impostazioni e sicurezza").performScrollTo().performClick()
        compose.onNodeWithText("Nuova password").performScrollTo().performTextInput("Mare28!x")
        compose.onNodeWithText("Robustezza: Discreta").assertExists()
        compose.onNodeWithText("Nuova password").performTextReplacement("Sentieri!Luna2027#")
        compose.onNodeWithText("Robustezza: Molto forte").assertExists()
    }

    @Test fun successfulPasswordChangeRequiresNewLogin() {
        open()
        compose.onNodeWithText("Impostazioni e sicurezza").performScrollTo().performClick()
        compose.onNodeWithText("Password attuale").performScrollTo().performTextInput("OldPassword1!")
        compose.onNodeWithText("Nuova password").performScrollTo().performTextInput("NewPassword2!")
        compose.onNodeWithText("Ripeti la nuova password").performScrollTo().performTextInput("NewPassword2!")
        compose.onNodeWithText("Aggiorna password").performScrollTo().performClick()
        compose.onNodeWithText("Password aggiornata").assertIsDisplayed()
        compose.onNodeWithText("Torna all’accesso").performClick()
        compose.runOnIdle { assertEquals(1, logouts); assertEquals("NewPassword2!", gateway.changedPassword) }
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), "$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}

private class FakeAccountGateway(private val user: UserDTO) : AccountGateway {
    var savedProfile: ProfileInput? = null
    var preferenceLoads = 0
    var cardLoads = 0
    var savedPreferences: AccountPreferences? = null
    var failDelete = false
    var deletions = 0
    var revocations = 0
    var changedPassword: String? = null
    var savedCard: CardInput? = null
    val ticketPages = mutableListOf<Int>()
    override suspend fun saveProfile(input: ProfileInput): ApiResult<UserDTO> {
        savedProfile = input
        return ApiResult.Success(user.copy(fullName = input.fullName, email = input.email, username = input.username))
    }
    override suspend fun removePhoto() = ApiResult.Success(user.copy(profilePhotoUrl = null))
    override suspend fun uploadPhoto(bytes: ByteArray, mime: String) = ApiResult.Success(user.copy(profilePhotoUrl = "/photo/profile/test.jpg"))
    override suspend fun preferences(): ApiResult<AccountPreferences> { preferenceLoads++; return ApiResult.Success(AccountPreferences()) }
    override suspend fun savePreferences(input: AccountPreferences): ApiResult<AccountPreferences> { savedPreferences = input; return ApiResult.Success(input) }
    override suspend fun cards(): ApiResult<Array<SavedCard>> { cardLoads++; return ApiResult.Success(arrayOf(SavedCard(1, "Visa", "1234", "Mario Rossi", 12, 2030))) }
    override suspend fun addCard(input: CardInput): ApiResult<SavedCard> {
        savedCard = input
        return ApiResult.Success(SavedCard(2, input.brand, input.last4, input.holder, input.expiryMonth, input.expiryYear))
    }
    override suspend fun removeCard(id: Long): ApiResult<Unit> { deletions++; return if (failDelete) ApiResult.HttpError(500, "") else ApiResult.Success(Unit) }
    override suspend fun tickets(page: Int): ApiResult<Array<SupportTicket>> {
        ticketPages.add(page)
        return ApiResult.Success(if (page == 0) Array(20) { SupportTicket(it.toLong(), "Account", "Richiesta $it", "Messaggio", "APERTO", "2026-10-27T12:00:00Z") }
            else arrayOf(SupportTicket(30, "Account", "Richiesta precedente", "Messaggio", "APERTO", "2026-10-25T12:00:00Z")))
    }
    override suspend fun sendTicket(input: TicketInput) = ApiResult.Success(SupportTicket(40, input.category, input.subject, input.message, "APERTO", null))
    override suspend fun changePassword(current: String, new: String): ApiResult<Unit> { changedPassword = new; return ApiResult.Success(Unit) }
    override suspend fun revokeSessions(): ApiResult<Unit> { revocations++; return ApiResult.Success(Unit) }
}
