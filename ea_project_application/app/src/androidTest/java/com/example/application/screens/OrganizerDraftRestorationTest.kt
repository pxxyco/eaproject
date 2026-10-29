package com.example.application.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.application.screens.organizer.*
import com.example.application.ui.theme.ApplicationTheme
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OrganizerDraftRestorationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun newOfferRemainsOpenWithUnsavedFieldsAfterRecreation() {
        val restoration = StateRestorationTester(compose)
        var saves = 0
        restoration.setContent {
            ApplicationTheme {
                var editor by rememberSaveable(stateSaver = OrganizerOfferSaver) {
                    mutableStateOf<OrganizerOffer?>(null)
                }
                Button(onClick = { editor = OrganizerOffer(false, JsonObject()) }) { Text("Crea offerta test") }
                editor?.let { OfferEditor(it, { editor = null }, { saves++ }) }
            }
        }
        compose.onNodeWithText("Crea offerta test").performClick()
        compose.onNodeWithText("Titolo", useUnmergedTree = true).performTextInput("Museo serale")
        compose.onNodeWithText("Città", useUnmergedTree = true).performTextInput("Cosenza")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Museo serale").assertExists()
        compose.onNodeWithText("Cosenza").assertExists()
        compose.onNodeWithText("Salva").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, saves) }
    }

    @Test fun itineraryAndOpenStageDraftSurviveRecreation() {
        val restoration = StateRestorationTester(compose)
        var resultingStage: JsonObject? = null
        restoration.setContent {
            ApplicationTheme {
                val stages = rememberSaveable(saver = OfferStagesSaver) {
                    mutableStateListOf(JsonParser.parseString("""{"stageId":9,"title":"Museo","day":1,"category":"VISIT","latitude":39.3,"longitude":16.2,"startTime":"09:00"}""").asJsonObject)
                }
                Column(Modifier.verticalScroll(rememberScrollState())) { ItineraryEditor(stages, true) }
                SideEffect { resultingStage = stages.first() }
            }
        }
        compose.onNodeWithText("Modifica tappa 1").performScrollTo().performClick()
        compose.onNodeWithText("Titolo tappa", useUnmergedTree = true).performTextReplacement("Museo aggiornato")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Museo aggiornato").assertExists()
        compose.onNodeWithText("Conferma tappa").performClick()
        compose.runOnIdle {
            assertEquals("Museo aggiornato", resultingStage!!.get("title").asString)
            assertEquals(9, resultingStage!!.get("stageId").asInt)
            assertEquals(39.3, resultingStage!!.get("latitude").asDouble, .0001)
            assertEquals(16.2, resultingStage!!.get("longitude").asDouble, .0001)
            assertEquals("09:00", resultingStage!!.get("startTime").asString)
        }
    }
}
