package com.example.application.screens.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.application.dto.ExperienceDetail
import com.example.application.dto.ExperienceRef
import com.example.application.network.ExperienceRepository
import com.example.application.screens.home.RemotePhoto
import com.example.application.ui.theme.ScreenBackground
import com.example.application.ui.theme.Stone200
import com.example.application.ui.theme.Stone300
import kotlinx.coroutines.CancellationException

@Composable
fun ExperienceDetailScreen(
    experience: ExperienceRef,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var detail by remember { mutableStateOf<ExperienceDetail?>(null)}
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(experience) {
        detail = null
        error = null
        try {
            detail = ExperienceRepository.getDetail(experience)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Impossibile caricare i dettagli"
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        val current = detail
        when {
            error != null -> Text(
                text = error!!,
                color = Stone300,
                modifier = Modifier 
                    .align(Alignment.Center)
                    .padding(24.dp)
            )
            current == null -> CircularProgressIndicator(
                color = Stone200,
                modifier = Modifier.align(Alignment.Center)
            )

            else -> ExperienceDetailContent(current)
        }
        BackButton(
            onBack = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .size(40.dp)
        )
    }
}

private val HEADER_HEIGHT = 250.dp

@Composable
private fun ExperienceDetailContent(detail: ExperienceDetail) {
    val listState = rememberLazyListState()
    var selectedTab by rememberSaveable { mutableStateOf(DetailTab.DESCRIPTION) }
    val headerHeightPx = with(LocalDensity.current) { HEADER_HEIGHT.toPx() }

    val headerOffset by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0)
                listState.firstVisibleItemScrollOffset * 0.5f
            else
                headerHeightPx
        }
    }

    val tabsPinned by remember {
        derivedStateOf { listState.firstVisibleItemIndex >= 2 }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RemotePhoto(
            imageUrl = detail.coverUrl,
            contentDescription = detail.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(HEADER_HEIGHT)
                .graphicsLayer { translationY = -headerOffset }
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item { Spacer(Modifier.height(HEADER_HEIGHT - 24.dp)) }
            item { ExperienceHeaderSheet(detail) }

            stickyHeader {
                DetailTabs(
                    selected = selectedTab,
                    onSelect = { selectedTab  = it },
                    edgePadding = if (tabsPinned) 56.dp else 16.dp
                )
            }

            item {
                when (selectedTab) {
                    DetailTab.DESCRIPTION -> SectionPlaceholder("Descrizione")
                    DetailTab.ITINERARY   -> SectionPlaceholder("Itinerario")
                    DetailTab.GALLERY     -> SectionPlaceholder("Galleria")
                    DetailTab.REVIEWS     -> SectionPlaceholder("Recensioni")
                }
            }
        }

        BookingBar(
            price = detail.price,
            availableSeats = detail.availableSeats,
            onBookClick = { /* TODO prenotazione */ },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}