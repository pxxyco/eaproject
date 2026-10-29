package com.example.application.screens.common

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

internal const val MAX_PHOTO_BYTES = 10 * 1024 * 1024
internal class PhotoTooLargeException : IOException()

internal fun readPhotoBytes(input: InputStream, maxBytes: Int = MAX_PHOTO_BYTES): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    var count = input.read(buffer)
    while (count != -1) {
        if (output.size() + count > maxBytes) throw PhotoTooLargeException()
        output.write(buffer, 0, count)
        count = input.read(buffer)
    }
    return output.toByteArray()
}
