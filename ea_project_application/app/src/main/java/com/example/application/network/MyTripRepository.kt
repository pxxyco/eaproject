package com.example.application.network

import androidx.compose.runtime.*
import kotlinx.coroutines.CancellationException
import com.example.application.dto.*
import com.example.application.dto.ActivityHomeDTO
import com.example.application.dto.TripHomeDTO
import java.io.IOException

private data class CreateListRequest(val name: String, val visibility: String)

data class BookingSummary(val id: Long, val upcoming: Boolean)
data class FavoriteSummary(val id: Long, val type: String, val itemId: Long)

object MyTripRepository {

    private val _bookings = mutableStateListOf<BookingSummary>()
    private val _favorites = mutableStateListOf<FavoriteSummary>()
    private val _lists = mutableStateListOf<FavoriteList>()

    var bookingsLoaded by mutableStateOf(false)
        private set
    var favoritesLoaded by mutableStateOf(false)
        private set
    var hubErrors by mutableStateOf<List<String>>(emptyList())
        private set
    val lists: List<FavoriteList> get() = _lists
    val upcomingBookings: Int get() = _bookings.count { it.upcoming }
    val favoritesCount: Int get() = _favorites.size

    private fun FavoriteListItemDTO.toUi(): FavoriteList? {
        val listId = id ?: return null
        val vis = runCatching { ListVisibility.valueOf(visibility.orEmpty()) }
            .getOrDefault(ListVisibility.PRIVATE)
        return FavoriteList(listId, name.orEmpty().ifBlank { "Senza nome" }, vis)
    }

