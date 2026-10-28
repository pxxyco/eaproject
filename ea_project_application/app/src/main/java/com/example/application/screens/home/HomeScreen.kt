package com.example.application.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.application.dto.SearchSuggestion
import com.example.application.dto.ActivityHomeDTO
import com.example.application.dto.TripHomeDTO
import com.example.application.dto.ExperienceRef
import com.example.application.network.AppStatus
import com.example.application.network.HomeRepository
import com.example.application.screens.common.LoadingOverlay
import com.example.application.screens.common.publishScreenLoading
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onExperienceClick: (ExperienceRef) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val appError by AppStatus::error

    var activities by remember { mutableStateOf(PagedList<ActivityHomeDTO>()) }
    var trips by remember { mutableStateOf(PagedList<TripHomeDTO>()) }
    var activitiesByCategory by remember {
        mutableStateOf<Map<String, PagedList<ActivityHomeDTO>>>(emptyMap())
    }
    var homeError by remember { mutableStateOf<String?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var searchedQuery by remember { mutableStateOf("") }
    var hasSearched by remember { mutableStateOf(false) }
    var searchActivityResults by remember { mutableStateOf(PagedList<ActivityHomeDTO>()) }
    var searchTripResults by remember { mutableStateOf(PagedList<TripHomeDTO>()) }
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
                    category to PagedList.first(deferred.await())
                }

                activities = PagedList.first(loadedActivities)
                trips = PagedList.first(loadedTrips)
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

    fun <T> loadMore(
        get: () -> PagedList<T>,
        set: (PagedList<T>) -> Unit,
        key: (T) -> Any,
        isStale: () -> Boolean = { false },
        fetch: suspend (Int) -> List<T>
    ) {
        val current = get()
        if (current.loadingMore || !current.hasMore) return
        set(current.copy(loadingMore = true, error = null))

        scope.launch {
            try {
                val page = fetch(current.nextPage)
                if (isStale()) return@launch
                set(get().appended(page, key))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!isStale()) {
                    set(
                        get().copy(
                            loadingMore = false,
                            error = errorMessage(e, "Impossibile caricare altri risultati.")
                        )
                    )
                }
            }
        }
    }

    fun resetSearch() {
        searchRequestId++
        suggestionsEnabled = false
        suggestions = emptyList()
        searchQuery = ""
        searchedQuery = ""
        searchActivityResults = PagedList()
        searchTripResults = PagedList()
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
        searchedQuery = query
        hasSearched = true
        searchLoading = true
        searchError = null
        searchActivityResults = PagedList()
        searchTripResults = PagedList()

        scope.launch {
            try {
                val (foundActivities, foundTrips) = coroutineScope {
                    val activitiesDeferred = async { HomeRepository.searchActivities(query) }
                    val tripsDeferred = async { HomeRepository.searchTrips(query) }
                    activitiesDeferred.await() to tripsDeferred.await()
                }

                if (requestId != searchRequestId) return@launch
                searchActivityResults = PagedList.first(foundActivities)
                searchTripResults = PagedList.first(foundTrips)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (requestId == searchRequestId) {
                    searchActivityResults = PagedList()
                    searchTripResults = PagedList()
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

    val loadingMessage = when {
        appError != null -> "Connessione persa. Riconnessione in corso..."
        homeLoading -> "Caricamento in corso..."
        searchLoading -> "Ricerca in corso..."
        else -> null
    }
    val hostedLoading = publishScreenLoading(loadingMessage)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Transparent)
    ) {
        if (homeLoading || appError != null) {
            if (!hostedLoading) LoadingOverlay(message = loadingMessage.orEmpty())
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
                onResetSearch = { resetSearch() },
                onExperienceClick = onExperienceClick,

                onLoadMoreTopActivities = {
                    loadMore(
                        get = { activities }, set = { activities = it },
                        key = { it.stableKey() }
                    ) { page -> HomeRepository.getTopActivities(page) }
                },
                onLoadMoreTrips = {
                    loadMore(
                        get = { trips }, set = { trips = it },
                        key = { it.stableKey() }
                    ) { page -> HomeRepository.getFeaturedTrips(page) }
                },
                onLoadMoreCategory = { category ->
                    loadMore(
                        get = { activitiesByCategory[category] ?: PagedList() },
                        set = { activitiesByCategory = activitiesByCategory + (category to it) },
                        key = { it.stableKey() }
                    ) { page -> HomeRepository.getActivitiesByCategory(category, page) }
                },
                onLoadMoreSearchActivities = {
                    val id = searchRequestId
                    loadMore(
                        get = { searchActivityResults }, set = { searchActivityResults = it },
                        key = { it.stableKey() },
                        isStale = { id != searchRequestId }
                    ) { page -> HomeRepository.searchActivities(searchedQuery, page) }
                },
                onLoadMoreSearchTrips = {
                    val id = searchRequestId
                    loadMore(
                        get = { searchTripResults }, set = { searchTripResults = it },
                        key = { it.stableKey() },
                        isStale = { id != searchRequestId }
                    ) { page -> HomeRepository.searchTrips(searchedQuery, page) }
                }
            )

            if (searchLoading && !hostedLoading) {
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
