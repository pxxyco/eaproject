package com.example.application.screens.organizer

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class OfferDraftStateTest {
    private val scope = object : SaverScope {
        override fun canBeSaved(value: Any): Boolean = true
    }
    private fun <T, S : Any> restore(saver: Saver<T, S>, value: T): T {
        val saved = with(saver) { scope.save(value) }
        return requireNotNull(saver.restore(requireNotNull(saved)))
    }

    @Test fun selectedOfferRetainsIdentityTypeAndServerFields() {
        val data = JsonParser.parseString("""{"tripId":7,"title":"Roma: sera","createdByUserId":4,"stages":[{"stageId":9,"notes":"Da conservare"}]}""").asJsonObject
        val recovered = restore(OrganizerOfferSaver, OrganizerOffer(true, data))!!
        assertTrue(recovered.trip)
        assertEquals("7", recovered.id)
        assertEquals(data, recovered.data)
        recovered.data.addProperty("title", "Modificato")
        assertEquals("Roma: sera", data.get("title").asString)
    }

    @Test fun unsavedFieldsRetainDatesCoordinatesAndTextIndependently() {
        val fields = mutableStateMapOf("title" to "Museo", "latitude" to "39,3", "longitude" to "16,2",
            "startDate" to "2027-06-01T09:00", "description" to "Prima riga\nSeconda riga")
        val recovered = restore(OfferFieldsSaver, fields)
        assertEquals(fields.toMap(), recovered.toMap())
        recovered["title"] = "Nuovo"
        assertEquals("Museo", fields["title"])
    }

    @Test fun stageOrderIdsTimesCoordinatesAndUnexposedFieldsArePreserved() {
        val first = JsonParser.parseString("""{"stageId":9,"day":2,"orderInDay":1,"latitude":39.3,"longitude":16.2,"startTime":"09:00","endTime":"10:00","notes":"Conserva","country":"Italia"}""").asJsonObject
        val stages = mutableStateListOf(first, JsonParser.parseString("""{"title":"Nuova tappa","day":3}""").asJsonObject)
        val recovered = restore(OfferStagesSaver, stages)
        assertEquals(stages.toList(), recovered.toList())
        recovered[0].addProperty("day", 4)
        assertEquals(2, first.get("day").asInt)
        assertFalse(recovered[1].has("stageId"))
    }
}
