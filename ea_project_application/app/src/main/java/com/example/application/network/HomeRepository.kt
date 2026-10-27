package com.example.application.network

import android.os.SystemClock
import com.example.application.dto.ActivitySuggestionDTO
import com.example.application.dto.SearchSuggestion
import com.example.application.dto.TripHomeDTO
import com.example.application.dto.TripSuggestionDTO
import com.example.application.dto.ActivityHomeDTO
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.lang.reflect.Type
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

object HomeRepository {

    private const val PAGE_SIZE = 9
    private const val CACHE_TTL_MS = 60_000L
    private const val MAX_CACHE_ENTRIES = 64

    private data class CacheEntry(
        val value: List<*>,
        val timestamp: Long
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val cacheLock = Any()
    private val activityListType =
        object : TypeToken<List<ActivityHomeDTO>>() {}.type

    private val tripListType =
        object : TypeToken<List<TripHomeDTO>>() {}.type

    private val activitySuggestionListType =
        object : TypeToken<List<ActivitySuggestionDTO>>() {}.type

    private val tripSuggestionListType =
        object : TypeToken<List<TripSuggestionDTO>>() {}.type

    private val categoryLabels = mapOf(
        "EXCURSION" to "Escursione",
        "VISIT" to "Visita",
        "TRANSPORT" to "Trasporto",
        "HOTEL" to "Hotel",
        "MEAL" to "Ristorazione",
        "OTHER" to "Altro"
    )

    //Home

    suspend fun getTopActivities(): List<ActivityHomeDTO> {
        return getList(
            "/api/activities/top/cards?page=0&size=$PAGE_SIZE",
            activityListType
        )
    }

    suspend fun getFeaturedTrips(): List<TripHomeDTO> {
        return getList(
            "/api/trips/search?page=0&size=$PAGE_SIZE",
            tripListType
        )
    }

    suspend fun getActivitiesByCategory(
        category: String
    ): List<ActivityHomeDTO> {
        require(
            category in setOf(
                "EXCURSION",
                "VISIT",
                "TRANSPORT",
                "HOTEL",
                "MEAL",
                "OTHER"
            )
        ) {
            "Categoria attività non valida: $category"
        }

        return getList("/api/activities/category/$category", activityListType)
    }

    //Ricerca

    suspend fun searchActivities(query: String): List<ActivityHomeDTO> {
        val path = buildString {
            append("/api/activities/search?page=0&size=$PAGE_SIZE")
            if (query.isNotBlank()) {
                append("&query=")
                append(encode(query.trim()))
            }
        }

        return getList(path, activityListType)
    }

    suspend fun searchTrips(query: String): List<TripHomeDTO> {
        val path = buildString {
            append("/api/trips/search?page=0&size=$PAGE_SIZE")
            if (query.isNotBlank()) {
                append("&query=")
                append(encode(query.trim()))
            }
        }

        return getList(path, tripListType)
    }

    //Suggerimenti TextBox
    suspend fun getSuggestions(query: String, limit: Int = 4): List<SearchSuggestion> =
        coroutineScope {
            val q = encode(query.trim())

            val activities = async {
                safeList<ActivitySuggestionDTO> {
                    getList(
                        "/api/activities/suggest?query=$q&limit=$limit",
                        activitySuggestionListType
                    )
                }
            }
            val trips = async {
                safeList<TripSuggestionDTO> {
                    getList(
                        "/api/trips/suggest?query=$q&limit=$limit",
                        tripSuggestionListType
                    )
                }
            }

            val activityItems = activities.await().mapNotNull { a ->
                val title = a.title?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val parts = listOfNotNull(
                    "Attività",
                    categoryLabels[a.category],
                    a.city?.takeIf { it.isNotBlank() }
                )
                SearchSuggestion(title, parts.joinToString(" · "))
            }

            val tripItems = trips.await().mapNotNull { t ->
                val title = t.title?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val parts = listOfNotNull(
                    "Viaggio",
                    t.location?.takeIf { it.isNotBlank() }
                )
                SearchSuggestion(title, parts.joinToString(" · "))
            }

            (activityItems + tripItems).distinctBy { it.title to it.subtitle }
        }

    //Altro

    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8")

    private suspend fun <T> safeList(block: suspend () -> List<T>): List<T> =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }

    private suspend fun <T> getList(
        path: String,
        type: Type
    ): List<T> {
        val now = SystemClock.elapsedRealtime()
        pruneCache(now)

        // Riutilizza i dati se la cache è ancora valida.
        cache[path]?.let { entry ->
            if (now - entry.timestamp < CACHE_TTL_MS) {
                @Suppress("UNCHECKED_CAST")
                return entry.value as List<T>
            } else {
                cache.remove(path, entry)
            }
        }

        return when (val result = ApiService.getRaw(path)) {
            is ApiResult.Success -> {
                val data: List<T> =
                    ApiService.gson.fromJson<List<T>>(result.data, type)
                        ?: emptyList()

                synchronized(cacheLock) {
                    cache[path] = CacheEntry(
                        value = data,
                        timestamp = SystemClock.elapsedRealtime()
                    )
                    pruneCache(SystemClock.elapsedRealtime())
                }

                data
            }

            is ApiResult.HttpError -> {
                throw IllegalStateException(
                    "Errore HTTP ${result.code}: ${result.message}"
                )
            }

            is ApiResult.RateLimited -> {
                throw IllegalStateException(
                    "Troppe richieste. Riprova tra " +
                            "${result.retryAfterSeconds} secondi."
                )
            }

            is ApiResult.NetworkError -> {
                throw result.cause
            }
        }
    }


    /**
     * Elimina le entry scadute e limita la cache per evitare crescita senza limiti
     * quando vengono eseguite ricerche con query sempre diverse.
     */
    private fun pruneCache(now: Long) {
        cache.forEach { (key, entry) ->
            if (now - entry.timestamp >= CACHE_TTL_MS) {
                cache.remove(key, entry)
            }
        }

        val excessEntries = cache.size - MAX_CACHE_ENTRIES
        if (excessEntries > 0) {
            cache.entries
                .sortedBy { it.value.timestamp }
                .take(excessEntries)
                .forEach { entry ->
                    cache.remove(entry.key, entry.value)
                }
        }
    }
}
