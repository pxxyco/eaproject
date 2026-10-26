package com.example.application

import android.app.Activity
import androidx.activity.compose.BackHandler
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
import com.example.application.dto.ExperienceRef
import com.example.application.network.ApiService
import com.example.application.network.AppStatus
import com.example.application.network.AuthRepository
import com.example.application.network.UserSession
import com.example.application.screens.AppErrorDialog
import com.example.application.screens.home.HomeScreen
import com.example.application.screens.favorite.MyTrip
import com.example.application.screens.ProfileScreen
import com.example.application.screens.activity.ExperienceDetailScreen

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
    var openExperience by rememberSaveable(stateSaver = ExperienceRef.Saver) {
        mutableStateOf<ExperienceRef?>(null)
    }
    BackHandler(enabled = openExperience != null) { openExperience = null }
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
                    onClick = {
                        openExperience = null
                        currentDestination = destination
                    }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            val experience = openExperience
            if (experience != null) {
                ExperienceDetailScreen(
                    experience = experience,
                    onBack = { openExperience = null },
                    modifier = Modifier.padding(innerPadding)
                )
            } else when (currentDestination) {
                AppDestinations.HOME -> HomeScreen(
                    onExperienceClick = {openExperience = it },
                    modifier = Modifier.padding(innerPadding)
                )
                AppDestinations.`MY-TRIP` -> MyTrip(
                    onExperienceClick = {openExperience = it },
                    modifier = Modifier.padding(innerPadding)
                )
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
    `MY-TRIP`("Preferiti", R.drawable.ic_favorite), //TODO Sostituire con Preferiti e non Trip (Trip è gestito dalla home inseme ad Activity)
    PROFILE("Account", R.drawable.ic_account_box),
}