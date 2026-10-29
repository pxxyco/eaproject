package com.example.application.screens.organizer

import com.example.application.screens.common.readPhotoBytes
import com.example.application.screens.common.PhotoTooLargeException
import com.example.application.screens.common.MAX_PHOTO_BYTES

import org.junit.Assert.*
import org.junit.Test
import java.io.InputStream

class PhotoUploadDataTest {
    private fun stream(size: Int) = object : InputStream() {
        var remaining = size
        override fun read(): Int = if (remaining-- > 0) 1 else -1
        override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
            if (remaining <= 0) return -1
            val count = minOf(length, remaining)
            java.util.Arrays.fill(bytes, offset, offset + count, 1.toByte())
            remaining -= count
            return count
        }
    }
    @Test fun acceptsImageAtServerLimit() {
        assertEquals(MAX_PHOTO_BYTES, stream(MAX_PHOTO_BYTES).use { readPhotoBytes(it) }.size)
    }
    @Test fun rejectsOversizedImageWithoutReadingUnboundedInput() {
        assertThrows(PhotoTooLargeException::class.java) { stream(MAX_PHOTO_BYTES + 1).use { readPhotoBytes(it) } }
    }
}
