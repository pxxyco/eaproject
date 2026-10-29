package com.example.application.service.media

import com.example.application.screens.common.readPhotoBytes
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import com.example.application.R
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.util.concurrent.TimeUnit

internal object WallpaperService {
    private val mutex = Mutex()
    private var day: LocalDate? = null
    @Volatile private var image: Bitmap? = null
    fun readyImage(): Bitmap? = image
    private val client = OkHttpClient.Builder().connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS).callTimeout(7, TimeUnit.SECONDS).build()

    suspend fun daily(context: Context): Bitmap = mutex.withLock {
        val today = LocalDate.now()
        image?.takeIf { day == today }?.let { return@withLock it }
        val selected = withContext(Dispatchers.IO) {
            val directory = File(context.cacheDir, "wallpapers").apply { mkdirs() }
            val target = File(directory, "bing-$today.jpg")
            var bitmap = decode(target)
            if (bitmap == null) {
                repeat(3) {
                    if (bitmap == null) {
                        try {
                            val body = fetch("https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=it-IT", 64 * 1024)
                            val relative = JsonParser.parseString(body.toString(Charsets.UTF_8)).asJsonObject
                                .getAsJsonArray("images")[0].asJsonObject.get("url").asString
                            if (!relative.startsWith("/") || relative.startsWith("//")) throw IOException("URL non valido")
                            val temporary = File(directory, "download.part")
                            try {
                                temporary.writeBytes(fetch("https://www.bing.com$relative", 8 * 1024 * 1024))
                                bitmap = decode(temporary) ?: throw IOException("Immagine non valida")
                                if (!temporary.renameTo(target)) temporary.copyTo(target, overwrite = true)
                            } finally { temporary.delete() }
                        } catch (_: IOException) {}
                        catch (_: RuntimeException) {}
                    }
                }
            }
            if (bitmap != null) directory.listFiles()?.filter { it != target }?.forEach { it.delete() }
            val original = bitmap ?: BitmapFactory.decodeResource(context.resources, R.drawable.wallpaper_fallback)
            soften(original)
        }
        day = today
        image = selected
        selected
    }

    private fun fetch(url: String, limit: Int): ByteArray = client.newCall(Request.Builder().url(url).build()).execute().use { response ->
        if (!response.isSuccessful) throw IOException("Bing non disponibile")
        val bytes = response.body?.byteStream()?.use { readPhotoBytes(it, limit) } ?: throw IOException("Risposta vuota")
        if (bytes.isEmpty() || bytes.size > limit) throw IOException("Risposta troppo grande")
        bytes
    }

    private fun decode(file: File): Bitmap? {
        if (!file.isFile || file.length() !in 1..8L * 1024 * 1024) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0 || bounds.outWidth.toLong() * bounds.outHeight > 16_000_000) return null
        return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply {
            inSampleSize = Integer.highestOneBit((maxOf(bounds.outWidth, bounds.outHeight) / 1280).coerceAtLeast(1))
        })
    }

    private fun soften(source: Bitmap): Bitmap {
        val width = minOf(720, source.width)
        val height = (source.height.toLong() * width / source.width).toInt().coerceAtLeast(1)
        val small = source.scale(width, height, true)
        val pixels = IntArray(width * height)
        small.getPixels(pixels, 0, width, 0, 0, width, height)
        val scratch = IntArray(pixels.size)
        val radius = 5
        fun pass(input: IntArray, output: IntArray, horizontal: Boolean) {
            val outer = if (horizontal) height else width
            val inner = if (horizontal) width else height
            fun index(a: Int, b: Int) = if (horizontal) a * width + b else b * width + a
            for (a in 0 until outer) {
                var red = 0; var green = 0; var blue = 0
                fun add(b: Int, sign: Int) {
                    val color = input[index(a, b.coerceIn(0, inner - 1))]
                    red += ((color ushr 16) and 255) * sign
                    green += ((color ushr 8) and 255) * sign
                    blue += (color and 255) * sign
                }
                for (b in -radius..radius) add(b, 1)
                val count = radius * 2 + 1
                for (b in 0 until inner) {
                    output[index(a, b)] = (255 shl 24) or (red / count shl 16) or (green / count shl 8) or (blue / count)
                    add(b - radius, -1); add(b + radius + 1, 1)
                }
            }
        }
        repeat(1) { pass(pixels, scratch, true); pass(scratch, pixels, false) }
        val result = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        if (small !== source) small.recycle()
        source.recycle()
        return result
    }
}
