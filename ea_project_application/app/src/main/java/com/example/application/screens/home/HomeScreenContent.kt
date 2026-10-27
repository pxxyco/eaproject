package com.example.application.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.application.dto.ActivityHomeDTO
import com.example.application.dto.SearchSuggestion
import com.example.application.dto.TripHomeDTO
import kotlinx.coroutines.launch

@Composable
internal fun HomeScreenContent(
    activities: PagedList<ActivityHomeDTO>,
    trips: PagedList<TripHomeDTO>,
    activitiesByCategory: Map<String, PagedList<ActivityHomeDTO>>,
    homeError: String?,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    suggestions: List<SearchSuggestion>,
    onSuggestionClick: (SearchSuggestion) -> Unit,
    onDismissSuggestions: () -> Unit,
    onSearchSubmit: () -> Unit,
    hasSearched: Boolean,
    searchActivityResults: PagedList<ActivityHomeDTO>,
    searchTripResults: PagedList<TripHomeDTO>,
    searchError: String?,
    onResetSearch: () -> Unit,
    onLoadMoreTopActivities: () -> Unit,
    onLoadMoreTrips: () -> Unit,
    onLoadMoreCategory: (String) -> Unit,
    onLoadMoreSearchActivities: () -> Unit,
    onLoadMoreSearchTrips: () -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var submittedQuery by remember { mutableStateOf("") }

    fun resetSearchAndScroll() {
        onResetSearch()
        scope.launch {
            listState.animateScrollToItem(0)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        SearchTopBar(
            searchQuery = searchQuery,
            suggestions = suggestions,
            onQueryChange = onQueryChange,
            onSuggestionClick = { suggestion ->
                submittedQuery = suggestion.title
                onSuggestionClick(suggestion)
            },
            onDismissSuggestions = onDismissSuggestions,
            onSearchSubmit = {
                submittedQuery = searchQuery.trim()
                onSearchSubmit()
            },
            onClear = { resetSearchAndScroll() }
        )

        val shownError = if (hasSearched) searchError else homeError

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            item(key = "top_space") {
                Spacer(Modifier.height(16.dp))
            }

            if (shownError != null) {
                item(key = "error") {
                    Column {
                        Text(
                            text = shownError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            if (hasSearched) {

                item(key = "search_trips_title") {
                    Column {
                        SectionTitle("Risultati viaggi")
                        Spacer(Modifier.height(12.dp))
                    }
                }
                item(key = "search_trips_row") {
                    TripRow(searchTripResults, onLoadMoreSearchTrips)
                }

                item(key = "search_activities_title") {
                    Column {
                        Spacer(Modifier.height(28.dp))
                        SectionTitle("Risultati attività")
                        Spacer(Modifier.height(12.dp))
                    }
                }

                if (searchActivityResults.items.isEmpty() && searchError == null) {
                    item(key = "search_activities_empty") {
                        Text(
                            text = "Attività con titolo \"$submittedQuery\" non trovata",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    activityGridItems(searchActivityResults, onLoadMoreSearchActivities)
                }

            } else if (homeError == null) {

                item(key = "top_activities_title") {
                    Column {
                        SectionTitle("Le Migliori Attività")
                        Spacer(Modifier.height(12.dp))
                    }
                }
                item(key = "top_activities_row") {
                    ActivityRow(activities, onLoadMoreTopActivities)
                }

                item(key = "trips_title") {
                    Column {
                        Spacer(Modifier.height(28.dp))
                        SectionTitle("I Migliori Viaggi in Primo Piano")
                        Spacer(Modifier.height(12.dp))
                    }
                }
                item(key = "trips_row") {
                    TripRow(trips, onLoadMoreTrips)
                }

                activityCategories.forEach { category ->
                    val list = activitiesByCategory[category.backendCategory] ?: PagedList()

                    if (list.items.isNotEmpty()) {
                        item(key = "title_${category.backendCategory}") {
                            Column {
                                Spacer(Modifier.height(28.dp))
                                SectionTitle(category.title)
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                        item(key = "row_${category.backendCategory}") {
                            ActivityRow(list) { onLoadMoreCategory(category.backendCategory) }
                        }
                    }
                }
            }

            item(key = "bottom_space") {
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}