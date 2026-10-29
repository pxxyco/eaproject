package com.example.application.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.application.AppDestinations
import com.example.application.screens.common.LoadingOverlay
import com.example.application.screens.common.LocalScreenLoadingHost
import com.example.application.screens.common.ScreenLoadingHost
import com.example.application.ui.effects.LocalNavigationOverlap
import com.example.application.ui.effects.LocalNavigationTail
import com.example.application.ui.effects.navigationFade

@Composable
internal fun FloatingNavigationLayout(
    destinations: List<AppDestinations>,
    selected: AppDestinations,
    onSelect: (AppDestinations) -> Unit,
    fadeContent: Boolean = true,
    content: @Composable (Modifier) -> Unit
) {
    val loadingHost = remember { ScreenLoadingHost() }
    val density = LocalDensity.current
    var navigationHeight by remember { mutableStateOf(80.dp) }
    val navigationOverlap = 22.dp
    val outerSpacing = 12.dp
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val pageFade = if (fadeContent) Modifier.navigationFade() else Modifier

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()
            .padding(bottom = (navigationHeight + outerSpacing + bottomInset - navigationOverlap).coerceAtLeast(0.dp))
            .then(pageFade)) {
            val contentModifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            CompositionLocalProvider(
                LocalNavigationTail provides 72.dp,
                LocalNavigationOverlap provides navigationOverlap,
                LocalScreenLoadingHost provides loadingHost
            ) { content(contentModifier) }
        }
        FloatingNavigation(
            destinations = destinations,
            selected = selected,
            onSelect = onSelect,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = 18.dp, vertical = outerSpacing)
                .onSizeChanged { navigationHeight = with(density) { it.height.toDp() } }
        )
        loadingHost.message?.let { LoadingOverlay(it, Modifier.fillMaxSize()) }
    }
}
