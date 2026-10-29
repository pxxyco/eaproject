package com.example.application.screens.chat

import android.net.Uri
import java.io.ByteArrayOutputStream
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import com.example.application.network.*
import com.example.application.ui.theme.*
import com.example.application.ui.effects.LocalNavigationTail
import kotlinx.coroutines.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun ChatScreen(state: ChatState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    DisposableEffect(state) { state.visible = true; onDispose { state.visible = false } }
    LaunchedEffect(state) { state.refresh() }
    BackHandler(state.selected != null) { if (!state.sending) state.closeConversation() }
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val conversation = state.selected
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (conversation != null) TextButton(enabled = !state.sending, onClick = { state.closeConversation() }) { Text("←") }
            Column(Modifier.weight(1f)) {
                Text(conversation?.name ?: "Messaggi", style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (state.connected) "In tempo reale" else "Riconnessione in corso…", style = MaterialTheme.typography.labelSmall, color = Stone400)
            }
            TextButton(enabled = !state.busy, onClick = { scope.launch { state.refresh() } }) { Text("Aggiorna") }
        }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        if (conversation == null) Inbox(state, Modifier.weight(1f))
        else key(conversation.id) { ConversationContent(state, conversation, Modifier.weight(1f)) }
    }
}

@Composable
private fun Inbox(state: ChatState, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    var deleting by remember { mutableStateOf<Conversation?>(null) }
    LazyColumn(modifier, contentPadding = PaddingValues(bottom = LocalNavigationTail.current), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!state.busy && state.inbox.isEmpty()) item {
            Text("Le tue conversazioni iniziano qui", style = MaterialTheme.typography.titleMedium)
            Text("Apri un’esperienza e scegli Contatta organizzatore. Gli organizzatori possono contattare i partecipanti dalla dashboard.", color = Stone400)
        }
        items(state.inbox, key = { it.id }) { chat ->
            var menu by remember { mutableStateOf(false) }
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = GlassFill, border = BorderStroke(1.dp, GlassBorder)) {
                Row(Modifier.clickable(enabled = !state.busy) { scope.launch { state.select(chat) } }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(chat.name.take(2).uppercase(), Modifier.padding(14.dp), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(chat.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(chat.preview.orEmpty().ifBlank { "Inizia la conversazione" }, maxLines = 2, overflow = TextOverflow.Ellipsis, color = Stone400)
                        Text(timeLabel(chat.updatedAt), style = MaterialTheme.typography.labelSmall, color = Stone400)
                    }
                    if (chat.unread > 0) Badge { Text(chat.unread.coerceAtMost(99).toString()) }
                    Box {
                        TextButton(onClick = { menu = true }) { Text("⋮") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Segna come letto") }, onClick = { menu = false; scope.launch { state.readAll(chat) } })
                            DropdownMenuItem(text = { Text("Elimina chat") }, onClick = { menu = false; deleting = chat })
                        }
                    }
                }
            }
        }
        if (state.moreInbox) item { OutlinedButton(enabled = !state.busy, onClick = { scope.launch { state.nextPage() } }) { Text("Altre conversazioni") } }
    }
    deleting?.let { chat -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Eliminare la chat?") },
        text = { Text("La cronologia verrà rimossa solo per te. L’altra persona conserverà i suoi messaggi.") },
        confirmButton = { TextButton(enabled = !state.busy, onClick = { scope.launch { state.delete(chat); deleting = null } }) { Text("Elimina") } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text("Annulla") } }) }
}

