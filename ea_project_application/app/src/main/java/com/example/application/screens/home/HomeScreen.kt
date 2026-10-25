package com.example.application.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.application.dto.SearchSuggestion
import com.example.application.dto.home.ActivityHomeDTO
import com.example.application.dto.home.TripHomeDTO
import com.example.application.network.AppStatus
import com.example.application.network.HomeRepository
import com.example.application.screens.common.LoadingOverlay
import com.example.application.ui.theme.ScreenBackground
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val appError by AppStatus::error

    var activities by remember { mutableStateOf<List<ActivityHomeDTO>>(emptyList()) }
    var trips by remember { mutableStateOf<List<TripHomeDTO>>(emptyList()) }
    var activitiesByCategory by remember {
        mutableStateOf<Map<String, List<ActivityHomeDTO>>>(emptyMap())
    }
    var homeError by remember { mutableStateOf<String?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var hasSearched by remember { mutableStateOf(false) }
    var searchActivityResults by remember { mutableStateOf<List<ActivityHomeDTO>>(emptyList()) }
    var searchTripResults by remember { mutableStateOf<List<TripHomeDTO>>(emptyList()) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var searchRequestId by remember { mutableStateOf(0) }
    var searchLoading by remember { mutableStateOf(false) }

    var suggestions by remember { mutableStateOf<List<SearchSuggestion>>(emptyList()) }
    var suggestionsEnabled by remember { mutableStateOf(true) }

    var homeLoading by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(appError) {
        if (appError != null) {
            homeLoading = false
            return@LaunchedEffect
        }

        homeLoading = true
        homeError = null
        try {
            coroutineScope {
                val activitiesDeferred = async { HomeRepository.getTopActivities() }
                val tripsDeferred = async { HomeRepository.getFeaturedTrips() }
                val categoriesDeferred = activityCategories.map { category ->
                    category.backendCategory to async {
                        HomeRepository.getActivitiesByCategory(category.backendCategory)
                    }
                }

                val loadedActivities = activitiesDeferred.await()
                val loadedTrips = tripsDeferred.await()
                val loadedCategories = categoriesDeferred.associate { (category, deferred) ->
                    category to deferred.await()
                }

                activities = loadedActivities
                trips = loadedTrips
                activitiesByCategory = loadedCategories
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            homeError = errorMessage(e, "Impossibile caricare la Home.")
        } finally {
            homeLoading = false
        }
    }

    fun resetSearch() {
        searchRequestId++
        suggestionsEnabled = false
        suggestions = emptyList()
        searchQuery = ""
        searchActivityResults = emptyList()
        searchTripResults = emptyList()
        searchError = null
        hasSearched = false
        searchLoading = false
    }

    fun search() {
        suggestionsEnabled = false
        suggestions = emptyList()

        val query = searchQuery.trim()
        if (query.isBlank()) {
            resetSearch()
            return
        }

        searchRequestId++
        val requestId = searchRequestId
        hasSearched = true
        searchLoading = true
        searchError = null

        scope.launch {
            try {
                val (foundActivities, foundTrips) = coroutineScope {
                    val activitiesDeferred = async { HomeRepository.searchActivities(query) }
                    val tripsDeferred = async { HomeRepository.searchTrips(query) }
                    activitiesDeferred.await() to tripsDeferred.await()
                }

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
                if (requestId == searchRequestId) searchLoading = false
            }
        }
    }

    LaunchedEffect(searchQuery, suggestionsEnabled) {
        val query = searchQuery.trim()
        if (!suggestionsEnabled || query.isBlank() || hasSearched) {
            suggestions = emptyList()
            return@LaunchedEffect
        }

        delay(300)
        suggestions = try {
            HomeRepository.getSuggestions(query)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        if (homeLoading || appError != null) {
            LoadingOverlay(
                message = if (appError != null) {
                    "Connessione persa. Riconnessione in corso..."
                } else {
                    "Caricamento in corso..."
                }
            )
        } else {
            HomeScreenContent(
                activities = activities,
                trips = trips,
                activitiesByCategory = activitiesByCategory,
                homeError = homeError,
                searchQuery = searchQuery,
                onQueryChange = {
                    suggestionsEnabled = true
                    searchQuery = it
                },
                suggestions = suggestions,
                onSuggestionClick = {
                    suggestionsEnabled = false
                    suggestions = emptyList()
                    searchQuery = it.title
                },
                onDismissSuggestions = { suggestions = emptyList() },
                onSearchSubmit = { search() },
                hasSearched = hasSearched,
                searchActivityResults = searchActivityResults,
                searchTripResults = searchTripResults,
                searchError = searchError,
                onResetSearch = { resetSearch() }
            )


            if (searchLoading) {
                LoadingOverlay(message = "Ricerca in corso...")
            }
        }
    }
}

private fun errorMessage(e: Exception, fallback: String): String {
    val message = e.message.orEmpty()
    return if (
        message.contains("failed to connect", ignoreCase = true) ||
        message.contains("refused", ignoreCase = true)
    ) {
        "Impossibile raggiungere il server."
    } else {
        message.ifBlank { fallback }
    }
}
