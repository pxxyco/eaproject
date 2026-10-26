package com.example.application.screens.favorite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.application.dto.ExperienceRef
import com.example.application.network.MyTripRepository
import com.example.application.network.UserSession
import com.example.application.screens.home.SectionTitle
import com.example.application.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTrip(
    onExperienceClick: (ExperienceRef) -> Unit,
    modifier: Modifier = Modifier,
    onLoginClick: () -> Unit = {}
) {
    if (!UserSession.isLoggedIn) {
        LoginRequired(modifier = modifier, onLoginClick = onLoginClick)
        return
    }

    var selected by remember { mutableStateOf<MyTripRepository.HubTarget?>(null) }

    selected?.let { target ->
        CollectionScreen(
            target = target,
            onBack = { selected = null },
            onExperienceClick = onExperienceClick,
            modifier = modifier
        )
        return
    }

    val lists = MyTripRepository.lists
    val upcoming = MyTripRepository.upcomingBookings
    val favorites = MyTripRepository.favoritesCount

    val scope = rememberCoroutineScope()
    var showNewList by remember { mutableStateOf(false) }
    var listsError by remember { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }

    // Si rilancia ogni volta che l'hub torna visibile (anche uscendo da una raccolta).
    LaunchedEffect(Unit) {
        listsError = try {
            MyTripRepository.refreshLists()
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.message ?: "Impossibile caricare le liste."
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        SectionTitle("Le mie liste")
        Spacer(Modifier.height(12.dp))

        listsError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item(key = "bookings") {
                ListRow(
                    title = "Le mie prenotazioni",
                    subtitle = if (upcoming == 0) "Nessuna in arrivo" else "$upcoming in arrivo",
                    icon = Icons.Default.DateRange,
                    onClick = { selected = MyTripRepository.HubTarget.Bookings }
                )
            }
            item(key = "favorites") {
                ListRow(
                    title = "Le mie preferite",
                    subtitle = countLabel(favorites),
                    icon = Icons.Default.Favorite,
                    onClick = { selected = MyTripRepository.HubTarget.Favorites }
                )
            }
            items(lists, key = { "list_${it.id}" }) { list ->
                ListRow(
                    title = list.name,
                    subtitle = "${countLabel(list.itemCount)} · ${list.visibility.label}",
                    icon = Icons.AutoMirrored.Filled.List,
                    onClick = { selected = MyTripRepository.HubTarget.Custom(list) }
                )
            }
            item(key = "new_list") {
                ListRow(
                    title = "Nuova lista",
                    subtitle = "Creane una personalizzata",
                    icon = Icons.Default.Add,
                    onClick = { showNewList = true },
                    muted = true
                )
            }
        }
    }

    if (showNewList) {
        NewListDialog(
            loading = creating,
            error = createError,
            onDismiss = {
                if (!creating) {
                    showNewList = false
                    createError = null
                }
            },
            onCreate = { name ->
                scope.launch {
                    creating = true
                    createError = null
                    try {
                        MyTripRepository.createList(name)
                        showNewList = false
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        createError = e.message ?: "Impossibile creare la lista."
                    } finally {
                        creating = false
                    }
                }
            }
        )
    }
}

private fun countLabel(n: Int) = if (n == 1) "1 elemento" else "$n elementi"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    muted: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (muted) OverlayDark else PopupSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Stone200, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Stone300,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Stone400)
        }
    }
}

@Composable
private fun NewListDialog(
    loading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Nuova lista") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text("Nome") },
                    singleLine = true,
                    enabled = !loading
                )
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name) },
                enabled = name.isNotBlank() && !loading
            ) {
                Text(if (loading) "Creazione..." else "Crea")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) { Text("Annulla") }
        }
    )
}

@Composable
private fun LoginRequired(modifier: Modifier = Modifier, onLoginClick: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Accedi per vedere le tue liste",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Prenotazioni, preferiti e liste personalizzate sono disponibili solo con un account.",
            style = MaterialTheme.typography.bodyMedium,
            color = Stone300,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onLoginClick) { Text("Accedi") }
    }
}