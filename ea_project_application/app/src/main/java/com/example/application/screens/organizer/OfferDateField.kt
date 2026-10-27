package com.example.application.screens.organizer

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
internal fun OfferDateField(label: String, value: String, includeTime: Boolean, enabled: Boolean, onChange: (String) -> Unit) {
    val context = LocalContext.current
    val display = if (includeTime) runCatching {
        LocalDateTime.parse(value).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    }.getOrDefault("") else dateLabel(value)
    OutlinedTextField(display, {}, label = { Text(label) }, readOnly = true, enabled = enabled,
        modifier = Modifier.fillMaxWidth(), trailingIcon = {
            TextButton(enabled = enabled, onClick = {
                val currentDate = runCatching { LocalDate.parse(value.take(10)) }.getOrDefault(LocalDate.now())
                DatePickerDialog(context, { _, year, month, day ->
                    val date = LocalDate.of(year, month + 1, day)
                    if (!includeTime) onChange(date.toString()) else {
                        val time = runCatching { LocalDateTime.parse(value).toLocalTime() }.getOrDefault(LocalTime.of(9, 0))
                        TimePickerDialog(context, { _, hour, minute -> onChange(date.atTime(hour, minute).toString()) },
                            time.hour, time.minute, true).show()
                    }
                }, currentDate.year, currentDate.monthValue - 1, currentDate.dayOfMonth).show()
            }) { Text("Scegli") }
        })
}
