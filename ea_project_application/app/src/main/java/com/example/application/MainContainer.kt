package com.example.application

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.application.dto.ExperienceRef
import com.example.application.dto.UserDTO
import com.example.application.network.ApiService
import com.example.application.network.AppStatus
import com.example.application.network.AuthRepository
import com.example.application.network.UserSession
import com.example.application.screens.AppErrorDialog
import com.example.application.screens.ProfileScreen
import com.example.application.screens.activity.ExperienceDetailScreen
import com.example.application.screens.favorite.MyTrip
import com.example.application.screens.favorite.LoginRequired
import com.example.application.screens.home.HomeScreen
import com.example.application.screens.organizer.OrganizerDashboardScreen
import com.example.application.ui.background.BackgroundHelper
import com.example.application.ui.navigation.FloatingNavigationLayout

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
    FloatingNavigationLayout(
        destinations = listOf(AppDestinations.HOME, if (organizer) AppDestinations.DASHBOARD else AppDestinations.`MY-TRIP`, AppDestinations.PROFILE),
        selected = current,
        onSelect = { openExperience = null; destination = it; returnToPersonalTab = false },
        fadeContent = openExperience == null
    ) { contentModifier ->
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
                    if (user == null) LoginRequired(modifier = contentModifier, onLoginClick = {
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

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("Esplora", R.drawable.ic_home),
    `MY-TRIP`("Preferiti", R.drawable.ic_favorite),
    DASHBOARD("Dashboard", R.drawable.ic_dashboard),
    PROFILE("Account", R.drawable.ic_account_box),
}
