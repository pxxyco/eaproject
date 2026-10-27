package com.example.application.screens.account

import com.example.application.ui.effects.LocalNavigationTail
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.application.dto.UserDTO
import com.example.application.network.*
import com.example.application.ui.theme.ApplicationTheme
import androidx.compose.ui.tooling.preview.Preview

private enum class AccountPage(val title: String, val subtitle: String) {
    OVERVIEW("Il tuo spazio", "Il tuo prossimo viaggio comincia da te."),
    PROFILE("Il mio profilo", "Le informazioni che rendono tuo questo spazio."),
    SECURITY("Impostazioni e sicurezza", "Scegli come accedere e cosa ricevere."),
    PAYMENTS("Metodi di pagamento", "Le tue carte, sempre a portata di mano."),
    SUPPORT("Centro assistenza", "Risposte pratiche e richieste sempre a portata di mano.")
}

@Composable
internal fun TravelerAccountScreen(user: UserDTO, onLogout: () -> Unit, modifier: Modifier = Modifier, gateway: AccountGateway = AccountRepository) {
    var page by rememberSaveable { mutableStateOf(AccountPage.OVERVIEW) }
    var logoutDialog by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val actions = remember { AccountActions(scope, snackbar) }
    BackHandler(page != AccountPage.OVERVIEW && !actions.busy) { page = AccountPage.OVERVIEW }
    Scaffold(modifier = modifier.fillMaxSize(), snackbarHost = { SnackbarHost(snackbar, modifier = Modifier.padding(bottom = LocalNavigationTail.current)) }, containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Crossfade(page, label = "account_page") { current ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(bottom = LocalNavigationTail.current).padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (current != AccountPage.OVERVIEW) OutlinedButton(enabled = !actions.busy,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = com.example.application.ui.theme.NavigationAccent),
                            border = androidx.compose.foundation.BorderStroke(1.dp, com.example.application.ui.theme.NavigationOutline), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), onClick = { page = AccountPage.OVERVIEW }) { Text("‹  Account") }
                        else Text("EA PROJECT  /  TRAVELER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(current.title, style = MaterialTheme.typography.headlineMedium)
                        Text(current.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    when (current) {
                        AccountPage.OVERVIEW -> {
                            AccountPanel("Benvenuto, ${user.fullName?.substringBefore(" ") ?: user.username.orEmpty()}") {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    AccountAvatar(user, Modifier.size(72.dp))
                                    Column {
                                        Text(user.fullName ?: user.username.orEmpty(), fontWeight = FontWeight.Bold)
                                        Text("@${user.username.orEmpty()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Account Traveler", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                            AccountPanel("Il tuo account", "Dati, accesso e preferenze: tutto nello stesso posto.") {
                                AccountEntry("👤", "Il mio profilo", "Foto e informazioni personali", !actions.busy) { page = AccountPage.PROFILE }
                                AccountEntry("🔐", "Impostazioni e sicurezza", "Password, sessioni e preferenze", !actions.busy) { page = AccountPage.SECURITY }
                                AccountEntry("💳", "Metodi di pagamento", "Gestisci le tue carte", !actions.busy) { page = AccountPage.PAYMENTS }
                                AccountEntry("💬", "Centro assistenza", "FAQ e le tue richieste", !actions.busy) { page = AccountPage.SUPPORT }
                            }
                            Surface(onClick = { logoutDialog = true }, enabled = !actions.busy,
                                modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                                color = com.example.application.ui.theme.GlassFill, contentColor = MaterialTheme.colorScheme.onSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.application.ui.theme.GlassBorder)) {
                                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("🚪", fontSize = 28.sp)
                                    Column(Modifier.weight(1f)) {
                                        Text("Disconnettiti", style = MaterialTheme.typography.titleMedium)
                                        Text("Esci dal tuo account in sicurezza", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("›", style = MaterialTheme.typography.titleLarge)
                                }
                            }
                        }
                        AccountPage.PROFILE -> ProfileEditor(user, actions, gateway)
                        AccountPage.SECURITY -> SecuritySettings(user, actions, onLogout, gateway)
                        AccountPage.PAYMENTS -> PaymentMethods(actions, gateway)
                        AccountPage.SUPPORT -> SupportCenter(actions, gateway)
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            if (actions.busy) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }
    }
    if (logoutDialog) AlertDialog(onDismissRequest = { if (!actions.busy) logoutDialog = false }, title = { Text("Disconnettersi?") },
        text = { Text("Dovrai rifare il login per accedere al tuo account.") },
        confirmButton = { TextButton(enabled = !actions.busy, onClick = { actions.run {
            when (val result = gateway.revokeSessions()) {
                is ApiResult.Success -> { logoutDialog = false; onLogout(); null }
                is ApiResult.HttpError -> if (result.code == 401) { logoutDialog = false; onLogout(); null } else result.accountError()
                else -> result.accountError()
            }
        } }) { Text("Disconnetti") } },
        dismissButton = { TextButton(enabled = !actions.busy, onClick = { logoutDialog = false }) { Text("Annulla") } })
}

@Composable
private fun AccountEntry(emoji: String, title: String, subtitle: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp), color = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp), color = com.example.application.ui.theme.GlassFillStrong) {
                Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) { Text(emoji, fontSize = 28.sp) }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
internal fun AccountAvatar(user: UserDTO, modifier: Modifier) {
    Box(modifier.clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
        Text((user.fullName ?: user.username ?: "U").split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() },
            style = MaterialTheme.typography.headlineMedium)
        user.profilePhotoUrl?.takeIf { it.isNotBlank() }?.let {
            AsyncImage(ApiService.resolveUrl(it), "Foto profilo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}

@Preview(name = "Account Traveler", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun TravelerAccountPreview() {
    ApplicationTheme {
        TravelerAccountScreen(UserDTO(id = 1, username = "mario.rossi", fullName = "Mario Rossi",
            email = "mario@example.it", role = "TRAVELER"), onLogout = {})
    }
}
