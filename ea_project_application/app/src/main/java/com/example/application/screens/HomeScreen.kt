package com.example.application.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.application.dto.home.ActivityHomeDTO
import com.example.application.dto.home.TripHomeDTO
import com.example.application.repository.HomeRepository
import com.example.application.screens.home.ExperienceCard
import com.example.application.screens.home.SearchTopBar
import com.example.application.screens.home.TripCard
import com.example.application.ui.theme.ScreenBackground
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import com.example.application.network.AppStatus

private data class ActivityCategory(
    val title: String,
    val backendCategory: String
)

private val activityCategories = listOf(
    ActivityCategory("Escursioni e Trekking", "EXCURSION"),
    ActivityCategory("Visite Culturali e Musei", "VISIT"),
    ActivityCategory("Hotel e Soggiorni", "HOTEL"),
    ActivityCategory("Ristorazione e Degustazioni", "MEAL"),
    ActivityCategory("Trasporti e Mobilità", "TRANSPORT")
)

private fun formatDateRange(start: String?, end: String?): String {
    if (start.isNullOrBlank() || end.isNullOrBlank()) return ""
    return try {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val startDate = LocalDate.parse(start.take(10))
        val endDate = LocalDate.parse(end.take(10))
        if (startDate == endDate) {
            startDate.format(formatter)
        } else {
            "${startDate.format(formatter)} - ${endDate.format(formatter)}"
        }
    } catch (_: Exception) {
        ""
    }
}

private fun formatPrice(price: Double?): String =
    price?.let { "€%.2f".format(Locale.ITALY, it) } ?: "N/D"

private fun formatTripPrice(price: BigDecimal?): String =
    price?.setScale(2, RoundingMode.HALF_UP)?.let { "€$it" } ?: "N/D"

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val appError by AppStatus::error
    var searchQuery by remember { mutableStateOf("") }
    var activities by remember { mutableStateOf<List<ActivityHomeDTO>>(emptyList()) }
    var trips by remember { mutableStateOf<List<TripHomeDTO>>(emptyList()) }
    var activitiesByCategory by remember { mutableStateOf<Map<String, List<ActivityHomeDTO>>>(emptyMap()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    fun loadHome() {
        scope.launch {
            loading = true
            error = null
            hasSearched = false
            try {
                activities = HomeRepository.getTopActivities()
                trips = HomeRepository.getFeaturedTrips()
                activitiesByCategory = activityCategories.associate { category ->
                    category.backendCategory to HomeRepository.getActivitiesByCategory(category.backendCategory)
                }
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                error = if (msg.contains("failed to connect", ignoreCase = true) || msg.contains("refused", ignoreCase = true)) {
                    "Impossibile raggiungere il server."
                } else {
                    msg.ifBlank { "Impossibile caricare la Home." }
                }
            } finally {
                loading = false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun search() {
        scope.launch {
            loading = true
            error = null
            hasSearched = true
            try {
                val query = searchQuery.trim()
                activities = HomeRepository.searchActivities(query)
                trips = HomeRepository.searchTrips(query)
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                error = if (msg.contains("failed to connect", ignoreCase = true) || msg.contains("refused", ignoreCase = true)) {
                    "Impossibile raggiungere il server."
                } else {
                    msg.ifBlank { "Errore durante la ricerca." }
                }
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadHome()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            SearchTopBar(
                searchQuery = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearchSubmit = { search() }
            )

            Spacer(Modifier.height(16.dp))

            if (loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }

            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
            }

            if (appError == null) {
                if (hasSearched) {
                    SectionTitle("Risultati attività")
                    Spacer(Modifier.height(12.dp))

                    if (!loading && activities.isEmpty() && error == null) {
                        Text("Nessuna attività trovata.", color = Color.White)
                    }

                    ActivityRow(activities)

                    Spacer(Modifier.height(28.dp))

                    SectionTitle("Risultati viaggi")
                    Spacer(Modifier.height(12.dp))

                    if (!loading && trips.isEmpty() && error == null) {
                        Text("Nessun viaggio trovato.", color = Color.White)
                    }

                    TripRow(trips)
                } else {
                    SectionTitle("Le Migliori Attività")
                    Spacer(Modifier.height(12.dp))

                    if (!loading && activities.isEmpty() && error == null) {
                        Text("Nessuna attività disponibile.", color = Color.White)
                    }

                    ActivityRow(activities)

                    Spacer(Modifier.height(28.dp))

                    SectionTitle("I Migliori Viaggi in Primo Piano")
                    Spacer(Modifier.height(12.dp))

                    if (!loading && trips.isEmpty() && error == null) {
                        Text("Nessun viaggio disponibile.", color = Color.White)
                    }

                    TripRow(trips)

                    activityCategories.forEach { category ->
                        Spacer(Modifier.height(28.dp))

                        SectionTitle(category.title)
                        Spacer(Modifier.height(12.dp))

                        val categoryActivities = activitiesByCategory[category.backendCategory].orEmpty()

                        if (!loading && categoryActivities.isEmpty() && error == null) {
                            Text(
                                "Nessuna attività disponibile in questa categoria.",
                                color = Color.White
                            )
                        }

                        ActivityRow(categoryActivities)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ActivityRow(itemsList: List<ActivityHomeDTO>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = itemsList,
            key = { it.activityId ?: it.hashCode().toLong() }
        ) { item ->
            ExperienceCard(
                title = item.title ?: "Senza titolo",
                location = item.city.orEmpty(),
                category = item.category ?: "Attività",
                price = formatPrice(item.price),
                rating = item.averageRating?.let {
                    "%.1f".format(Locale.ITALY, it)
                } ?: "-",
                imageUrl = item.imageUrl.orEmpty(),
                dateInfo = formatDateRange(item.startDate, item.endDate),
                onClick = {
                    // TODO: navigazione ai dettagli dell'attività
                }
            )
        }
    }
}

@Composable
private fun TripRow(itemsList: List<TripHomeDTO>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = itemsList,
            key = { it.tripId ?: it.hashCode().toLong() }
        ) { item ->
            TripCard(
                title = item.title ?: "Senza titolo",
                location = item.location.orEmpty(),
                price = formatTripPrice(item.totalPrice),
                rating = item.averageRating?.toPlainString() ?: "-",
                dateRange = formatDateRange(item.startDate, item.endDate),
                imageUrl = item.coverPhotoUrl.orEmpty(),
                onClick = {
                    // TODO: navigazione ai dettagli del viaggio
                }
            )
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = Color.White,
        fontSize = 21.sp,
        fontWeight = FontWeight.ExtraBold
    )
}