package com.example.application.network

import com.example.application.screens.common.ScreenLoadingHost
import org.junit.Assert.*
import org.junit.Test

class ScreenLoadingHostTest {
    @Test fun disposingAnOldPageCannotClearANewerPagesLoading() {
        val host = ScreenLoadingHost()
        val first = Any(); val second = Any()
        host.show(first, "Prima pagina")
        host.show(second, "Seconda pagina")
        host.clear(first)
        assertEquals("Seconda pagina", host.message)
        host.clear(second)
        assertNull(host.message)
    }
}
