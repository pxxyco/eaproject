package com.example.application

import android.app.Activity
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
    val current = destinationForUser(destination, user)
    BackHandler(enabled = current != AppDestinations.HOME) {
        destination = AppDestinations.HOME
        returnToPersonalTab = false
    }
    NavigationSuiteScaffold(navigationSuiteItems = {
        destinationsFor(user).forEach { destinationItem ->
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
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            key(UserSession.user?.id, UserSession.role) {
            when (currentDestination) {
                AppDestinations.HOME -> HomeScreen(modifier = Modifier.padding(innerPadding))
                AppDestinations.`MY-TRIP` -> MyTrip(modifier = Modifier.padding(innerPadding), onLoginClick = { currentDestination = AppDestinations.PROFILE })
                AppDestinations.PROFILE -> ProfileScreen(
                    user = UserSession.user,
                    onLoginSuccess = { },
                    onLogoutClick = { AuthRepository.logout() },
                    modifier = Modifier.padding(innerPadding)
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
    `MY-TRIP`("Preferiti", R.drawable.ic_favorite), //TODO Sostituire con Preferiti e non Trip (Trip è gestito dalla home inseme ad Activity)
    PROFILE("Account", R.drawable.ic_account_box),
}
