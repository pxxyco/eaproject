package com.example.application.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.application.dto.ActivityHomeDTO
import com.example.application.dto.ExperienceRef
import com.example.application.dto.ExperienceType
import com.example.application.dto.TripHomeDTO
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class HomeMergeNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun pagedRowsOpenCorrectExperienceAndKeepLoadMoreSeparate() {
        var opened: ExperienceRef? = null
        var loads = 0
        compose.setContent { MaterialTheme {
            Column {
                ActivityRow(PagedList(items = listOf(ActivityHomeDTO(activityId = 7, title = "Museo attività")), hasMore = true),
                    onExperienceClick = { opened = it }, onLoadMore = { loads++ })
                TripRow(PagedList(items = listOf(TripHomeDTO(tripId = 9, title = "Viaggio Roma"))),
                    onExperienceClick = { opened = it }, onLoadMore = { loads++ })
            }
        } }
        compose.onNodeWithText("Museo attività").performClick()
        compose.runOnIdle { assertEquals(ExperienceRef(ExperienceType.ACTIVITY, 7), opened) }
        compose.onNodeWithText("Viaggio Roma").performClick()
        compose.runOnIdle { assertEquals(ExperienceRef(ExperienceType.TRIP, 9), opened) }
        compose.onNodeWithText("Carica altro").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, loads)
            assertEquals(ExperienceRef(ExperienceType.TRIP, 9), opened)
        }
    }
}