    suspend fun refreshLists() {
        _lists.clear()
        _bookings.clear()
        _favorites.clear()
        bookingsLoaded = false
        favoritesLoaded = false
        hubErrors = emptyList()
        suspend fun section(label: String, load: suspend () -> Unit) {
            try { load() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { hubErrors = hubErrors + "$label: ${e.message ?: "dati non disponibili"}" }
        }
        section("Liste") {
            val remote = fetch("/api/favorite-lists/mine", Array<FavoriteListItemDTO>::class.java)
            _lists.addAll(remote.mapNotNull { it.toUi() })
        }
        if (UserSession.role.equals("TRAVELER", true)) section("Prenotazioni") {
            val bookings = fetch("/api/bookings/mine", Array<BookingItemDTO>::class.java)
            _bookings.addAll(bookings.mapNotNull { b -> b.bookingId?.let { BookingSummary(it, b.bookingStatus in listOf("PENDING", "CONFIRMED")) } })
            bookingsLoaded = true
        }
        section("Preferiti") {
            val favorites = fetch("/api/favorites", Array<FavoriteItemDTO>::class.java)
            _favorites.addAll(favorites.mapNotNull { f -> f.id?.let { FavoriteSummary(it, f.type.orEmpty(), f.itemId ?: 0) } })
            favoritesLoaded = true
        }
    }

    suspend fun createList(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        val request = CreateListRequest(clean, ListVisibility.PRIVATE.name)
        when (val result = ApiService.post("/api/favorite-lists", request, FavoriteListItemDTO::class.java)) {
            is ApiResult.Success -> result.data.toUi()?.let { _lists.add(it) }
            is ApiResult.HttpError -> throw IOException(myTripHttpError(result.code))
            is ApiResult.RateLimited -> throw IOException("Troppe richieste, riprova tra poco.")
            is ApiResult.NetworkError -> throw IOException("Impossibile raggiungere il server.")
        }
    }

    sealed class HubTarget(val title: String, val path: String) {
        data object Bookings : HubTarget("Le mie prenotazioni", "/api/bookings/mine")
        data object Favorites : HubTarget("Le mie preferite", "/api/favorites")
        data class Custom(val list: FavoriteList) :
            HubTarget(list.name, "/api/favorite-lists/${list.id}")
    }

    suspend fun loadItems(target: HubTarget): CollectionItemsDTO = when (target) {
        HubTarget.Bookings -> {
            val bookings = fetch(target.path, Array<BookingItemDTO>::class.java)
                .filter { !it.bookingStatus.equals("CANCELLED", true) }
            CollectionItemsDTO(
                bookings = bookings,
                trips = bookings
                    .filter { it.bookingType.equals("TRIP", ignoreCase = true) }
                    .map { bookingToTrip(it) },
                activities = bookings
                    .filter { it.bookingType.equals("ACTIVITY", ignoreCase = true) }
                    .map { bookingToActivity(it) }
            )
        }

        HubTarget.Favorites -> {
            val favorites = fetch(target.path, Array<FavoriteItemDTO>::class.java)
            CollectionItemsDTO(
                trips = favorites
                    .filter { it.type.equals("TRIP", ignoreCase = true) }
                    .map { favoriteToTrip(it) },
                activities = favorites
                    .filter { !it.type.equals("TRIP", ignoreCase = true) }
                    .map { favoriteToActivity(it) }
            )
        }

        is HubTarget.Custom -> CollectionItemsDTO(
            trips = fetch("/api/list-trips/list/${target.list.id}", Array<TripHomeDTO>::class.java),
            activities = fetch("/api/list-activities/list/${target.list.id}", Array<ActivityHomeDTO>::class.java)
        )
    }

    suspend fun deleteList(id: Long) {
        delete("/api/favorite-lists/$id")
        _lists.removeAll { it.id == id }
    }

    suspend fun cancelBooking(id: Long) {
        delete("/api/bookings/$id")
        _bookings.removeAll { it.id == id }
    }

    suspend fun removeItem(target: HubTarget, id: Long, trip: Boolean) {
        val kind = if (trip) "trips" else "activities"
        when (target) {
            HubTarget.Favorites -> delete("/api/favorites/$kind/$id")
            is HubTarget.Custom ->
                delete("/api/list-$kind/list/${target.list.id}/${if (trip) "trip" else "activity"}/$id")
            HubTarget.Bookings -> cancelBooking(id)
        }
    }

    private suspend fun delete(path: String) {
        when (val result = ApiService.delete(path, Unit::class.java)) {
            is ApiResult.Success -> Unit
            is ApiResult.HttpError -> throw IOException(myTripHttpError(result.code))
            is ApiResult.RateLimited -> throw IOException("Troppe richieste, riprova tra poco.")
            is ApiResult.NetworkError -> throw IOException("Impossibile raggiungere il server.")
        }
    }

    private suspend fun <T> fetch(path: String, type: Class<Array<T>>): List<T> =
        when (val result = ApiService.get(path, type)) {
            is ApiResult.Success -> result.data.toList()
            is ApiResult.HttpError -> throw IOException(myTripHttpError(result.code))
            is ApiResult.RateLimited -> throw IOException("Troppe richieste, riprova tra poco.")
            is ApiResult.NetworkError -> throw IOException("Impossibile raggiungere il server.")
        }

    private fun <T> build(type: Class<T>, fields: Map<String, Any?>): T =
        ApiService.gson.fromJson(ApiService.gson.toJson(fields), type)

    private fun bookingToTrip(b: BookingItemDTO): TripHomeDTO = build(
        TripHomeDTO::class.java,
        mapOf(
            "tripId" to b.tripId,
            "title" to b.itemTitle,
            "location" to b.itemLocation,
            "totalPrice" to b.price,
            "startDate" to b.itemDate,
            "endDate" to b.itemEndDate
        )
    )

    private fun bookingToActivity(b: BookingItemDTO): ActivityHomeDTO = build(
        ActivityHomeDTO::class.java,
        mapOf(
            "activityId" to b.activityId,
            "title" to b.itemTitle,
            "city" to b.itemLocation,
            "price" to b.price,
            "startDate" to b.itemDate,
            "endDate" to b.itemEndDate
        )
    )

    private fun favoriteToTrip(f: FavoriteItemDTO): TripHomeDTO = build(
        TripHomeDTO::class.java,
        mapOf("tripId" to f.itemId, "title" to f.title, "location" to f.city)
    )

    private fun favoriteToActivity(f: FavoriteItemDTO): ActivityHomeDTO = build(
        ActivityHomeDTO::class.java,
        mapOf("activityId" to f.itemId, "title" to f.title, "city" to f.city)
    )
}