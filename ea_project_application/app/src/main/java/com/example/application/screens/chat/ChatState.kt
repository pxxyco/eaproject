package com.example.application.screens.chat

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.application.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

internal val LocalChatState = staticCompositionLocalOf<ChatState?> { null }
internal data class ChatDraft(val clientId: String, val body: String, val reply: Long?, val photo: ByteArray?, val mime: String)

internal class ChatState(val userId: Long) {
    var inbox by mutableStateOf<List<Conversation>>(emptyList()); private set
    var messages by mutableStateOf<List<ChatMessage>>(emptyList()); private set
    var selected by mutableStateOf<Conversation?>(null); private set
    var unread by mutableLongStateOf(0); private set
    var error by mutableStateOf<String?>(null)
    var connected by mutableStateOf(false)
    var busy by mutableStateOf(false); private set
    var sending by mutableStateOf(false); private set
    var moreInbox by mutableStateOf(false); private set
    var moreHistory by mutableStateOf(false); private set
    var requested by mutableStateOf(false)
    var visible by mutableStateOf(false)
    var failedDraft by mutableStateOf<ChatDraft?>(null); private set
    private var page = 0
    private val readThrough = mutableMapOf<Long, Long>()
    private val mutex = Mutex()

    private suspend fun perform(block: suspend () -> Unit) {
        mutex.withLock {
            busy = true; error = null
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { error = failure.message ?: "Impossibile aggiornare i messaggi." }
            finally { busy = false }
        }
    }
    suspend fun refresh() = perform {
        unread = ChatRepository.unread()
        if (visible) {
            val rows = ChatRepository.inbox()
            inbox = rows; page = 0; moreInbox = rows.size == 30
            selected?.let { current ->
                val latest = ChatRepository.history(current.id)
                messages = mergeMessages(messages, latest)
                markVisible(current.id)
            }
        }
    }
    suspend fun nextPage() = perform {
        val rows = ChatRepository.inbox(page + 1)
        inbox = (inbox + rows).distinctBy { it.id }; page++; moreInbox = rows.size == 30
    }
    suspend fun select(conversation: Conversation) = perform {
        selected = conversation; messages = emptyList(); failedDraft = null
        val rows = ChatRepository.history(conversation.id)
        messages = mergeMessages(emptyList(), rows); moreHistory = rows.size == 50
        markVisible(conversation.id)
    }
    private suspend fun markVisible(id: Long) {
        val through = messages.lastOrNull()?.id ?: return
        if (through > (readThrough[id] ?: 0)) {
            ChatRepository.read(id, through)
            readThrough[id] = through
            unread = ChatRepository.unread()
            inbox = inbox.map { if (it.id == id) it.copy(unread = 0) else it }
        }
    }
    suspend fun older() = perform {
        val id = selected?.id ?: return@perform
        val rows = ChatRepository.history(id, messages.firstOrNull()?.id)
        messages = mergeMessages(messages, rows); moreHistory = rows.size == 50
    }
    suspend fun openOrganizer(id: Long) = open(id, true)
    suspend fun openTraveler(id: Long) = open(id, false)
    private suspend fun open(id: Long, organizer: Boolean) = perform {
        val conversation = ChatRepository.open(id, organizer)
        val rows = ChatRepository.history(conversation.id)
        selected = conversation; messages = mergeMessages(emptyList(), rows)
        moreHistory = rows.size == 50; failedDraft = null
        markVisible(conversation.id)
        requested = true
    }
    fun closeConversation() { selected = null; messages = emptyList(); failedDraft = null; error = null }
    suspend fun readAll(conversation: Conversation) = perform {
        ChatRepository.readAll(conversation.id)
        inbox = inbox.map { if (it.id == conversation.id) it.copy(unread = 0) else it }
        unread = ChatRepository.unread()
    }
    suspend fun delete(conversation: Conversation) = perform {
        ChatRepository.delete(conversation.id)
        inbox = inbox.filterNot { it.id == conversation.id }
        if (selected?.id == conversation.id) closeConversation()
        unread = ChatRepository.unread()
    }
    suspend fun send(body: String, reply: Long?, photo: ByteArray?, mime: String) {
        if (sending || failedDraft != null || (body.isBlank() && photo == null)) return
        deliver(ChatDraft(UUID.randomUUID().toString(), body.trim(), reply, photo, mime))
    }
    suspend fun retry() { failedDraft?.let { deliver(it) } }
    private suspend fun deliver(draft: ChatDraft) {
        if (sending) return
        val id = selected?.id ?: return
        sending = true; error = null
        try {
            val sent = ChatRepository.send(id, draft.clientId, draft.body, draft.reply, draft.photo, draft.mime)
            if (selected?.id == id) messages = mergeMessages(messages, listOf(sent))
            failedDraft = null
            refresh()
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { failedDraft = draft; error = failure.message ?: "Invio non riuscito. Puoi riprovare senza duplicare il messaggio." }
        finally { sending = false }
    }
}

internal fun mergeMessages(old: List<ChatMessage>, latest: List<ChatMessage>) =
    (old + latest).distinctBy { it.id }.sortedBy { it.id }

@OptIn(FlowPreview::class)
@Composable
internal fun ObserveChat(state: ChatState?) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(state, lifecycle) {
        if (state == null) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var retry = 3L
            try {
                while (isActive) {
                    var delaySeconds = retry
                    try {
                        ChatRepository.events().debounce(180).collect {
                            state.connected = true; retry = 3
                            state.refresh()
                        }
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (failure: Exception) { delaySeconds = (failure as? ChatStreamException)?.retrySeconds?.toLong() ?: retry }
                    state.connected = false
                    delay(delaySeconds * 1000)
                    retry = (retry * 2).coerceAtMost(60)
                }
            } finally { state.connected = false }
        }
    }
}
