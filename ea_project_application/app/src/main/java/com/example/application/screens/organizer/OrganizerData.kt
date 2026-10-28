package com.example.application.screens.organizer

import com.example.application.network.ApiResult
import com.example.application.network.ApiService
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun JsonObject.text(key: String): String {
    val value = get(key)?.takeUnless { it.isJsonNull } ?: return ""
    if (value.isJsonPrimitive) return value.asString
    // Jackson can serialize java.time values as numeric arrays instead of ISO strings.
    if (value.isJsonArray && key in setOf("startDate", "endDate", "createdAt", "updatedAt", "deletedAt")) {
        return runCatching {
            val parts = value.asJsonArray.map { it.asInt }
            val date = LocalDate.of(parts[0], parts[1], parts[2])
            if (parts.size == 3) date.toString() else date.atTime(
                parts[3], parts[4], parts.getOrElse(5) { 0 }, parts.getOrElse(6) { 0 }
            ).toString()
        }.getOrDefault("")
    }
    return ""
}
internal fun JsonObject.number(key: String): Int = text(key).toIntOrNull() ?: 0
internal fun money(value: String): String = NumberFormat.getCurrencyInstance(Locale.ITALY)
    .format(value.toBigDecimalOrNull() ?: BigDecimal.ZERO)
internal fun dateLabel(value: String): String = runCatching {
    LocalDate.parse(value.take(10)).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}.getOrDefault(value)

internal data class OrganizerOffer(val trip: Boolean, val data: JsonObject) {
    val id get() = data.text(if (trip) "tripId" else "activityId")
    val path get() = "/api/${if (trip) "trips" else "activities"}"
    val title get() = data.text("title")
    val price get() = data.text(if (trip) "totalPrice" else "price")
    val city get() = data.text(if (trip) "destinationCity" else "city")
}

internal fun <T> ApiResult<T>.errorMessage(): String? = when (this) {
    is ApiResult.Success -> null
    is ApiResult.NetworkError -> "Connessione non disponibile. Riprova."
    is ApiResult.RateLimited -> "Troppe richieste. Riprova tra $retryAfterSeconds secondi."
    is ApiResult.HttpError -> when (code) {
        401 -> "Sessione scaduta. Accedi di nuovo."
        403 -> "Non hai i permessi per questa operazione."
        409 -> "L'offerta è cambiata o i posti sono insufficienti. Aggiorna la dashboard e riprova."
        400 -> "Dati non validi. Controlla i campi dell'offerta."
        502 -> "Risposta del server non valida. Riprova."
        404 -> "Offerta non più disponibile. Aggiorna la dashboard."
        else -> "Operazione non riuscita (errore $code). Riprova."
    }
}

internal suspend fun loadOrganizerList(path: String): ApiResult<List<JsonObject>> =
    when (val result = ApiService.get(path, JsonArray::class.java)) {
        is ApiResult.Success -> ApiResult.Success(result.data.map { it.asJsonObject })
        is ApiResult.HttpError -> result
        is ApiResult.NetworkError -> result
        is ApiResult.RateLimited -> result
    }

/** Builds an update without discarding fields that are not exposed in the mobile form. */
internal fun offerPayload(offer: OrganizerOffer, fields: Map<String, String>): JsonObject {
    val payload = offer.data.deepCopy()
    listOf("createdAt", "updatedAt", "deletedAt", "createdByUserId", "images", "averageRating", "icsUid").forEach(payload::remove)
    // Details-only updates preserve the itinerary; the editor attaches an explicit list when editing stages.
    if (offer.trip) payload.remove("stages")
    fields.forEach { (key, value) -> payload.addProperty(key, value.trim()) }
    val priceKey = if (offer.trip) "totalPrice" else "price"
    val price = fields.getValue(priceKey).replace(',', '.').toBigDecimalOrNull()
    require(price != null && price >= BigDecimal.ZERO) { "Inserisci un prezzo valido, anche zero." }
    payload.addProperty(priceKey, price)
    val seats = fields.getValue("maxSeats").toIntOrNull()
    val booked = (offer.data.number("maxSeats") - offer.data.number("availableSeats")).coerceAtLeast(0)
    require(seats != null && seats > 0 && seats >= booked) { "I posti devono essere positivi e almeno pari a quelli già prenotati ($booked)." }
    payload.addProperty("maxSeats", seats)
    payload.addProperty("availableSeats", seats - booked)
    require(fields.getValue("title").isNotBlank()) { "Inserisci il titolo." }
    require(fields.getValue(if (offer.trip) "destinationCity" else "city").isNotBlank()) { "Inserisci la città." }
    val start = fields.getValue("startDate").trim()
    val end = fields.getValue("endDate").trim()
    if (offer.trip) {
        require(fields.getValue("destinationCountry").isNotBlank()) { "Inserisci il paese." }
        val startDate = runCatching { LocalDate.parse(start) }.getOrNull()
        val endDate = runCatching { LocalDate.parse(end) }.getOrNull()
        require(startDate != null && endDate != null && !endDate.isBefore(startDate)) { "Date non valide: usa AAAA-MM-GG e una fine successiva o uguale all'inizio." }
    } else {
        val startDate = runCatching { LocalDateTime.parse(start) }.getOrNull()
        val endDate = runCatching { LocalDateTime.parse(end) }.getOrNull()
        require(startDate != null && endDate != null && !endDate.isBefore(startDate)) { "Date non valide: usa AAAA-MM-GGTHH:MM e una fine successiva o uguale all'inizio." }
        if (fields.containsKey("latitude") || fields.containsKey("longitude"))
            applyCoordinates(payload, fields["latitude"].orEmpty(), fields["longitude"].orEmpty())
        val duration = fields.getValue("durationMinutes").toIntOrNull()
        require(duration != null && duration > 0) { "Inserisci una durata positiva in minuti." }
        payload.addProperty("durationMinutes", duration)
    }
    if (offer.id.isBlank()) payload.addProperty("status", "PUBLISHED")
    return payload
}

internal fun offerStatuses(trip: Boolean): List<Pair<String, String>> = buildList {
    add("PUBLISHED" to "Pubblicata")
    if (trip) add("DRAFT" to "Bozza")
    add("CANCELLED" to "Annullata")
    add("COMPLETED" to "Conclusa")
}

internal fun OrganizerOffer.matches(query: String, status: String): Boolean =
    (status == "ALL" || data.text("status").equals(status, true)) &&
        (title.contains(query.trim(), true) || city.contains(query.trim(), true))
