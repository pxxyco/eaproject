package com.example.application.screens.account

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp

/** The stored value contains exactly MMYY digits. Formatting never rewrites the cursor position. */
internal object ExpiryTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val result = if (raw.length > 2) raw.take(2) + "/" + raw.drop(2) else raw
        return TransformedText(AnnotatedString(result), object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = if (offset <= 2) offset else offset + 1
            override fun transformedToOriginal(offset: Int) = (if (offset <= 2) offset else offset - 1).coerceIn(0, raw.length)
        })
    }
}

@Composable
internal fun CardExpiryField(value: String, enabled: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(value, onValueChange = { onChange(it.filter { c -> c in '0'..'9' }.take(4)) },
        modifier = Modifier.fillMaxWidth(), label = { Text("Scadenza (MM/AA)") }, placeholder = { Text("02/27") },
        enabled = enabled, singleLine = true, shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        visualTransformation = ExpiryTransformation)
}
