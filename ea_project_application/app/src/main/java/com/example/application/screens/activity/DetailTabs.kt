package com.example.application.screens.activity

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.PrimaryIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.application.ui.theme.GlassBorder
import com.example.application.ui.theme.ScreenBackground
import com.example.application.ui.theme.Stone200
import com.example.application.ui.theme.Stone400

internal enum class DetailTab(val label: String) {
    DESCRIPTION("Descrizione"),
    ITINERARY("Itinerario"),
    GALLERY("Foto"),
    REVIEWS("Recensioni")
}

@Composable
internal fun DetailTabs(
    selected: DetailTab,
    onSelect: (DetailTab) -> Unit,
    modifier: Modifier = Modifier,
    edgePadding: Dp = 16.dp
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = selected.ordinal,
        modifier = modifier.fillMaxWidth(),
        containerColor = ScreenBackground,
        contentColor = Color.White,
        edgePadding = edgePadding,
        indicator = {
            PrimaryIndicator(
                modifier = Modifier.tabIndicatorOffset(selected.ordinal, matchContentSize = true),
                width = Dp.Unspecified,
                color = Stone200
            )
        },
        divider = { HorizontalDivider(color = GlassBorder) }
    ) {
        DetailTab.entries.forEach { tab ->
            Tab(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                selectedContentColor = Color.White,
                unselectedContentColor = Stone400,
                text = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                }
            )
        }
    }
}