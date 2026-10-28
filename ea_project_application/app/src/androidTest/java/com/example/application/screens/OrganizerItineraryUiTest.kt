package com.example.application.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.application.screens.organizer.ItineraryEditor
import com.example.application.ui.theme.ApplicationTheme
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrganizerItineraryUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun organizerCanAddEditAndRemoveStageWithoutTravelerFeatures() {
        val stages = mutableStateListOf<JsonObject>()
        compose.setContent {
            ApplicationTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) { ItineraryEditor(stages, true) }
            }
        }
        compose.onNodeWithText("+ Aggiungi tappa").performClick()
        compose.onNodeWithText("Titolo tappa").performTextInput("Museo")
        compose.onNodeWithText("Giorno del viaggio").performTextReplacement("2")
        compose.onNodeWithText("Conferma tappa").performClick()
        compose.onNodeWithText("Giorno 2 · Museo").assertIsDisplayed()
        compose.onNodeWithText("Modifica tappa 1").performClick()
        compose.onNodeWithText("Titolo tappa").performTextReplacement("Centro storico")
        compose.onNodeWithText("Conferma tappa").performClick()
        compose.runOnIdle {
            assertEquals("Centro storico", stages.single().get("title").asString)
            assertEquals(2, stages.single().get("day").asInt)
        }
        compose.onNodeWithText("Rimuovi").performClick()
        compose.runOnIdle { assertEquals(0, stages.size) }
    }
}
