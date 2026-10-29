package com.example.application.screens.activity.reviews

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.application.ui.theme.PopupSurface
import com.example.application.ui.theme.Stone200
import com.example.application.ui.theme.Stone400
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun ReviewDialogs(state: ReviewsState) {
    state.form?.let { form ->
        key(form) {
            ReviewFormSheet(
                initial = (form as? ReviewForm.Edit)?.review,
                onDismiss = state::closeForm,
                onSubmit = { rating, comment -> state.save(rating, comment) }
            )
        }
    }

    state.pendingDelete?.let { review ->
        key(review) {
            ConfirmDeleteDialog(
                title = "Eliminare la recensione?",
                onCancel = state::cancelDelete,
                onConfirm = { state.confirmDelete() }
            )
        }
    }

    state.responseForm?.let { form ->
        key(form) {
            ResponseFormSheet(
                review = form.review,
                initialComment = (form as? ResponseForm.Edit)?.response?.comment,
                onDismiss = state::closeResponseForm,
                onSubmit = { comment -> state.saveResponse(comment) }
            )
        }
    }

    state.pendingResponseDelete?.let { response ->
        key(response) {
            ConfirmDeleteDialog(
                title = "Eliminare la risposta?",
                onCancel = state::cancelDeleteResponse,
                onConfirm = { state.confirmDeleteResponse() }
            )
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    onCancel: () -> Unit,
    onConfirm: suspend () -> Unit
) {
    var deleting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!deleting) onCancel() },
        containerColor = PopupSurface,
        title = { Text(title, color = Color.White) },
        text = {
            Text(
                text = error ?: "L'operazione non si può annullare.",
                color = if (error != null) MaterialTheme.colorScheme.error else Stone400
            )
        },
        confirmButton = {
            TextButton(
                enabled = !deleting,
                onClick = {
                    deleting = true
                    error = null
                    scope.launch {
                        try {
                            onConfirm()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            error = e.message ?: "Eliminazione non riuscita"
                        } finally {
                            deleting = false
                        }
                    }
                }
            ) {
                Text(
                    text = if (deleting) "Elimino..." else "Elimina",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !deleting) {
                Text("Annulla", color = Stone200)
            }
        }
    )
}