package com.example.application.screens.organizer

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

internal const val MAX_PHOTO_BYTES = 10 * 1024 * 1024
internal class PhotoTooLargeException : IOException()

/** Enforces the server limit even when a document provider cannot report the file size. */
internal fun readPhotoBytes(input: InputStream): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    var count = input.read(buffer)
    while (count != -1) {
        if (output.size() + count > MAX_PHOTO_BYTES) throw PhotoTooLargeException()
        output.write(buffer, 0, count)
        count = input.read(buffer)
    }
    return output.toByteArray()
}
