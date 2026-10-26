package com.example.application.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.application.dto.ActivityHomeDTO
import com.example.application.dto.ExperienceRef
import com.example.application.dto.ExperienceType
import com.example.application.dto.TripHomeDTO
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
        onClick = {  item.activityId?.let { onExperienceClick(ExperienceRef(ExperienceType.ACTIVITY, it)) }
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
    itemsList: List<ActivityHomeDTO>,
    onExperienceClick: (ExperienceRef) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items = itemsList, key = { it.activityId ?: it.hashCode().toLong() }) { item ->
            ActivityCardItem(item, onExperienceClick)
        }
    }
}

@Composable
internal fun TripRow(
    itemsList: List<TripHomeDTO>,
    onExperienceClick: (ExperienceRef) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(items = itemsList, key = { it.tripId ?: it.hashCode().toLong() }) { item ->
            TripCardItem(item,onExperienceClick)
        }
    }
}

@Composable
internal fun ActivityGrid(
    itemsList: List<ActivityHomeDTO>,
    onExperienceClick: (ExperienceRef) -> Unit
) {
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
                        onExperienceClick = onExperienceClick,
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
internal fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = Color.White,
        fontSize = 21.sp,
        fontWeight = FontWeight.ExtraBold
    )
}
