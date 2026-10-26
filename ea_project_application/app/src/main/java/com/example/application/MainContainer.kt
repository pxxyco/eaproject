package com.example.application

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.application.dto.UserDTO
import com.example.application.network.*
import com.example.application.screens.*
import com.example.application.screens.organizer.OrganizerDashboardScreen

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
    }) {
        Scaffold(Modifier.fillMaxSize()) { padding ->
            val contentModifier = Modifier.padding(padding).fillMaxSize()
            // A different account must never reuse the previous user's screen state.
            key(user?.id, user?.role) {
                when (current) {
                    AppDestinations.HOME -> homeContent(contentModifier)
                    AppDestinations.`MY-TRIP` -> {
                        if (user == null) LoginRequiredScreen(
                            onLogin = { returnToPersonalTab = true; destination = AppDestinations.PROFILE },
                            onExplore = { destination = AppDestinations.HOME }, modifier = contentModifier
                        ) else tripsContent(contentModifier)
                    }
                    AppDestinations.DASHBOARD -> {
                        val id = user?.id
                        if (id != null && user.isOrganizer()) dashboardContent(id, contentModifier)
                        else LoginRequiredScreen(
                            onLogin = { destination = AppDestinations.PROFILE },
                            onExplore = { destination = AppDestinations.HOME }, modifier = contentModifier
                        )
                    }
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

@Composable
private fun LoginRequiredScreen(onLogin: () -> Unit, onExplore: () -> Unit, modifier: Modifier) {
    Column(modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text("I tuoi viaggi, in un unico posto", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text("Accedi o crea un account per aprire la tua area personale.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("Accedi o registrati") }
        TextButton(onClick = onExplore) { Text("Continua a esplorare") }
    }
}
