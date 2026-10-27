package com.example.application.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.application.AppDestinations
import com.example.application.ui.theme.GlassBorder
import com.example.application.ui.theme.GlassFill
import com.example.application.ui.theme.NavigationAccent
import com.example.application.ui.theme.NavigationSelection
import com.example.application.ui.theme.NavigationOutline

@Composable
internal fun FloatingNavigation(destinations: List<AppDestinations>, selected: AppDestinations, onSelect: (AppDestinations) -> Unit,
    modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(32.dp)
    Box(modifier.widthIn(max = 560.dp).fillMaxWidth()) {
        // Paint the soft shadow without an elevated RenderNode: translucent elevation can
        // leave a bright rectangular strip behind the labels on Android.
        Box(Modifier.matchParentSize().dropShadow(shape, Shadow(radius = 12.dp, color = Color.Black.copy(alpha = .18f), offset = DpOffset(0.dp, 4.dp))).clip(shape).background(GlassFill).border(1.dp, GlassBorder, shape))
        Row(Modifier.selectableGroup().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            destinations.forEach { destination ->
                val active = selected == destination
                val tint by animateColorAsState(if (active) NavigationAccent else MaterialTheme.colorScheme.onSurfaceVariant, label = "navigation_tint")
                val fill by animateColorAsState(if (active) NavigationSelection else Color.Transparent, label = "navigation_selection")
                val interactions = remember { MutableInteractionSource() }
                Column(Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(fill)
                    .then(if (active) Modifier.border(1.dp, NavigationOutline.copy(alpha = .6f), RoundedCornerShape(24.dp)) else Modifier)
                    .selectable(active, role = Role.Tab, interactionSource = interactions, indication = null, onClick = { onSelect(destination) })
                    .heightIn(min = 64.dp).padding(vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(painterResource(destination.icon), null, Modifier.size(26.dp), tint = tint)
                    Text(destination.label, style = MaterialTheme.typography.labelMedium, color = tint, maxLines = 1)
                }
            }
        }
    }
}
