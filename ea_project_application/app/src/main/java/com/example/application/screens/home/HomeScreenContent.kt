package com.example.application.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.application.dto.SearchSuggestion
import com.example.application.dto.ActivityHomeDTO
import com.example.application.dto.TripHomeDTO
import com.example.application.dto.ExperienceRef
import kotlinx.coroutines.launch

@Composable
internal fun HomeScreenContent(
    activities: List<ActivityHomeDTO>,
    trips: List<TripHomeDTO>,
    activitiesByCategory: Map<String, List<ActivityHomeDTO>>,
    homeError: String?,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    suggestions: List<SearchSuggestion>,
    onSuggestionClick: (SearchSuggestion) -> Unit,
    onDismissSuggestions: () -> Unit,
    onSearchSubmit: () -> Unit,
    hasSearched: Boolean,
    searchActivityResults: List<ActivityHomeDTO>,
    searchTripResults: List<TripHomeDTO>,
    searchError: String?,
    onResetSearch: () -> Unit,
    onExperienceClick: (ExperienceRef) -> Unit
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    var submittedQuery by remember { mutableStateOf("") }

    fun resetSearchAndScroll() {
        onResetSearch()
        scope.launch {
            scrollState.animateScrollTo(0)
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

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            val shownError = if (hasSearched) searchError else homeError

            if (shownError != null) {
                Text(
                    text = shownError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
            }

            if (hasSearched) {

                SectionTitle("Risultati viaggi")
                Spacer(Modifier.height(12.dp))
                TripRow(searchTripResults, onExperienceClick)

                Spacer(Modifier.height(28.dp))

                SectionTitle("Risultati attività")
                Spacer(Modifier.height(12.dp))

                if (searchActivityResults.isEmpty() && searchError == null) {
                    Text(
                        text = "Attività con titolo \"$submittedQuery\" non trovata",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    ActivityGrid(searchActivityResults, onExperienceClick)
                }

            } else if (homeError == null) {

                SectionTitle("Le Migliori Attività")
                Spacer(Modifier.height(12.dp))
                ActivityRow(activities, onExperienceClick)

                Spacer(Modifier.height(28.dp))

                SectionTitle("I Migliori Viaggi in Primo Piano")
                Spacer(Modifier.height(12.dp))
                TripRow(trips, onExperienceClick)

                activityCategories.forEach { category ->
                    val categoryActivities =
                        activitiesByCategory[category.backendCategory].orEmpty()

                    if (categoryActivities.isNotEmpty()) {
                        Spacer(Modifier.height(28.dp))
                        SectionTitle(category.title)
                        Spacer(Modifier.height(12.dp))
                        ActivityRow(categoryActivities, onExperienceClick)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}