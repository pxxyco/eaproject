package com.example.application.screens.organizer

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.application.ui.theme.PopupSurface
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OfferDateField(label: String, value: String, includeTime: Boolean, enabled: Boolean, onChange: (String) -> Unit) {
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf<LocalDate?>(null) }
    val display = if (includeTime) runCatching {
        LocalDateTime.parse(value).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    }.getOrDefault("") else dateLabel(value)
    OutlinedTextField(display, {}, label = { Text(label) }, readOnly = true, enabled = enabled,
        modifier = Modifier.fillMaxWidth(), trailingIcon = {
            TextButton(enabled = enabled, onClick = { pickingDate = true }, shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.textButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer)) { Text("Scegli") }
        })
    if (pickingDate) {
        val initial = runCatching { LocalDate.parse(value.take(10)) }.getOrDefault(LocalDate.now())
        val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { pickingDate = false }, colors = DatePickerDefaults.colors(containerColor = PopupSurface),
            confirmButton = { TextButton(enabled = state.selectedDateMillis != null, onClick = {
                state.selectedDateMillis?.let { millis ->
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    if (includeTime) pickingTime = date else onChange(date.toString())
                }
                pickingDate = false
            }) { Text("Conferma") } },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Annulla") } }) {
            DatePicker(state, colors = DatePickerDefaults.colors(containerColor = PopupSurface))
        }
    }
    pickingTime?.let { date ->
        val initial = runCatching { LocalDateTime.parse(value).toLocalTime() }.getOrDefault(LocalTime.of(9, 0))
        val state = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
        AlertDialog(onDismissRequest = { pickingTime = null }, containerColor = PopupSurface,
            title = { Text("Scegli l’orario") }, text = { TimeInput(state) },
            confirmButton = { TextButton(onClick = {
                onChange(date.atTime(state.hour, state.minute).toString()); pickingTime = null
            }) { Text("Conferma") } },
            dismissButton = { TextButton(onClick = { pickingTime = null }) { Text("Annulla") } })
    }
}