@Composable
private fun ConversationContent(state: ChatState, conversation: Conversation, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val list = rememberLazyListState()
    var text by remember { mutableStateOf("") }
    var reply by remember { mutableStateOf<ChatMessage?>(null) }
    var photo by remember { mutableStateOf<ByteArray?>(null) }
    var mime by remember { mutableStateOf("image/jpeg") }
    var reading by remember { mutableStateOf(false) }
    var photoError by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) scope.launch {
            reading = true; photoError = null
            try {
                val result = withContext(Dispatchers.IO) {
                    val type = context.contentResolver.getType(uri) ?: "image/jpeg"
                    require(type in setOf("image/jpeg", "image/png", "image/gif", "image/bmp")) { "Scegli una foto JPEG, PNG, GIF o BMP." }
                    val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (output.size() <= 8 * 1024 * 1024) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray() } ?: error("Foto non disponibile")
                    require(bytes.size in 1..8 * 1024 * 1024) { "La foto deve essere entro 8 MB." }
                    bytes to type
                }
                photo = result.first; mime = result.second
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { photoError = failure.message }
            finally { reading = false }
        }
    }
    LaunchedEffect(state.messages.lastOrNull()?.id) {
        // reverseLayout keeps the newest bubble at the bottom without interrupting older-history reading.
        if (list.firstVisibleItemIndex < 2) list.animateScrollToItem(0)
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list, reverseLayout = true,
            contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.messages.asReversed(), key = { it.id }) { message ->
                MessageBubble(message, conversation.id, message.senderId == state.userId) { reply = message }
            }
            if (state.moreHistory) item { OutlinedButton(enabled = !state.busy, onClick = { scope.launch { state.older() } }) { Text("Messaggi precedenti") } }
            if (!state.busy && state.messages.isEmpty()) item { Text("Scrivi il primo messaggio 👋", Modifier.padding(20.dp)) }
        }
        state.failedDraft?.let { TextButton(enabled = !state.sending, onClick = { scope.launch { state.retry() } }) { Text("Riprova invio") } }
        photoError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Surface(shape = RoundedCornerShape(24.dp), color = GlassFill, border = BorderStroke(1.dp, GlassBorder)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                reply?.let { message -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Rispondi: ${message.body.ifBlank { "📷 Foto" }}", Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    TextButton(onClick = { reply = null }) { Text("×") }
                } }
                if (photo != null) Row { Text("📷 Foto pronta", Modifier.weight(1f)); TextButton(onClick = { photo = null }) { Text("Rimuovi") } }
                OutlinedTextField(value = text, onValueChange = { if (it.length <= 4000) text = it }, modifier = Modifier.fillMaxWidth(),
                    enabled = !state.sending && state.failedDraft == null, placeholder = { Text("Scrivi un messaggio…") }, maxLines = 4, shape = RoundedCornerShape(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(enabled = !state.sending && !reading && state.failedDraft == null, onClick = { picker.launch("image/*") }) { Text(if (reading) "Lettura…" else "＋ Foto") }
                    Button(enabled = !state.sending && !reading && state.failedDraft == null && (text.isNotBlank() || photo != null), onClick = {
                        val body = text; val image = photo; val replyId = reply?.id
                        scope.launch {
                            state.send(body, replyId, image, mime)
                            // The retry draft owns the same UUID and attachment after an ambiguous network failure.
                            text = ""; photo = null; reply = null
                        }
                    }) { Text(if (state.sending) "Invio…" else "Invia ↑") }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, conversation: Long, mine: Boolean, onReply: () -> Unit) {
    val context = LocalContext.current
    var image by remember(message.id) { mutableStateOf<ByteArray?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Surface(Modifier.widthIn(max = 300.dp).animateItemForChat(), shape = RoundedCornerShape(22.dp),
            color = if (mine) MaterialTheme.colorScheme.primaryContainer else GlassFill, border = BorderStroke(1.dp, GlassBorder)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                message.replyTo?.let { Text("↪ ${it.body}", maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = Stone400) }
                if (message.body.isNotBlank()) Text(message.body)
                if (message.hasPhoto) {
                    if (image == null) TextButton(enabled = !loading, onClick = {
                        loading = true
                        scope.launch {
                            try { image = ChatRepository.photo(conversation, message.id); error = null }
                            catch (cancelled: CancellationException) { throw cancelled }
                            catch (_: Exception) { error = "Foto non disponibile. Riprova." }
                            finally { loading = false }
                        }
                    }) { Text(if (loading) "Caricamento…" else "📷 Mostra foto") }
                    else AsyncImage(ImageRequest.Builder(context).data(image).memoryCachePolicy(CachePolicy.DISABLED)
                        .diskCachePolicy(CachePolicy.DISABLED).build(), "Foto allegata", Modifier.fillMaxWidth().heightIn(max = 240.dp))
                    error?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(timeLabel(message.sentAt), style = MaterialTheme.typography.labelSmall, color = Stone400, modifier = Modifier.weight(1f))
                    TextButton(onClick = onReply, contentPadding = PaddingValues(0.dp)) { Text("Rispondi", style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }
}

@Composable
private fun Modifier.animateItemForChat(): Modifier {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) { alpha.animateTo(1f, tween(180)) }
    return graphicsLayer { this.alpha = alpha.value }
}

private fun timeLabel(value: String?): String = runCatching {
    DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault()).format(Instant.parse(value))
}.getOrDefault("")
