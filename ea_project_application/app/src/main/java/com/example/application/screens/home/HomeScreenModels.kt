package com.example.application.screens.home

import com.example.application.dto.ActivityHomeDTO
import com.example.application.dto.TripHomeDTO
import com.example.application.network.HomeRepository

internal data class ActivityCategory(
    val title: String,
    val backendCategory: String
)

internal val activityCategories = listOf(
    ActivityCategory("Escursioni e Trekking", "EXCURSION"),
    ActivityCategory("Visite Culturali e Musei", "VISIT"),
    ActivityCategory("Hotel e Soggiorni", "HOTEL"),
    ActivityCategory("Ristorazione e Degustazioni", "MEAL"),
    ActivityCategory("Trasporti e Mobilità", "TRANSPORT")
)

internal data class PagedList<T>(
    val items: List<T> = emptyList(),
    val nextPage: Int = 0,
    val hasMore: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null
) {
    fun appended(newItems: List<T>, key: (T) -> Any): PagedList<T> {
        val merged = (items + newItems).distinctBy(key)
        return copy(
            items = merged,
            nextPage = nextPage + 1,
            hasMore = newItems.size >= HomeRepository.PAGE_SIZE && merged.size > items.size,
            loadingMore = false,
            error = null
        )
    }

    companion object {
        fun <T> first(items: List<T>) = PagedList(
            items = items,
            nextPage = 1,
            hasMore = items.size >= HomeRepository.PAGE_SIZE
        )
    }
}

internal fun ActivityHomeDTO.stableKey(): Long = activityId ?: hashCode().toLong()
internal fun TripHomeDTO.stableKey(): Long = tripId ?: hashCode().toLong()