package com.example.application.screens.activity

import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.application.network.BookingRepository
import com.example.application.network.FavoriteRepository
import com.example.application.network.UserSession
import com.example.application.screens.activity.booking.BookingSheet
import com.example.application.screens.activity.lists.AddToListSheet
import com.example.application.screens.activity.reviews.ReviewDialogs
import kotlinx.coroutines.launch
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
import androidx.compose.runtime.key
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
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
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
    load: suspend (ExperienceRef) -> ExperienceDetail = { ExperienceRepository.getDetail(it) },
    onLoginRequired: () -> Unit = {}

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
                else -> key(current.ref) { ExperienceDetailContent(current,
                    onRefresh = { detail = load(experience) }, onLoginRequired = onLoginRequired) }
            }
        }
    }
}

private val HEADER_HEIGHT = 250.dp

@Composable
internal fun ExperienceDetailContent(detail: ExperienceDetail, onBookClick: (() -> Unit)? = null,
    onRefresh: suspend () -> Unit = {}, onLoginRequired: () -> Unit = {}) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val overlap = LocalNavigationOverlap.current
    val bottomGap = 12.dp + if (overlap > 0.dp) overlap else WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
    var bookingHeight by remember { mutableStateOf(100.dp) }
    val scrollTail = LocalNavigationTail.current.coerceAtLeast(72.dp)
    var selectedTab by rememberSaveable { mutableStateOf(DetailTab.DESCRIPTION) }
    var openPhoto by remember { mutableStateOf<PhotoViewerRequest?>(null) }
    val user = UserSession.user
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bookingSeats by remember { mutableStateOf<Int?>(null) }
    var favorite by remember(detail.ref, user?.id) { mutableStateOf(false) }
    var favoriteBusy by remember { mutableStateOf(false) }
    var favoriteLoaded by remember(detail.ref, user?.id) { mutableStateOf(false) }
    var showAddToList by remember { mutableStateOf(false) }
    val ownerId = when (detail) {
        is ExperienceDetail.Activity -> detail.dto.createdByUserId
        is ExperienceDetail.Trip -> detail.dto.createdByUserId
    }
    val reviewsState = rememberReviewsState(detail.ref, ownerId)
    val traveler = user?.role.equals("TRAVELER", true)
    val bookable = detail.isBookable()
    val reserved = reviewsState.bookings.any { it.bookingStatus != "CANCELLED" }
    val actionLabel = when {
        onBookClick != null -> null
        user == null -> "Accedi"
        !traveler -> "Solo viaggiatori"
        reviewsState.participationBusy -> "Verifica…"
        !reviewsState.participationLoaded -> "Riprova"
        reserved -> "Già prenotata"
        !bookable -> "Non prenotabile"
        else -> null
    }
    LaunchedEffect(detail.ref, user?.id) {
        reviewsState.refreshParticipation()
        if (user != null) {
            try { favorite = FavoriteRepository.isFavorite(detail.ref) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { Toast.makeText(context, "Impossibile caricare i preferiti", Toast.LENGTH_SHORT).show() }
        }
        favoriteLoaded = true
    }
    fun toggleFavorite() {
        if (user == null) { onLoginRequired(); return }
        if (favoriteBusy) return
        favoriteBusy = true
        scope.launch {
            try { favorite = FavoriteRepository.setFavorite(detail.ref, !favorite) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { Toast.makeText(context, e.message ?: "Impossibile aggiornare i preferiti", Toast.LENGTH_SHORT).show() }
            finally { favoriteBusy = false }
        }
    }
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
                    .padding(horizontal = 16.dp)
                    .graphicsLayer { translationY = -headerOffset }
                    .clip(RoundedCornerShape(24.dp))
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().testTag("experience_detail_scroll"),
                contentPadding = PaddingValues(bottom = scrollTail)
            ) {
                item { Spacer(Modifier.height(HEADER_HEIGHT)) }
                item { ExperienceHeaderSheet(detail, rating = if (reviewsState.reviews != null) reviewsState.average else detail.averageRating) }
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FavoriteButton(favorite, !favoriteBusy && favoriteLoaded, ::toggleFavorite)
                        AddToListButton(onClick = { if (user == null) onLoginRequired() else showAddToList = true })
                    }
                }

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
                    DetailTab.REVIEWS -> reviewsSection(reviewsState)
                }
            }

        }

        BookingBar(
            price = detail.price,
            availableSeats = detail.availableSeats,
            onBookClick = {
                if (onBookClick != null) onBookClick()
                else if (user == null) onLoginRequired()
                else if (!reviewsState.participationLoaded) scope.launch { reviewsState.refreshParticipation() }
                else bookingSeats = detail.availableSeats?.takeIf { it > 0 }
            },
            enabled = onBookClick != null || user == null || (traveler && !reviewsState.participationBusy && !reserved && bookable),
            actionLabel = actionLabel,
            modifier = Modifier.align(Alignment.BottomCenter)
                .padding(horizontal = 18.dp).padding(bottom = bottomGap)
                .onSizeChanged { bookingHeight = with(density) { it.height.toDp() } }
        )

        if (showAddToList) AddToListSheet(detail.ref, onDismiss = { showAddToList = false }, onAdded = { name ->
            showAddToList = false
            Toast.makeText(context, "Aggiunto a $name", Toast.LENGTH_SHORT).show()
        })
        bookingSeats?.let { seats ->
            BookingSheet(detail.title, detail.price, seats, onDismiss = { bookingSeats = null }, onConfirm = { selected ->
                val booking = BookingRepository.book(detail.ref, selected)
                reviewsState.refreshParticipation()
                try { onRefresh() } catch (e: CancellationException) { throw e }
                catch (e: Exception) { Toast.makeText(context, "Prenotazione salvata. Riapri la scheda per aggiornare i posti.", Toast.LENGTH_LONG).show() }
                booking
            })
        }
        ReviewDialogs(reviewsState)
        openPhoto?.let { request ->
            PhotoViewer(request = request, onDismiss = { openPhoto = null })
        }
    }
}
