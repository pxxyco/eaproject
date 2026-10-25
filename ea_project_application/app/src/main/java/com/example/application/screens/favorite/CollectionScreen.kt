package com.example.application.screens.favorite

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.application.dto.ActivityHomeDTO
import com.example.application.dto.TripHomeDTO
import com.example.application.network.MyTripRepository
import com.example.application.screens.common.LoadingOverlay
import com.example.application.screens.home.ActivityGrid
import com.example.application.screens.home.SectionTitle
import com.example.application.screens.home.TripRow
import com.example.application.ui.theme.ScreenBackground
import kotlinx.coroutines.CancellationException

@Composable
fun CollectionScreen(
    target: MyTripRepository.HubTarget,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var trips by remember(target) { mutableStateOf<List<TripHomeDTO>>(emptyList()) }
    var activities by remember(target) { mutableStateOf<List<ActivityHomeDTO>>(emptyList()) }
    var loading by remember(target) { mutableStateOf(true) }
    var error by remember(target) { mutableStateOf<String?>(null) }

    LaunchedEffect(target) {
        loading = true
        error = null
        try {
            val items = MyTripRepository.loadItems(target)
            trips = items.trips.orEmpty()
            activities = items.activities.orEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message?.takeIf { it.isNotBlank() } ?: "Impossibile caricare gli elementi."
        } finally {
            loading = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Indietro",
                        tint = androidx.compose.ui.graphics.Color.White
                    )
                }
                SectionTitle(target.title)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (error == null && !loading) {
                    SectionTitle("Viaggi")
                    Spacer(Modifier.height(12.dp))
                    if (trips.isEmpty()) EmptyMessage("Nessun viaggio qui.")
                    else TripRow(trips)

                    Spacer(Modifier.height(28.dp))

                    SectionTitle("Attività")
                    Spacer(Modifier.height(12.dp))
                    if (activities.isEmpty()) EmptyMessage("Nessuna attività qui.")
                    else ActivityGrid(activities)
                }

                Spacer(Modifier.height(32.dp))
            }
        }

        if (loading) LoadingOverlay(message = "Caricamento in corso...")
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}