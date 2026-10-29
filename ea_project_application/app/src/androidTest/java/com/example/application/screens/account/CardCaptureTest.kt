package com.example.application.screens.account

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.application.device.CardTextReader
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.YearMonth
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class CardCaptureTest {
    @Test fun bundledOcrReadsASyntheticCardInMemoryWithoutCreatingPhotoFiles() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        fun photos(): Set<String> = listOf(context.cacheDir, context.filesDir).flatMap { root ->
            root.walkTopDown().filter { it.isFile && it.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp") }.map(File::getAbsolutePath).toList()
        }.toSet()
        val before = photos()
        val bitmap = Bitmap.createBitmap(1280, 800, Bitmap.Config.ARGB_8888)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 56f; typeface = Typeface.MONOSPACE }
            listOf("VISA", "4111 1111 1111 1111", "VALID THRU 02/27", "MARIO ROSSI").forEachIndexed { index, text ->
                canvas.drawText(text, 80f, 140f + index * 160f, paint)
            }
            val text = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0)), 30, TimeUnit.SECONDS)
            val result = CardTextReader.read(text.textBlocks.flatMap { it.lines.map { line -> line.text } }, YearMonth.of(2026,10))
            assertEquals("4111111111111111", result.pan)
            assertEquals("MARIO ROSSI", result.holder)
            assertEquals(YearMonth.of(2027,2), result.expiry)
            assertEquals(before, photos())
        } finally { recognizer.close(); bitmap.recycle() }
    }

    @Test fun expiryCursorMappingKeepsFebruary2027InTheCorrectOrder() {
        val result = ExpiryTransformation.filter(AnnotatedString("0227"))
        assertEquals("02/27", result.text.text)
        assertEquals(5, result.offsetMapping.originalToTransformed(4))
        assertEquals(4, result.offsetMapping.transformedToOriginal(5))
        for (offset in 0..4) assertEquals(offset, result.offsetMapping.transformedToOriginal(result.offsetMapping.originalToTransformed(offset)))
    }
}
