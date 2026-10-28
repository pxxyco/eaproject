package com.example.application.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.application.dto.ActivityHomeDTO
import com.example.application.dto.ExperienceRef
import com.example.application.dto.ExperienceType
import com.example.application.dto.TripHomeDTO
import com.example.application.ui.theme.Stone200
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun formatDateRange(start: String?, end: String?): String {
    if (start.isNullOrBlank() || end.isNullOrBlank()) return ""
    return try {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val startDate = LocalDate.parse(start.take(10))
        val endDate = LocalDate.parse(end.take(10))
        if (startDate == endDate) startDate.format(formatter)
        else "${startDate.format(formatter)} - ${endDate.format(formatter)}"
    } catch (_: Exception) {
        ""
    }
}

private fun formatPrice(price: Double?): String =
    price?.let { "€%.2f".format(Locale.ITALY, it) } ?: "N/D"

private fun formatTripPrice(price: BigDecimal?): String =
    price?.setScale(2, RoundingMode.HALF_UP)?.let { "€$it" } ?: "N/D"

@Composable
private fun ActivityCardItem(
    item: ActivityHomeDTO,
    onExperienceClick: (ExperienceRef) -> Unit,
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
            item.activityId?.let { onExperienceClick(ExperienceRef(ExperienceType.ACTIVITY, it)) }
        },
        modifier = modifier
    )
}

@Composable
private fun TripCardItem(
    item: TripHomeDTO,
    onExperienceClick: (ExperienceRef) -> Unit
) {
    TripCard(
        title = item.title ?: "Senza titolo",
        location = item.location.orEmpty(),
        price = formatTripPrice(item.totalPrice),
        rating = item.averageRating?.toPlainString() ?: "-",
        dateRange = formatDateRange(item.startDate, item.endDate),
        imageUrl = item.coverPhotoUrl.orEmpty(),
        onClick = {
            item.tripId?.let { onExperienceClick(ExperienceRef(ExperienceType.TRIP, it)) }
        }
    )
}

@Composable
internal fun ActivityRow(
    list: PagedList<ActivityHomeDTO>,
    onExperienceClick: (ExperienceRef) -> Unit,
    onLoadMore: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(items = list.items, key = { it.stableKey() }) { item ->
            ActivityCardItem(item, onExperienceClick)
        }
        if (list.hasMore) {
            item(key = "load_more") {
                LoadMoreButton(
                    loading = list.loadingMore,
                    hasError = list.error != null,
                    onClick = onLoadMore,
                    modifier = Modifier.width(130.dp)
                )
            }
        }
    }
}

@Composable
internal fun TripRow(
    list: PagedList<TripHomeDTO>,
    onExperienceClick: (ExperienceRef) -> Unit,
    onLoadMore: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(items = list.items, key = { it.stableKey() }) { item ->
            TripCardItem(item, onExperienceClick)
        }
        if (list.hasMore) {
            item(key = "load_more") {
                LoadMoreButton(
                    loading = list.loadingMore,
                    hasError = list.error != null,
                    onClick = onLoadMore,
                    modifier = Modifier.width(130.dp)
                )
            }
        }
    }
}



internal fun LazyListScope.activityGridItems(
    list: PagedList<ActivityHomeDTO>,
    onExperienceClick: (ExperienceRef) -> Unit,
    onLoadMore: () -> Unit
) {
    items(
        items = list.items.chunked(2),
        key = { pair -> pair.first().stableKey() }
    ) { pair ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            pair.forEach { item ->
                ActivityCardItem(item, onExperienceClick, Modifier.weight(1f).fillMaxHeight())
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
    if (list.hasMore) {
        item(key = "grid_load_more") {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                LoadMoreButton(list.loadingMore, list.error != null, onLoadMore)
            }
        }
    }
}


@Composable
internal fun LoadMoreButton(
    loading: Boolean,
    hasError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = !loading,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Stone200
            )
        } else {
            Text(
                text = if (hasError) "Riprova" else "Carica altro",
                color = Stone200,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 21.sp,
        fontWeight = FontWeight.ExtraBold
    )
}
