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
    val shape = RoundedCornerShape(20.dp)
    BoxWithConstraints(modifier.fillMaxWidth().padding(horizontal = edgePadding, vertical = 8.dp)) {
        val compact = maxWidth < 370.dp
        val labels = DetailTab.entries
        val groups = if (compact) labels.chunked(2) else listOf(labels)
        Column(Modifier.fillMaxWidth().clip(shape)
            .background(GlassFill.compositeOver(MaterialTheme.colorScheme.surface))
            .border(1.dp, GlassBorder, shape).padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            groups.forEach { group ->
                Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    group.forEach { tab ->
                        val active = tab == selected
                        val fill by animateColorAsState(if (active) NavigationSelection else Color.Transparent, label = "detail_section_fill")
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(fill)
                            .selectable(active, role = Role.Tab, onClick = { onSelect(tab) })
                            .heightIn(min = 48.dp).padding(horizontal = 4.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text(tab.label, color = if (active) NavigationAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelLarge, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
