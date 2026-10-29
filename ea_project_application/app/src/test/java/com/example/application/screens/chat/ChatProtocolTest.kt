package com.example.application.screens.chat

import com.example.application.network.ChatEventParser
import com.example.application.network.ChatMessage
import org.junit.Assert.*
import org.junit.Test

class ChatProtocolTest {
    @Test fun sseAcceptsReadyAndChangesButIgnoresHeartbeatsAndInvalidIds() {
        val parser = ChatEventParser()
        assertNull(parser.line(":heartbeat")); assertNull(parser.line(""))
        parser.line("event:ready"); parser.line("data:connected")
        assertEquals(0L, parser.line(""))
        parser.line("event:changed"); parser.line("data:42")
        assertEquals(42L, parser.line(""))
        parser.line("event:changed"); parser.line("data:invalid")
        assertNull(parser.line("")); assertNull(parser.line(""))
    }
    @Test fun refreshAndSendAcknowledgementDoNotDuplicateOrReorderHistory() {
        fun message(id: Long) = ChatMessage(id, 1, "client-$id", "Ciao 👋", "2026-10-29T12:00:00Z", null, false)
        val result = mergeMessages(listOf(message(2), message(1)), listOf(message(3), message(2)))
        assertEquals(listOf(1L, 2L, 3L), result.map { it.id })
    }
}
