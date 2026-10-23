package com.example.application.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.application.dto.home.ActivityHomeDTO
import com.example.application.dto.SearchSuggestion
import com.example.application.dto.home.TripHomeDTO
import com.example.application.network.AppStatus
import com.example.application.network.HomeRepository
import com.example.application.screens.home.ExperienceCard
import com.example.application.screens.home.SearchTopBar
import com.example.application.screens.home.TripCard
import com.example.application.ui.theme.ScreenBackground
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import com.example.application.ui.theme.PopupSurface
import com.example.application.ui.theme.Stone200


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

private fun errorMessage(e: Exception, fallback: String): String {
    val msg = e.message.orEmpty()
    return if (msg.contains("failed to connect", ignoreCase = true) ||
        msg.contains("refused", ignoreCase = true)
    ) {
        "Impossibile raggiungere il server."
    } else {
        msg.ifBlank { fallback }
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val appError by AppStatus::error

    //Stato della home
    var activities by remember { mutableStateOf<List<ActivityHomeDTO>>(emptyList()) }
    var trips by remember { mutableStateOf<List<TripHomeDTO>>(emptyList()) }
    var activitiesByCategory by remember { mutableStateOf<Map<String, List<ActivityHomeDTO>>>(emptyMap()) }
    var homeError by remember { mutableStateOf<String?>(null) }

    //Stato della ricerca
    var searchQuery by remember { mutableStateOf("") }
    var hasSearched by remember { mutableStateOf(false) }
    var searchActivityResults by remember { mutableStateOf<List<ActivityHomeDTO>>(emptyList()) }
    var searchTripResults by remember { mutableStateOf<List<TripHomeDTO>>(emptyList()) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var searchRequestId by remember { mutableStateOf(0) }


    //Suggerimenti
    var suggestions by remember { mutableStateOf<List<SearchSuggestion>>(emptyList()) }
    var suggestionsEnabled by remember { mutableStateOf(true) }

    var loading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val showFloatingReset by remember { derivedStateOf { scrollState.value > 200 } }

    fun loadHome() {
        scope.launch {
            loading = true
            homeError = null
            try {
                activities = HomeRepository.getTopActivities()
                trips = HomeRepository.getFeaturedTrips()
                activitiesByCategory = activityCategories.associate { category ->
                    category.backendCategory to HomeRepository.getActivitiesByCategory(category.backendCategory)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                homeError = errorMessage(e, "Impossibile caricare la Home.")
            } finally {
                loading = false
            }
        }
    }

    //Torna allo stato iniziale(la home è già in memoria, connessione assente)
    fun resetSearch() {
        searchRequestId += 1
        suggestionsEnabled = false
        suggestions = emptyList()
        searchQuery = ""
        searchActivityResults = emptyList()
        searchTripResults = emptyList()
        searchError = null
        hasSearched = false
        loading = false
        scope.launch { scrollState.animateScrollTo(0) }
    }

    fun search() {
        suggestionsEnabled = false
        suggestions = emptyList()

        val query = searchQuery.trim()
        if (query.isBlank()) {
            resetSearch()
            return
        }

        searchRequestId += 1
        val requestId = searchRequestId

        scope.launch {
            loading = true
            searchError = null
            hasSearched = true
            try {
                val foundActivities = HomeRepository.searchActivities(query)
                val foundTrips = HomeRepository.searchTrips(query)
                if (requestId != searchRequestId) return@launch
                searchActivityResults = foundActivities
                searchTripResults = foundTrips
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (requestId == searchRequestId) {
                    searchActivityResults = emptyList()
                    searchTripResults = emptyList()
                    searchError = errorMessage(e, "Errore durante la ricerca.")
                }
            } finally {
                if (requestId == searchRequestId) loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadHome()
    }

    //Debounce dei suggerimenti
    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (!suggestionsEnabled || q.isBlank()) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(300)
        if (!suggestionsEnabled) return@LaunchedEffect
        suggestions = try {
            HomeRepository.getSuggestions(q)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
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
                suggestions = suggestions,
                onQueryChange = {
                    suggestionsEnabled = true
                    searchQuery = it
                },
                onSuggestionClick = {
                    suggestionsEnabled = false
                    suggestions = emptyList()
                    searchQuery = it.title
                },
                onDismissSuggestions = { suggestions = emptyList() },
                onSearchSubmit = { search() }
            )

            if (hasSearched) {
                Spacer(Modifier.height(8.dp))

                val inlineAlpha by animateFloatAsState(
                    targetValue = if (showFloatingReset) 0f else 1f,
                    label = "inlineResetAlpha"
                )
                OutlinedButton(
                    onClick = { resetSearch() },
                    enabled = !showFloatingReset,
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(inlineAlpha),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = PopupSurface,
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Stone200)
                ) {
                    Text("Azzera Ricerche")
                }
            }

            Spacer(Modifier.height(16.dp))

            if (loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }

            val shownError = if (hasSearched) searchError else homeError
            if (shownError != null) {
                Text(
                    text = shownError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
            }

            if (appError == null) {
                if (hasSearched) {
                    //Ricerca
                    SectionTitle("Risultati viaggi")
                    Spacer(Modifier.height(12.dp))

                    if (!loading && searchTripResults.isEmpty() && searchError == null) {
                        Text("Nessun viaggio trovato.", color = Color.White)
                    }
                    TripRow(searchTripResults)

                    Spacer(Modifier.height(28.dp))

                    // Attività lista verticale
                    SectionTitle("Risultati attività")
                    Spacer(Modifier.height(12.dp))

                    if (!loading && searchActivityResults.isEmpty() && searchError == null) {
                        Text("Nessuna attività trovata.", color = Color.White)
                    }
                    ActivityGrid(searchActivityResults)
                } else {
                    //home
                    SectionTitle("Le Migliori Attività")
                    Spacer(Modifier.height(12.dp))

                    if (!loading && activities.isEmpty() && homeError == null) {
                        Text("Nessuna attività disponibile.", color = Color.White)
                    }
                    ActivityRow(activities)

                    Spacer(Modifier.height(28.dp))

                    SectionTitle("I Migliori Viaggi in Primo Piano")
                    Spacer(Modifier.height(12.dp))

                    if (!loading && trips.isEmpty() && homeError == null) {
                        Text("Nessun viaggio disponibile.", color = Color.White)
                    }
                    TripRow(trips)

                    activityCategories.forEach { category ->
                        Spacer(Modifier.height(28.dp))

                        SectionTitle(category.title)
                        Spacer(Modifier.height(12.dp))

                        val categoryActivities = activitiesByCategory[category.backendCategory].orEmpty()

                        if (!loading && categoryActivities.isEmpty() && homeError == null) {
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
        AnimatedVisibility(
            visible = hasSearched && showFloatingReset,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            ExtendedFloatingActionButton(
                onClick = { resetSearch() },
                containerColor = Stone200,
                contentColor = Color.Black,
                icon = { Icon(Icons.Default.Close, contentDescription = null) },
                text = { Text("Azzera Ricerche") }
            )
        }
    }
}


@Composable
private fun ActivityCardItem(
    item: ActivityHomeDTO,
    modifier: Modifier = Modifier.width(180.dp)
) {
    ExperienceCard(
        title = item.title ?: "Senza titolo",
        location = item.city.orEmpty(),
        category = item.category ?: "Attività",
        price = formatPrice(item.price),
        rating = item.averageRating?.let { "%.1f".format(Locale.ITALY, it) } ?: "-",
        imageUrl = item.imageUrl.orEmpty(),
        dateInfo = formatDateRange(item.startDate, item.endDate),
        onClick = {
            // TODO navigazione attività
        },
        modifier = modifier
    )
}

@Composable
private fun TripCardItem(item: TripHomeDTO) {
    TripCard(
        title = item.title ?: "Senza titolo",
        location = item.location.orEmpty(),
        price = formatTripPrice(item.totalPrice),
        rating = item.averageRating?.toPlainString() ?: "-",
        dateRange = formatDateRange(item.startDate, item.endDate),
        imageUrl = item.coverPhotoUrl.orEmpty(),
        onClick = {
            // TODO navigazione viaggio
        }
    )
}

//Liste orizzontali (home)

@Composable
private fun ActivityRow(itemsList: List<ActivityHomeDTO>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(
            items = itemsList,
            key = { it.activityId ?: it.hashCode().toLong() }
        ) { item -> ActivityCardItem(item) }
    }
}

@Composable
private fun TripRow(itemsList: List<TripHomeDTO>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(
            items = itemsList,
            key = { it.tripId ?: it.hashCode().toLong() }
        ) { item -> TripCardItem(item) }
    }
}

//Liste verticali (ricerca)
@Composable
private fun ActivityGrid(itemsList: List<ActivityHomeDTO>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsList.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEach { item ->
                    ActivityCardItem(
                        item = item,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
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