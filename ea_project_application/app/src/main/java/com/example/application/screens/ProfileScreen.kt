package com.example.application.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.application.ui.theme.*
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
import com.example.application.screens.login.AuthWizardScreen
import com.example.application.screens.organizer.OrganizerDashboardScreen
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(user: UserDTO?, onLoginSuccess: () -> Unit, onLogoutClick: () -> Unit, modifier: Modifier = Modifier) {
    Crossfade(targetState = user, modifier = modifier.fillMaxSize(), label = "profile_state") { current ->
        if (current == null) AuthWizardScreen(onLoginSuccess = onLoginSuccess, onBackToHome = {})
        else AccountShell(current, onLogoutClick)
    }
}

@Composable
private fun AccountShell(user: UserDTO, onLogoutClick: () -> Unit) {
    key(user.id) {
        var showLogoutDialog by remember { mutableStateOf(false) }
        var settings by rememberSaveable { mutableStateOf(false) }
        val drawer = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val organizer = user.role?.trim().equals("ORGANIZER", ignoreCase = true)
        BackHandler(enabled = drawer.isOpen || settings) {
            if (drawer.isOpen) scope.launch { drawer.close() } else settings = false
        }
        ModalNavigationDrawer(
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
                        onClick = { showLogoutDialog = true }, modifier = Modifier.padding(horizontal = 12.dp)
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
                    ProfileContent(user, { showLogoutDialog = true }, if (organizer) ({ settings = false }) else null, Modifier.weight(1f))
                }
            }
        }
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                shape = RoundedCornerShape(28.dp),
                title = { Text("Disconnettersi?") },
                text = { Text("Dovrai rifare il login per accedere al tuo account.") },
                confirmButton = { TextButton(onClick = { showLogoutDialog = false; onLogoutClick() }) { Text("Disconnetti") } },
                dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Annulla") } }
            )
        }
    }
}

@Composable
private fun ProfileContent(user: UserDTO, onLogoutClick: () -> Unit, onDashboard: (() -> Unit)?, modifier: Modifier) {

    val displayName = user.fullName?.takeIf { it.isNotBlank() } ?: user.username ?: "Utente"
    val initials = displayName.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
    val roleLabel = when (user.role) {
        "ORGANIZER" -> "Account Business (Organizer)"
        "TRAVELER" -> "Account Personal (Traveler)"
        else -> user.role ?: "Account"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Avatar con le iniziali (la foto profilo reale arriverà con una libreria immagini)
        Box(
            modifier = Modifier.size(96.dp).clip(CircleShape).background(Stone600),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = displayName,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "@${user.username.orEmpty()}",
            style = MaterialTheme.typography.bodyMedium,
            color = Stone400
        )

        Spacer(modifier = Modifier.height(12.dp))

        Surface(shape = RoundedCornerShape(8.dp), color = OverlayDark) {
            Text(
                text = roleLabel,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PopupSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow(label = "Email", value = user.email ?: "—")
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Stone600)
                InfoRow(label = "Username", value = user.username ?: "—")
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Stone600)
                InfoRow(label = "Accesso", value = if (user.oauthProvider) "Account esterno" else "Email e password")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        onDashboard?.let { action ->
            OutlinedButton(onClick = action, modifier = Modifier.fillMaxWidth()) { Text("Torna alla dashboard") }
            Spacer(Modifier.height(12.dp))
        }

        Button(
            onClick = onLogoutClick,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Text(text = "Disconnettiti", fontWeight = FontWeight.Bold)
        }
    }

}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Stone400)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = Color.White)
    }
}