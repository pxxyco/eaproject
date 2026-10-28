package com.example.application.screens.activity

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.application.ui.theme.*

internal enum class DetailTab(val label: String) {
    DESCRIPTION("Descrizione"), ITINERARY("Itinerario"), GALLERY("Foto"), REVIEWS("Recensioni")
}

@Composable
internal fun DetailTabs(
    selected: DetailTab,
    onSelect: (DetailTab) -> Unit,
    modifier: Modifier = Modifier,
    edgePadding: Dp = 16.dp
) {
    val list = rememberLazyListState()
    LaunchedEffect(selected) { list.animateScrollToItem(selected.ordinal) }
    val shape = RoundedCornerShape(24.dp)
    Box(modifier.fillMaxWidth().padding(horizontal = edgePadding, vertical = 8.dp)
        .clip(shape).background(GlassFill.compositeOver(MaterialTheme.colorScheme.surface))
        .border(1.dp, GlassBorder, shape)) {
        LazyRow(state = list, modifier = Modifier.selectableGroup(), contentPadding = PaddingValues(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(DetailTab.entries, key = { it.name }) { tab ->
                val active = tab == selected
                val fill by animateColorAsState(if (active) NavigationSelection else Color.Transparent, label = "detail_section_fill")
                val tint by animateColorAsState(if (active) NavigationAccent else MaterialTheme.colorScheme.onSurfaceVariant, label = "detail_section_tint")
                Box(Modifier.clip(RoundedCornerShape(18.dp)).background(fill)
                    .selectable(active, role = Role.Tab, onClick = { onSelect(tab) })
                    .heightIn(min = 44.dp).padding(horizontal = 18.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Text(tab.label, color = tint, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        }
    }
}
