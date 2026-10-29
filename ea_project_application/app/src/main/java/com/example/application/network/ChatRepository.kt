package com.example.application.network

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

internal data class Conversation(val id: Long, val otherUserId: Long, val name: String, val role: String,
    val preview: String?, val updatedAt: String?, val unread: Long)
internal data class ChatReply(val id: Long, val senderId: Long, val body: String)
internal data class ChatMessage(val id: Long, val senderId: Long, val clientId: String, val body: String,
    val sentAt: String, val replyTo: ChatReply?, val hasPhoto: Boolean)
internal data class ChatPhoto(val data: String)
internal class ChatStreamException(val retrySeconds: Int) : IOException("Connessione chat interrotta")

internal object ChatRepository {
    private fun conversation(id: Long) = "/api/chat/conversations/$id"
    suspend fun inbox(page: Int = 0) = ApiService.get("/api/chat/conversations?page=$page", Array<Conversation>::class.java).requireData().toList()
    suspend fun unread() = ApiService.get("/api/chat/unread-count", Long::class.javaObjectType).requireData()
    suspend fun open(userId: Long, organizer: Boolean) = ApiService.post("/api/chat/conversations",
        mapOf((if (organizer) "organizerId" else "travelerId") to userId), Conversation::class.java).requireData()
    suspend fun history(id: Long, before: Long? = null) = ApiService.get(conversation(id) + "/messages" +
        (before?.let { "?before=$it" } ?: ""), Array<ChatMessage>::class.java).requireData().toList()
    suspend fun send(id: Long, clientId: String, body: String, reply: Long?, photo: ByteArray?, mime: String): ChatMessage {
        return if (photo == null) ApiService.post(conversation(id) + "/messages",
            mapOf("clientId" to clientId, "body" to body, "replyToId" to reply), ChatMessage::class.java).requireData()
        else ApiService.uploadPhoto(conversation(id) + "/photos", "file", photo, mime, ChatMessage::class.java,
            buildMap { put("clientId", clientId); put("body", body); reply?.let { put("replyToId", it.toString()) } }).requireData()
    }
    suspend fun read(id: Long, through: Long) = ApiService.post(conversation(id) + "/read", mapOf("throughId" to through), Unit::class.java).requireData()
    suspend fun readAll(id: Long) = ApiService.post(conversation(id) + "/read-all", null, Unit::class.java).requireData()
    suspend fun delete(id: Long) = ApiService.delete(conversation(id), Unit::class.java).requireData()
    suspend fun photo(id: Long, message: Long): ByteArray = withContext(Dispatchers.IO) {
        Base64.decode(ApiService.get(conversation(id) + "/messages/$message/photo", ChatPhoto::class.java).requireData().data, Base64.DEFAULT)
    }

    // One authenticated stream per foreground session. Cancellation closes the socket immediately.
    fun events() = callbackFlow<Long> {
        val call = ApiService.eventCall("/api/chat/events")
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { close(e) }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        close(ChatStreamException(it.header("Retry-After")?.toIntOrNull()?.coerceIn(3, 300) ?: 5)); return
                    }
                    try {
                        val parser = ChatEventParser()
                        it.body?.source()?.let { source ->
                            while (!call.isCanceled()) {
                                val line = source.readUtf8Line() ?: break
                                parser.line(line)?.let { event -> trySend(event) }
                            }
                        }
                        close()
                    } catch (e: IOException) { close(e) }
                }
            }
        })
        awaitClose { call.cancel() }
    }
}

internal class ChatEventParser {
    private var event = ""
    private var data = ""
    fun line(line: String): Long? {
        if (line.isEmpty()) {
            val result = when (event) { "ready" -> 0L; "changed" -> data.toLongOrNull(); else -> null }
            event = ""; data = ""
            return result
        }
        if (line.startsWith("event:")) event = line.substringAfter(':').trim()
        if (line.startsWith("data:")) data = line.substringAfter(':').trim()
        return null
    }
}
