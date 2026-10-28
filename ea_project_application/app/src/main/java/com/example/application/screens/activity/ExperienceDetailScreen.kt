package com.example.application.screens.activity

import android.widget.Toast
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.application.dto.ExperienceDetail
import com.example.application.dto.ExperienceRef
import com.example.application.network.BookingRepository
import com.example.application.network.ExperienceRepository
import com.example.application.network.FavoriteRepository
import com.example.application.network.UserSession
import com.example.application.screens.activity.booking.BookingSheet
import com.example.application.screens.activity.lists.AddToListSheet
import com.example.application.screens.activity.itinerary.ItinerarySection
import com.example.application.screens.activity.photos.PhotoSection
import com.example.application.screens.activity.photos.PhotoViewer
import com.example.application.screens.activity.photos.PhotoViewerRequest
import com.example.application.screens.activity.reviews.ReviewDialogs
import com.example.application.screens.activity.reviews.rememberReviewsState
import com.example.application.screens.activity.reviews.reviewsSection
import com.example.application.screens.home.RemotePhoto
import com.example.application.ui.theme.ScreenBackground
import com.example.application.ui.theme.Stone200
import com.example.application.ui.theme.Stone300
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

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

            else -> ExperienceDetailContent(
                detail = current,
                onRefresh = { detail = ExperienceRepository.getDetail(experience) }
            )
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
private fun ExperienceDetailContent(
    detail: ExperienceDetail,
    onRefresh: suspend () -> Unit
) {
    val listState = rememberLazyListState()
    var selectedTab by rememberSaveable { mutableStateOf(DetailTab.DESCRIPTION) }
    var openPhoto by remember { mutableStateOf<PhotoViewerRequest?>(null) }
    var bookingSeats by remember { mutableStateOf<Int?>(null) }
    var favorite by remember(detail.ref) { mutableStateOf(false) }
    var favoriteBusy by remember { mutableStateOf(false) }
    var showAddToList by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val userId = UserSession.user?.id
    val ownerId = when (detail) {
        is ExperienceDetail.Activity -> detail.dto.createdByUserId
        is ExperienceDetail.Trip -> detail.dto.createdByUserId
    }
    val reviewsState = rememberReviewsState(detail.ref, ownerId)
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

    LaunchedEffect(reviewsState, selectedTab) {
        if (selectedTab == DetailTab.REVIEWS) reviewsState.loadIfNeeded()
    }

    LaunchedEffect(detail.ref, userId) {
        favorite = false
        if (userId != null) {
            favorite = runCatching { FavoriteRepository.isFavorite(detail.ref) }.getOrDefault(false)
        }
    }

    fun toggleFavorite() {
        val target = !favorite
        favorite = target
        favoriteBusy = true
        scope.launch {
            try {
                favorite = FavoriteRepository.setFavorite(detail.ref, target)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                favorite = !target
                Toast.makeText(context, e.message ?: "Impossibile aggiornare i preferiti", Toast.LENGTH_SHORT).show()
            } finally {
                favoriteBusy = false
            }
        }
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
            item {
                ExperienceHeaderSheet(
                    detail = detail,
                    rating = if (reviewsState.reviews != null) reviewsState.average else detail.averageRating
                )
            }

            stickyHeader {
                DetailTabs(
                    selected = selectedTab,
                    onSelect = { selectedTab  = it },
                    edgePadding = if (tabsPinned) 56.dp else 16.dp
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
                DetailTab.REVIEWS -> reviewsSection(reviewsState)
            }
        }

        BookingBar(
            price = detail.price,
            availableSeats = detail.availableSeats,
            onBookClick = {
                bookingSeats = detail.availableSeats?.takeIf { it > 0 }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        FavoriteButton(
            favorite = favorite,
            enabled = !favoriteBusy,
            onToggle = ::toggleFavorite,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(40.dp)
        )

        AddToListButton(
            onClick = { showAddToList = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 56.dp)
                .size(40.dp)
        )

        if (showAddToList) {
            AddToListSheet(
                ref = detail.ref,
                onDismiss = { showAddToList = false },
                onAdded = { listName ->
                    showAddToList = false
                    Toast.makeText(context, "Aggiunto a \"$listName\"", Toast.LENGTH_SHORT).show()
                }
            )
        }

        openPhoto?.let { request ->
            PhotoViewer(request = request, onDismiss = { openPhoto = null })
        }

        bookingSeats?.let { seatsLeft ->
            BookingSheet(
                title = detail.title,
                price = detail.price,
                availableSeats = seatsLeft,
                onDismiss = { bookingSeats = null },
                onConfirm = { seats ->
                    BookingRepository.book(detail.ref, seats)
                    runCatching { onRefresh() }
                }
            )
        }

        ReviewDialogs(reviewsState)
    }
}