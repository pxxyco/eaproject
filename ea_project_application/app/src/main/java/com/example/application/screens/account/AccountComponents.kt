package com.example.application.screens.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.application.ui.theme.GlassBorder
import com.example.application.ui.theme.GlassFill
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** One operation per account page; cancelling navigation cancels its UI work. */
internal class AccountActions(private val scope: CoroutineScope, val snackbar: SnackbarHostState) {
    var busy by mutableStateOf(false)
        private set
    fun run(operation: suspend () -> String?) {
        if (busy) return
        busy = true
        scope.launch {
            val message = try { operation() } finally { busy = false }
            message?.let { snackbar.showSnackbar(it) }
        }
    }
}

@Composable
internal fun AccountPanel(title: String, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = GlassFill, contentColor = MaterialTheme.colorScheme.onSurface, border = BorderStroke(1.dp, GlassBorder)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            content()
        }
    }
}

@Composable
internal fun AccountField(label: String, value: String, onChange: (String) -> Unit, enabled: Boolean,
    max: Int = 120, keyboard: KeyboardType = KeyboardType.Text, password: Boolean = false, multiline: Boolean = false) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(value = value, onValueChange = { if (it.length <= max) onChange(it) },
        label = { Text(label) }, modifier = Modifier.fillMaxWidth(), enabled = enabled,
        singleLine = !multiline, minLines = if (multiline) 4 else 1, shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = if (password && keyboard == KeyboardType.Text) KeyboardType.Password else keyboard),
        visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (password) ({ TextButton(onClick = { visible = !visible }) { Text(if (visible) "Nascondi" else "Mostra") } }) else null)
}

@Composable
internal fun AccountToggle(title: String, description: String, checked: Boolean, enabled: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, change, enabled = enabled)
    }
}
