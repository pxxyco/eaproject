package com.example.application

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.edit
import com.example.application.network.ApiService
import com.example.application.network.AppStatus
import com.example.application.network.AuthRepository
import com.example.application.network.UserSession
import com.example.application.screens.AppErrorDialog
import com.example.application.screens.HomeScreen
import com.example.application.screens.MyTrip
import com.example.application.screens.ProfileScreen

@Composable
fun MainContainer() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isLoggedIn = UserSession.isLoggedIn

    LaunchedEffect(AppStatus.reloadTick) {
        if (!ApiService.isBackendReachable()) {
            AppStatus.showNetworkError()
        } else {
            AuthRepository.restoreSession()
        }
    }

    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    AppErrorDialog(onExit = { (context as? Activity)?.finishAffinity() })

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach { destination ->
                item(
                    icon = {
                        Icon(
                            painterResource(destination.icon),
                            contentDescription = destination.label
                        )
                    },
                    label = { Text(destination.label) },
                    selected = destination == currentDestination,
                    onClick = { currentDestination = destination }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.HOME -> HomeScreen(modifier = Modifier.padding(innerPadding))
                AppDestinations.`MY-TRIP` -> MyTrip(modifier = Modifier.padding(innerPadding))
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

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("Esplora", R.drawable.ic_home),
    `MY-TRIP`("Viaggi", R.drawable.ic_favorite), //TODO Sostituire con Preferiti e non Trip (Trip è gestito dalla home inseme ad Activity)
    PROFILE("Account", R.drawable.ic_account_box),
}