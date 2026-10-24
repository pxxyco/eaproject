package com.example.application.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.application.R
import com.example.application.dto.UserDTO
import com.example.application.network.UserSession
import com.example.application.screens.login.AuthWizardScreen
import com.example.application.screens.organizer.OrganizerDashboardScreen
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    isLoggedIn: Boolean,
    onLoginSuccess: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val user = UserSession.user
    if (!isLoggedIn || user == null) {
        Box(modifier.fillMaxSize()) {
            AuthWizardScreen(onLoginSuccess = onLoginSuccess, onBackToHome = {})
        }
        return
    }
    key(user.id) {
        var settings by rememberSaveable { mutableStateOf(false) }
        val drawer = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val organizer = user.role?.trim().equals("ORGANIZER", ignoreCase = true)
        BackHandler(enabled = drawer.isOpen || settings) {
            if (drawer.isOpen) scope.launch { drawer.close() } else settings = false
        }
        ModalNavigationDrawer(
            modifier = modifier,
            drawerState = drawer,
            gesturesEnabled = drawer.isOpen,
            drawerContent = {
                ModalDrawerSheet {
                    Column(Modifier.padding(24.dp)) {
                        Text(user.fullName ?: user.username.orEmpty(), style = MaterialTheme.typography.titleLarge)
                        Text(user.email.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (organizer) NavigationDrawerItem(
                        label = { Text("Dashboard organizzatore") }, selected = !settings,
                        onClick = { settings = false; scope.launch { drawer.close() } },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text("Impostazioni account") }, selected = settings || !organizer,
                        onClick = { settings = true; scope.launch { drawer.close() } },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(Modifier.padding(vertical = 16.dp))
                    NavigationDrawerItem(
                        label = { Text("Disconnettiti", color = MaterialTheme.colorScheme.error) }, selected = false,
                        onClick = onLogoutClick, modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        ) {
            Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (settings || !organizer) "Impostazioni account" else "Account Business",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                    IconButton(onClick = { scope.launch { drawer.open() } }) {
                        Icon(painterResource(R.drawable.ic_menu), contentDescription = "Apri menu account")
                    }
                }
                if (organizer && !settings && user.id != null) {
                    OrganizerDashboardScreen(userId = user.id, modifier = Modifier.weight(1f))
                } else {
                    AccountSettings(user, onLogoutClick, if (organizer) ({ settings = false }) else null, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AccountSettings(user: UserDTO, onLogout: () -> Unit, onDashboard: (() -> Unit)?, modifier: Modifier) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val initials = (user.fullName ?: user.username.orEmpty()).split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
            Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) { Text(initials, style = MaterialTheme.typography.headlineMedium) }
        }
        Text(user.fullName.orEmpty(), style = MaterialTheme.typography.titleLarge)
        Text("@${user.username.orEmpty()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(if (user.role?.trim().equals("ORGANIZER", true)) "Account Business (Organizzatore)" else "Account Viaggiatore")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AccountField("Email", user.email.orEmpty())
                HorizontalDivider()
                AccountField("Username", user.username.orEmpty())
                HorizontalDivider()
                AccountField("Accesso", if (user.oauthProvider) "Account esterno" else "Email e password")
            }
        }
        onDashboard?.let { OutlinedButton(onClick = it, modifier = Modifier.fillMaxWidth()) { Text("Torna alla dashboard") } }
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) { Text("Disconnettiti") }
    }
}

@Composable
private fun AccountField(label: String, value: String) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = MaterialTheme.typography.bodyLarge)
}
