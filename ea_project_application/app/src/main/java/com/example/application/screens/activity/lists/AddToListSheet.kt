package com.example.application.screens.activity.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.application.dto.ExperienceRef
import com.example.application.dto.FavoriteList
import com.example.application.network.MyTripRepository
import com.example.application.ui.theme.GlassBorder
import com.example.application.ui.theme.PopupSurface
import com.example.application.ui.theme.Stone200
import com.example.application.ui.theme.Stone400
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddToListSheet(
    ref: ExperienceRef,
    onDismiss: () -> Unit,
    onAdded: (listName: String) -> Unit
) {
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var newName by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val lists = MyTripRepository.lists

    LaunchedEffect(Unit) {
        MyTripRepository.refreshLists()
        error = MyTripRepository.hubErrors.firstOrNull { it.startsWith("Liste") }
        loading = false
    }

    fun run(action: suspend () -> String?) {
        saving = true
        error = null
        scope.launch {
            try {
                action()?.let(onAdded)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "Impossibile aggiungere alla lista"
            } finally {
                saving = false
            }
        }
    }

    fun addTo(list: FavoriteList) = run {
        MyTripRepository.addToList(list.id, ref)
        list.name
    }

    fun createAndAdd() = run {
        val created = MyTripRepository.createList(newName) ?: return@run null
        MyTripRepository.addToList(created.id, ref)
        newName = ""
        created.name
    }

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
                text = "Aggiungi a una lista",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            when {
                loading -> CircularProgressIndicator(
                    color = Stone200,
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.CenterHorizontally)
                )

                lists.isEmpty() -> Text(
                    text = "Non hai ancora nessuna lista.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Stone400
                )

                else -> LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(items = lists, key = { it.id }) { list ->
                        Text(
                            text = list.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !saving) { addTo(list) }
                                .padding(vertical = 14.dp)
                        )
                        HorizontalDivider(color = GlassBorder)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Nuova lista",
                style = MaterialTheme.typography.titleSmall,
                color = Stone400,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("Nome della lista") },
                    singleLine = true,
                    enabled = !saving,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = Stone200,
                        unfocusedBorderColor = GlassBorder,
                        focusedPlaceholderColor = Stone400,
                        unfocusedPlaceholderColor = Stone400,
                        cursorColor = Stone200
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = ::createAndAdd,
                    enabled = newName.isNotBlank() && !saving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Stone200,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Crea")
                }
            }

            if (saving) {
                Spacer(Modifier.height(12.dp))
                CircularProgressIndicator(
                    color = Stone200,
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
