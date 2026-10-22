
package com.example.application.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.application.dto.home.ActivityHomeDTO
import com.example.application.dto.home.TripHomeDTO
import com.example.application.network.ApiResult
import com.example.application.network.ApiService
import com.google.gson.reflect.TypeToken
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object HomeRepository {

    private const val PAGE_SIZE = 9

    private val activityListType =
        object : TypeToken<List<ActivityHomeDTO>>() {}.type

    private val tripListType =
        object : TypeToken<List<TripHomeDTO>>() {}.type

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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    suspend fun searchActivities(
        query: String
    ): List<ActivityHomeDTO> {
        val path = buildString {
            append("/api/activities/search?page=0&size=$PAGE_SIZE")
            if (query.isNotBlank()) {
                append("&query=")
                append(encode(query.trim()))
            }
        }

        return getList(path, activityListType)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    suspend fun searchTrips(
        query: String
    ): List<TripHomeDTO> {
        val path = buildString {
            append("/api/trips/search?page=0&size=$PAGE_SIZE")
            if (query.isNotBlank()) {
                append("&query=")
                append(encode(query.trim()))
            }
        }

        return getList(path, tripListType)
    }


    suspend fun getActivitiesByCategory(
        category: String
    ): List<ActivityHomeDTO> {
        require(category in setOf(
            "EXCURSION",
            "VISIT",
            "TRANSPORT",
            "HOTEL",
            "MEAL",
            "OTHER"
        )) {
            "Categoria attività non valida: $category"
        }

        val path = "/api/activities/category/$category"

        return getList(path, activityListType)
    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)

    private suspend fun <T> getList(
        path: String,
        type: java.lang.reflect.Type
    ): List<T> {
        return when (val result = ApiService.getRaw(path)) {
            is ApiResult.Success -> {
                ApiService.gson.fromJson<List<T>>(
                    result.data,
                    type
                ) ?: emptyList()
            }

            is ApiResult.HttpError ->
                throw IllegalStateException(
                    "Errore HTTP ${result.code}: ${result.message}"
                )

            is ApiResult.RateLimited ->
                throw IllegalStateException(
                    "Troppe richieste. Riprova tra " +
                            "${result.retryAfterSeconds} secondi."
                )

            is ApiResult.NetworkError ->
                throw result.cause
        }
    }
}
