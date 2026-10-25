package com.example.application.network

import androidx.compose.runtime.mutableStateListOf
import com.example.application.dto.ActivityHomeDTO
import com.example.application.dto.TripHomeDTO
import java.io.IOException

enum class ListVisibility(val label: String) {
    PRIVATE("Privata"), SHARED("Condivisa"), PUBLIC("Pubblica")
}

/** Solo i campi che servono. Niente LocalDateTime: il Gson di ApiService non lo sa leggere. */
data class BookingItemDTO(
    val bookingId: Long? = null,
    val tripId: Long? = null,
    val activityId: Long? = null,
    val itemTitle: String? = null,
    val itemDate: String? = null,
    val itemEndDate: String? = null,
    val itemLocation: String? = null,
    val bookingType: String? = null,
    val bookingStatus: String? = null,
    val price: Double? = null
)

data class FavoriteItemDTO(
    val id: Long? = null,
    val type: String? = null,
    val itemId: Long? = null,
    val title: String? = null,
    val city: String? = null
)

data class FavoriteList(
    val id: Long,
    val name: String,
    val visibility: ListVisibility = ListVisibility.PRIVATE,
    val itemCount: Int = 0
)

data class FavoriteListItemDTO(
    val id: Long? = null,
    val name: String? = null,
    val visibility: String? = null,
    val deleted: Boolean? = null
)

private data class CreateListRequest(val name: String, val visibility: String)

/** Modelli solo UI per i contatori dell'hub, ancora dati locali. */
data class BookingSummary(val id: Long, val upcoming: Boolean)
data class FavoriteSummary(val id: Long, val type: String, val itemId: Long)

object MyTripRepository {

    // ---------- Hub (ancora dati locali di prova) ----------


    private val _bookings = mutableStateListOf(
        BookingSummary(1, upcoming = false), // CANCELLED
        BookingSummary(2, upcoming = true),  // PENDING
        BookingSummary(3, upcoming = true)   // PENDING
    )
    private val _favorites = mutableStateListOf(FavoriteSummary(1, "ACTIVITY", 7))
    private val _lists = mutableStateListOf<FavoriteList>()

    val lists: List<FavoriteList> get() = _lists
    val upcomingBookings: Int get() = _bookings.count { it.upcoming }
    val favoritesCount: Int get() = _favorites.size

    private fun FavoriteListItemDTO.toUi(): FavoriteList? {
        val listId = id ?: return null
        val vis = runCatching { ListVisibility.valueOf(visibility.orEmpty()) }
            .getOrDefault(ListVisibility.PRIVATE)
        return FavoriteList(listId, name.orEmpty().ifBlank { "Senza nome" }, vis)
    }

    /** Ricarica le liste dell'utente dal server. */
    suspend fun refreshLists() {
        val remote = fetch("/api/favorite-lists/mine", Array<FavoriteListItemDTO>::class.java)
        _lists.clear()
        _lists.addAll(remote.mapNotNull { it.toUi() })
    }

    /** Crea la lista sul server; aggiunge all'hub solo se il salvataggio riesce. */
    suspend fun createList(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        val request = CreateListRequest(clean, ListVisibility.PRIVATE.name)
        when (val result = ApiService.post("/api/favorite-lists", request, FavoriteListItemDTO::class.java)) {
            is ApiResult.Success -> result.data.toUi()?.let { _lists.add(it) }
            is ApiResult.HttpError -> throw IOException("Errore ${result.code} nel salvataggio della lista.")
            is ApiResult.RateLimited -> throw IOException("Troppe richieste, riprova tra poco.")
            is ApiResult.NetworkError -> throw IOException("Impossibile raggiungere il server.")
        }
    }

    // ---------- Contenuto delle raccolte (backend reale) ----------

    sealed class HubTarget(val title: String, val path: String) {
        data object Bookings : HubTarget("Le mie prenotazioni", "/api/bookings/mine")
        data object Favorites : HubTarget("Le mie preferite", "/api/favorites")
        data class Custom(val list: FavoriteList) :
            HubTarget(list.name, "/api/favorite-lists/${list.id}")
    }

    data class CollectionItemsDTO(
        val trips: List<TripHomeDTO>? = null,
        val activities: List<ActivityHomeDTO>? = null
    )

    suspend fun loadItems(target: HubTarget): CollectionItemsDTO = when (target) {
        HubTarget.Bookings -> {
            val bookings = fetch(target.path, Array<BookingItemDTO>::class.java)
                .filter { it.bookingStatus != "CANCELLED" } // togli questa riga per mostrarle
            CollectionItemsDTO(
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

        is HubTarget.Custom ->
            throw IOException("Il contenuto delle liste personalizzate non è ancora disponibile.")
    }

    private suspend fun <T> fetch(path: String, type: Class<Array<T>>): List<T> =
        when (val result = ApiService.get(path, type)) {
            is ApiResult.Success -> result.data.toList()
            is ApiResult.HttpError -> throw IOException("Errore ${result.code} su $path")
            is ApiResult.RateLimited -> throw IOException("Troppe richieste, riprova tra poco.")
            is ApiResult.NetworkError -> throw IOException("Impossibile raggiungere il server.")
        }

    // ---------- Conversione verso i DTO della home ----------
    // Costruisce un JSON con i nomi dei campi della home e lo fa leggere a Gson,
    // così non serve conoscere i costruttori di TripHomeDTO e ActivityHomeDTO.

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