package com.example.application

import android.app.Activity
import androidx.activity.compose.BackHandler
import com.example.application.dto.ExperienceRef
import com.example.application.dto.UserDTO
import com.example.application.screens.activity.ExperienceDetailScreen
import com.example.application.screens.organizer.OrganizerDashboardScreen
import androidx.compose.foundation.layout.*
import com.example.application.ui.background.BackgroundHelper
import androidx.compose.material3.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.application.ui.effects.navigationFade
import com.example.application.ui.effects.LocalNavigationTail
import com.example.application.screens.common.ScreenLoadingHost
import com.example.application.screens.common.LocalScreenLoadingHost
import com.example.application.screens.common.LoadingOverlay
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.application.network.ApiService
import com.example.application.network.AppStatus
import com.example.application.network.AuthRepository
import com.example.application.network.UserSession
import com.example.application.screens.AppErrorDialog
import com.example.application.screens.home.HomeScreen
import com.example.application.screens.favorite.MyTrip
import com.example.application.screens.ProfileScreen

@Composable
fun MainContainer() {
    val context = LocalContext.current
    LaunchedEffect(AppStatus.reloadTick) {
        if (!ApiService.isBackendReachable()) AppStatus.showNetworkError()
        else AuthRepository.restoreSession()
    }
    AppErrorDialog(onExit = { (context as? Activity)?.finishAffinity() })
    BackgroundHelper { MainNavigation(user = UserSession.user, onLogout = { AuthRepository.logout() }) }
}

@Composable
internal fun MainNavigation(
    user: UserDTO?,
    onLogout: () -> Unit,
    homeContent: (@Composable (Modifier) -> Unit)? = null,
    tripsContent: (@Composable (Modifier) -> Unit)? = null,
    dashboardContent: @Composable (Long, Modifier) -> Unit = { id, modifier -> OrganizerDashboardScreen(id, modifier) }
) {
    var destination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var returnToPersonalTab by rememberSaveable { mutableStateOf(false) }
    var openExperience by rememberSaveable(user?.id, user?.role, stateSaver = ExperienceRef.Saver) {
        mutableStateOf<ExperienceRef?>(null)
    }
    val organizer = user?.role?.trim().equals("ORGANIZER", ignoreCase = true)
    val current = when (destination) {
        AppDestinations.DASHBOARD, AppDestinations.`MY-TRIP` -> if (organizer) AppDestinations.DASHBOARD else AppDestinations.`MY-TRIP`
        else -> destination
    }
    BackHandler(enabled = current != AppDestinations.HOME) {
        destination = AppDestinations.HOME
        returnToPersonalTab = false
    }
    BackHandler(enabled = openExperience != null) { openExperience = null }
    val loadingHost = remember { ScreenLoadingHost() }
    Box(Modifier.fillMaxSize()) {
        val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Box(Modifier.fillMaxSize().padding(bottom = 70.dp + bottomInset).navigationFade()) {
            val contentModifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            CompositionLocalProvider(LocalNavigationTail provides 72.dp, LocalScreenLoadingHost provides loadingHost) {
                val experience = openExperience
                if (experience != null) {
                    key(experience) {
                        ExperienceDetailScreen(experience, onBack = { openExperience = null }, modifier = contentModifier)
                    }
                } else key(user?.id, user?.role) {
                    when (current) {
                        AppDestinations.HOME -> {
                            if (homeContent != null) homeContent(contentModifier)
                            else HomeScreen(onExperienceClick = { openExperience = it }, modifier = contentModifier)
                        }
                        AppDestinations.`MY-TRIP` -> {
                            if (user == null) MyTrip(modifier = contentModifier, onLoginClick = {
                                returnToPersonalTab = true
                                destination = AppDestinations.PROFILE
                            }) else if (tripsContent != null) tripsContent(contentModifier)
                            else MyTrip(onExperienceClick = { openExperience = it }, modifier = contentModifier)
                        }
                        AppDestinations.DASHBOARD -> user?.id?.let { dashboardContent(it, contentModifier) }
                        AppDestinations.PROFILE -> ProfileScreen(
                            user = user,
                            onLoginSuccess = {
                                if (returnToPersonalTab) destination = AppDestinations.`MY-TRIP`
                                returnToPersonalTab = false
                            },
                            onLogoutClick = {
                                returnToPersonalTab = false
                                destination = AppDestinations.PROFILE
                                onLogout()
                            },
                            modifier = contentModifier
                        )
                    }
                }
            }
        }
        com.example.application.ui.navigation.FloatingNavigation(
            destinations = listOf(AppDestinations.HOME, if (organizer) AppDestinations.DASHBOARD else AppDestinations.`MY-TRIP`, AppDestinations.PROFILE),
            selected = current,
            onSelect = { openExperience = null; destination = it; returnToPersonalTab = false },
            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter).navigationBarsPadding()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)).padding(horizontal = 18.dp, vertical = 12.dp))
        loadingHost.message?.let { LoadingOverlay(message = it, modifier = Modifier.fillMaxSize()) }
    }

}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("Esplora", R.drawable.ic_home),
    `MY-TRIP`("Preferiti", R.drawable.ic_favorite),
    DASHBOARD("Dashboard", R.drawable.ic_dashboard),
    PROFILE("Account", R.drawable.ic_account_box),
}
