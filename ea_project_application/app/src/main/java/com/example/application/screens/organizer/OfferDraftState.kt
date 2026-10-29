package com.example.application.screens.organizer

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.google.gson.JsonParser

// Store only draft values; network operations are never replayed during restoration.
internal val OrganizerOfferSaver = Saver<OrganizerOffer?, String>(
    save = { offer -> offer?.let { "${it.trip}:" + it.data.toString() } },
    restore = { value -> OrganizerOffer(value.substringBefore(':').toBooleanStrict(),
        JsonParser.parseString(value.substringAfter(':')).asJsonObject) }
)

internal val OfferFieldsSaver = mapSaver<SnapshotStateMap<String, String>>(
    save = { it.toMap() },
    restore = { saved -> mutableStateMapOf<String, String>().apply {
        saved.forEach { (key, value) -> put(key, value as String) }
    } }
)

internal val OfferStagesSaver = listSaver<SnapshotStateList<com.google.gson.JsonObject>, String>(
    save = { stages -> stages.map { it.toString() } },
    restore = { values -> values.map { JsonParser.parseString(it).asJsonObject }.toMutableStateList() }
)
