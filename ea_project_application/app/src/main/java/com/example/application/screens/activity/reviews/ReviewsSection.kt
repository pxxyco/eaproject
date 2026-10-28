package com.example.application.screens.activity.reviews

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.application.ui.theme.Stone200
import com.example.application.ui.theme.Stone400

internal fun LazyListScope.reviewsSection(state: ReviewsState) {
    val reviews = state.reviews
    val error = state.error

    item(key = "reviews-summary") {
        ReviewsBlock(top = 20.dp) {
            ReviewsSummary(count = reviews?.size)

            if (state.canWrite) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = state::openNew,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Stone200,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Scrivi una recensione")
                }
            }
        }
    }

    when {
        error != null -> item(key = "reviews-error") {
            ReviewsBlock { Text(error, style = MaterialTheme.typography.bodyMedium, color = Stone400) }
        }

        reviews == null -> item(key = "reviews-loading") {
            ReviewsBlock {
                CircularProgressIndicator(
                    color = Stone200,
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
        }

        reviews.isEmpty() -> item(key = "reviews-empty") {
            ReviewsBlock {
                Text(
                    text = "Ancora nessuna recensione.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Stone400
                )
            }
        }

        else -> items(items = reviews, key = { it.reviewId ?: it.hashCode().toLong() }) { review ->
            val mine = state.isMine(review)
            ReviewsBlock {
                ReviewCard(
                    review = review,
                    onEdit = if (mine) ({ state.openEdit(review) }) else null,
                    onDelete = if (mine) ({ state.askDelete(review) }) else null,
                    onRespond = if (state.canRespond(review)) ({ state.openNewResponse(review) }) else null,
                    onEditResponse = if (state.canManageResponse(review)) ({ state.openEditResponse(review) }) else null,
                    onDeleteResponse = if (state.canManageResponse(review)) ({ state.askDeleteResponse(review) }) else null
                )
            }
        }
    }
}

@Composable
private fun ReviewsBlock(
    top: androidx.compose.ui.unit.Dp = 6.dp,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = top, bottom = 6.dp),
        content = content
    )
}

@Composable
private fun ReviewsSummary(count: Int?) {
    Column {
        if (count != null) {
            Text(
                text = if (count == 1) "1 recensione" else "$count recensioni",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}