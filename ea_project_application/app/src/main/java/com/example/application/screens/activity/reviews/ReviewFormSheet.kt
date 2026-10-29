package com.example.application.screens.activity.reviews

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.application.dto.ReviewDTO
import com.example.application.ui.theme.GlassBorder
import com.example.application.ui.theme.PopupSurface
import com.example.application.ui.theme.RatingAmber
import com.example.application.ui.theme.Stone200
import com.example.application.ui.theme.Stone400
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val MAX_COMMENT = 200

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReviewFormSheet(
    initial: ReviewDTO?,
    onDismiss: () -> Unit,
    onSubmit: suspend (rating: Int, comment: String) -> Unit
) {
    var rating by rememberSaveable { mutableIntStateOf(initial?.rating ?: 0) }
    var comment by rememberSaveable { mutableStateOf(initial?.comment.orEmpty()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = { if (!saving) onDismiss() },
        containerColor = PopupSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
        ) {
            Text(
                text = if (initial == null) "Scrivi una recensione" else "Modifica la recensione",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            StarPicker(rating = rating, onRatingChange = { rating = it })
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = comment,
                onValueChange = { if (it.length <= MAX_COMMENT) comment = it },
                label = { Text("Commento (facoltativo)") },
                minLines = 3,
                supportingText = {
                    Text(
                        text = "${comment.length}/$MAX_COMMENT",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Stone200,
                    unfocusedBorderColor = GlassBorder,
                    focusedLabelColor = Stone200,
                    unfocusedLabelColor = Stone400,
                    cursorColor = Stone200
                ),
                modifier = Modifier.fillMaxWidth()
            )

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss, enabled = !saving) {
                    Text("Annulla", color = Stone200)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        saving = true
                        error = null
                        scope.launch {
                            try {
                                onSubmit(rating, comment)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                error = e.message ?: "Impossibile salvare la recensione"
                            } finally {
                                saving = false
                            }
                        }
                    },
                    enabled = rating > 0 && !saving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Stone200,
                        contentColor = Color.Black
                    )
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text("Pubblica")
                    }
                }
            }
        }
    }
}

@Composable
private fun StarPicker(rating: Int, onRatingChange: (Int) -> Unit) {
    Row {
        for (value in 1..5) {
            Text(
                text = if (value <= rating) "★" else "☆",
                color = RatingAmber,
                fontSize = 36.sp,
                modifier = Modifier
                    .clickable { onRatingChange(value) }
                    .padding(end = 6.dp)
            )
        }
    }
}