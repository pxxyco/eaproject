package com.example.application.screens.organizer

import com.google.gson.JsonNull
import com.google.gson.JsonObject

internal fun applyCoordinates(payload: JsonObject, latitude: String, longitude: String) {
    val lat = latitude.trim().replace(',', '.')
    val lon = longitude.trim().replace(',', '.')
    if (lat.isBlank() && lon.isBlank()) {
        payload.add("latitude", JsonNull.INSTANCE)
        payload.add("longitude", JsonNull.INSTANCE)
        return
    }
    val first = lat.toDoubleOrNull()
    val second = lon.toDoubleOrNull()
    require(first != null && second != null && first.isFinite() && second.isFinite() && first in -90.0..90.0 && second in -180.0..180.0) {
        "Inserisci entrambe le coordinate: latitudine tra -90 e 90, longitudine tra -180 e 180."
    }
    payload.addProperty("latitude", first)
    payload.addProperty("longitude", second)
}
