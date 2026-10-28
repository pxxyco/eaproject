package com.example.application.screens.home
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.example.application.dto.SearchSuggestion
import com.example.application.ui.theme.Stone200
import kotlinx.coroutines.delay

@Composable
fun SearchTopBar(
    searchQuery: String,
    suggestions: List<SearchSuggestion>,
    onQueryChange: (String) -> Unit,
    onSuggestionClick: (SearchSuggestion) -> Unit,
    onDismissSuggestions: () -> Unit,
    onSearchSubmit: () -> Unit,
    onClear: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    var fieldWidth by remember { mutableStateOf(0.dp) }


    val fieldInteraction = remember { MutableInteractionSource() }
    val isFocused by fieldInteraction.collectIsFocusedAsState()
    val buttonInteraction = remember { MutableInteractionSource() }
    val isPressed by buttonInteraction.collectIsPressedAsState()

    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    val iconTint by animateColorAsState(
        targetValue = if (isFocused) Stone200 else Color.Gray,
        animationSpec = tween(250),
        label = "iconTint"
    )
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "buttonScale"
    )
    val fieldScale by animateFloatAsState(
        targetValue = if (isFocused) 1.01f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "fieldScale"
    )
    val clearProgress by animateFloatAsState(
        targetValue = if (searchQuery.isNotEmpty()) 1f else 0f,
        animationSpec = tween(200),
        label = "clearProgress"
    )

    val submit = {
        focusManager.clearFocus()
        onDismissSuggestions()
        onSearchSubmit()
    }

    AnimatedVisibility(
        visible = appeared,
        enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -it / 2 }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    interactionSource = fieldInteraction,
                    placeholder = {
                        Text("Cerca attività...", color = Color.Gray)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = iconTint
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                focusManager.clearFocus()
                                onClear()
                            },
                            enabled = searchQuery.isNotEmpty(),
                            modifier = Modifier.graphicsLayer {
                                alpha = clearProgress
                                scaleX = clearProgress
                                scaleY = clearProgress
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancella",
                                tint = Color.Gray
                            )
                        }
                    },
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(fieldScale)
                        .onSizeChanged { fieldWidth = with(density) { it.width.toDp() } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { submit() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Stone200,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Stone200
                    )
                )

                DropdownMenu(
                    expanded = suggestions.isNotEmpty(),
                    onDismissRequest = onDismissSuggestions,
                    properties = PopupProperties(focusable = false),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.width(fieldWidth)
                ) {
                    suggestions.forEachIndexed { index, suggestion ->
                        var visible by remember(suggestion) { mutableStateOf(false) }
                        LaunchedEffect(suggestion) {
                            delay(index * 40L)
                            visible = true
                        }

                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 3 }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            suggestion.title,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            suggestion.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    focusManager.clearFocus()
                                    onSuggestionClick(suggestion)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = submit,
                interactionSource = buttonInteraction,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .height(56.dp)
                    .scale(buttonScale)
            ) {
                Text("Cerca")
            }
        }
    }
}