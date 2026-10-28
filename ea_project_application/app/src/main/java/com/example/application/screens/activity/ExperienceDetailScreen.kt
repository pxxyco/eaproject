package com.example.application.screens.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.application.dto.ExperienceDetail
import com.example.application.dto.ExperienceRef
import com.example.application.network.ExperienceRepository
import com.example.application.screens.activity.itinerary.ItinerarySection
import com.example.application.screens.activity.photos.PhotoSection
import com.example.application.screens.activity.photos.PhotoViewer
import com.example.application.screens.activity.photos.PhotoViewerRequest
import com.example.application.screens.activity.reviews.rememberReviewsState
import com.example.application.screens.activity.reviews.reviewsSection
import com.example.application.screens.home.RemotePhoto
import com.example.application.ui.effects.LocalNavigationTail
import com.example.application.ui.effects.LocalNavigationOverlap
import com.example.application.ui.effects.navigationFade
import com.example.application.ui.theme.GlassFill
import com.example.application.ui.theme.Stone200
import com.example.application.ui.theme.Stone300
import kotlinx.coroutines.CancellationException

@Composable
fun ExperienceDetailScreen(
    experience: ExperienceRef,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    load: suspend (ExperienceRef) -> ExperienceDetail = { ExperienceRepository.getDetail(it) }
) {
    var detail by remember { mutableStateOf<ExperienceDetail?>(null)}
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(experience) {
        detail = null
        error = null
        try {
            detail = load(experience)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Impossibile caricare i dettagli"
        }
    }

    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BackButton(onBack)
            Text("Dettagli esperienza", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val current = detail
            when {
                error != null -> Text(error!!, color = Stone300, modifier = Modifier.align(Alignment.Center).padding(24.dp))
                current == null -> CircularProgressIndicator(color = Stone200, modifier = Modifier.align(Alignment.Center))
                else -> ExperienceDetailContent(current)
            }
        }
    }
}

private val HEADER_HEIGHT = 250.dp

@Composable
internal fun ExperienceDetailContent(detail: ExperienceDetail, onBookClick: () -> Unit = {}) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val overlap = LocalNavigationOverlap.current
    val bottomGap = 12.dp + if (overlap > 0.dp) overlap else WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
    var bookingHeight by remember { mutableStateOf(100.dp) }
    val scrollTail = LocalNavigationTail.current.coerceAtLeast(72.dp)
    var selectedTab by rememberSaveable { mutableStateOf(DetailTab.DESCRIPTION) }
    var openPhoto by remember { mutableStateOf<PhotoViewerRequest?>(null) }
    val reviewsState = rememberReviewsState(detail.ref)
    val headerHeightPx = with(LocalDensity.current) { HEADER_HEIGHT.toPx() }

    val headerOffset by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0)
                listState.firstVisibleItemScrollOffset * 0.5f
            else
                headerHeightPx
        }
    }

    LaunchedEffect(reviewsState, selectedTab) {
        if (selectedTab == DetailTab.REVIEWS) reviewsState.loadIfNeeded()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Only the scrolling page fades. The booking action stays outside the shader,
        // and the page overlaps its rounded top by a few pixels for a soft transition.
        Box(Modifier.fillMaxSize().padding(bottom = bookingHeight + bottomGap - 22.dp).navigationFade().background(GlassFill)) {
            RemotePhoto(
                imageUrl = detail.coverUrl,
                contentDescription = detail.title,
                modifier = Modifier
                    .testTag("experience_cover")
                    .fillMaxWidth()
                    .height(HEADER_HEIGHT)
                    .graphicsLayer { translationY = -headerOffset }
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().testTag("experience_detail_scroll"),
                contentPadding = PaddingValues(bottom = scrollTail)
            ) {
                item { Spacer(Modifier.height(HEADER_HEIGHT - 24.dp)) }
                item { ExperienceHeaderSheet(detail) }

                stickyHeader {
                    DetailTabs(
                        selected = selectedTab,
                        onSelect = { selectedTab  = it },
                        edgePadding = 16.dp
                    )
                }

                when (selectedTab) {
                    DetailTab.DESCRIPTION -> item { DescriptionSection(detail) }
                    DetailTab.ITINERARY   -> item { ItinerarySection(detail) }
                    DetailTab.GALLERY     -> item {
                        PhotoSection(
                            detail = detail,
                            onPhotoClick = { photos, index -> openPhoto = PhotoViewerRequest(photos, index) }
                        )
                    }
                    DetailTab.REVIEWS -> reviewsSection(reviewsState, detail.averageRating)
                }
            }

        }

        BookingBar(
            price = detail.price,
            availableSeats = detail.availableSeats,
            onBookClick = onBookClick,
            modifier = Modifier.align(Alignment.BottomCenter)
                .padding(horizontal = 18.dp).padding(bottom = bottomGap)
                .onSizeChanged { bookingHeight = with(density) { it.height.toDp() } }
        )

        openPhoto?.let { request ->
            PhotoViewer(request = request, onDismiss = { openPhoto = null })
        }
    }
}
