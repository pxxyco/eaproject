package com.example.application.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.example.application.dto.SearchSuggestion
import com.example.application.ui.theme.Stone200

@Composable
fun SearchTopBar(
    searchQuery: String,
    suggestions: List<SearchSuggestion>,
    onQueryChange: (String) -> Unit,
    onSuggestionClick: (SearchSuggestion) -> Unit,
    onDismissSuggestions: () -> Unit,
    onSearchSubmit: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    var fieldWidth by remember { mutableStateOf(0.dp) }

    val submit = {
        focusManager.clearFocus()
        onSearchSubmit()
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                placeholder = { Text("Cerca destinazioni, attività o hotel...", color = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { fieldWidth = with(density) { it.width.toDp() } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submit() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Stone200,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            DropdownMenu(
                expanded = suggestions.isNotEmpty(),
                onDismissRequest = onDismissSuggestions,
                properties = PopupProperties(focusable = false),
                modifier = Modifier.width(fieldWidth)
            ) {
                suggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(suggestion.title, style = MaterialTheme.typography.bodyLarge)
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

        Spacer(Modifier.width(8.dp))

        Button(
            onClick = submit,
            modifier = Modifier.height(56.dp)
        ) {
            Text("Cerca")
        }
    }
}