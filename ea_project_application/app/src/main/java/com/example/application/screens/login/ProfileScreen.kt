package com.example.application.screens

import com.example.application.ui.effects.LocalNavigationTail
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.application.dto.UserDTO
import com.example.application.screens.account.TravelerAccountScreen
import com.example.application.screens.login.AuthWizardScreen
import com.example.application.ui.theme.*

@Composable
fun ProfileScreen(user: UserDTO?, onLoginSuccess: () -> Unit, onLogoutClick: () -> Unit, modifier: Modifier = Modifier) {
    if (user?.role.equals("TRAVELER", ignoreCase = true)) {
        key(user?.id) { TravelerAccountScreen(user!!, onLogoutClick, modifier) }
        return
    }
    Crossfade(targetState = user, modifier = modifier.fillMaxSize(), label = "profile_state") { current ->
        if (current == null) {
            AuthWizardScreen(onLoginSuccess = onLoginSuccess, onBackToHome = {})
        } else key(current.id) {
            var showLogoutDialog by remember { mutableStateOf(false) }
            ProfileContent(current, { showLogoutDialog = true }, Modifier.fillMaxSize())
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
}

@Composable
private fun ProfileContent(user: UserDTO, onLogoutClick: () -> Unit, modifier: Modifier) {

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
            .background(androidx.compose.ui.graphics.Color.Transparent)
            .verticalScroll(rememberScrollState()).padding(bottom = LocalNavigationTail.current)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier.size(96.dp).clip(CircleShape).background(Stone600),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = displayName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
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
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}