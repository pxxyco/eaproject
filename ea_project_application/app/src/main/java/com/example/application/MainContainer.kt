package com.example.application

import android.app.Activity
import androidx.activity.compose.BackHandler
import com.example.application.dto.UserDTO
import com.example.application.screens.organizer.OrganizerDashboardScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    MainNavigation(user = UserSession.user, onLogout = { AuthRepository.logout() })
}

@Composable
internal fun MainNavigation(
    user: UserDTO?,
    onLogout: () -> Unit,
    homeContent: @Composable (Modifier) -> Unit = { HomeScreen(it) },
    tripsContent: @Composable (Modifier) -> Unit = { MyTrip(it) },
    dashboardContent: @Composable (Long, Modifier) -> Unit = { id, modifier -> OrganizerDashboardScreen(id, modifier) }
) {
    var destination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var returnToPersonalTab by rememberSaveable { mutableStateOf(false) }
    val organizer = user?.role?.trim().equals("ORGANIZER", ignoreCase = true)
    val current = when (destination) {
        AppDestinations.DASHBOARD, AppDestinations.`MY-TRIP` -> if (organizer) AppDestinations.DASHBOARD else AppDestinations.`MY-TRIP`
        else -> destination
    }
    BackHandler(enabled = current != AppDestinations.HOME) {
        destination = AppDestinations.HOME
        returnToPersonalTab = false
    }
    NavigationSuiteScaffold(navigationSuiteItems = {
        listOf(AppDestinations.HOME, if (organizer) AppDestinations.DASHBOARD else AppDestinations.`MY-TRIP`, AppDestinations.PROFILE).forEach { destinationItem ->
            item(
                icon = { Icon(painterResource(destinationItem.icon), contentDescription = null) },
                label = { Text(destinationItem.label) },
                selected = destinationItem == current,
                onClick = {
                    destination = destinationItem
                    returnToPersonalTab = false
                }
            )
        }
    }) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            val contentModifier = Modifier.padding(innerPadding)
            key(user?.id, user?.role) {
                when (current) {
                    AppDestinations.HOME -> homeContent(contentModifier)
                    AppDestinations.`MY-TRIP` -> {
                        if (user == null) MyTrip(modifier = contentModifier, onLoginClick = {
                            returnToPersonalTab = true
                            destination = AppDestinations.PROFILE
                        }) else tripsContent(contentModifier)
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
