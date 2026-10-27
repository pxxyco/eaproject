package com.example.application.screens.account

import android.os.SystemClock
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.application.device.CardScanSession
import com.example.application.device.ScannedCard
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Only Preview + ImageAnalysis are bound. There is no ImageCapture, file URI, bitmap cache or upload path. */
@Composable
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
internal fun CameraCardPreview(modifier: Modifier, session: CardScanSession,
    onProgress: (ScannedCard?) -> Unit, onComplete: (ScannedCard) -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val view = remember { PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE } }
    var error by remember { mutableStateOf<String?>(null) }
    val progress by rememberUpdatedState(onProgress)
    val complete by rememberUpdatedState(onComplete)
    DisposableEffect(owner, view) {
        val disposed = AtomicBoolean(false)
        val delivered = AtomicBoolean(false)
        val inFlight = AtomicBoolean(false)
        val executor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "card-ocr") }
        val main = ContextCompat.getMainExecutor(context)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        var preview: Preview? = null
        var analysis: ImageAnalysis? = null
        var lastFrame = 0L
        future.addListener({
            if (!disposed.get()) try {
                provider = future.get()
                preview = Preview.Builder().build().apply { setSurfaceProvider(view.surfaceProvider) }
                @Suppress("DEPRECATION")
                val reader = ImageAnalysis.Builder().setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analysis = reader
                reader.setAnalyzer(executor) { frame ->
                    val now = SystemClock.elapsedRealtime()
                    val image = frame.image
                    if (disposed.get() || delivered.get() || !owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) ||
                        image == null || now - lastFrame < 400 || !inFlight.compareAndSet(false, true)) {
                        frame.close()
                    } else {
                        lastFrame = now
                        try {
                            recognizer.process(InputImage.fromMediaImage(image, frame.imageInfo.rotationDegrees))
                                .addOnSuccessListener(main) { text ->
                                    if (!disposed.get() && !delivered.get() && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                        val result = session.accept(text.textBlocks.flatMap { block -> block.lines.map { it.text.take(128) } }.take(80))
                                        progress(session.snapshot())
                                        if (result != null && delivered.compareAndSet(false, true)) { reader.clearAnalyzer(); complete(result) }
                                    }
                                }
                                .addOnFailureListener(main) { if (!disposed.get()) error = "Lettura non disponibile. Puoi annullare e compilare i dati a mano." }
                                .addOnCompleteListener { frame.close(); inFlight.set(false) }
                        } catch (_: RuntimeException) { frame.close(); inFlight.set(false) }
                    }
                }
                val selector = if (provider!!.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                provider!!.bindToLifecycle(owner, selector, preview!!, reader)
            } catch (_: Exception) { error = "Fotocamera non disponibile. Puoi tornare al modulo e inserire i dati a mano." }
        }, main)
        onDispose {
            disposed.set(true)
            analysis?.clearAnalyzer()
            listOfNotNull(preview, analysis).takeIf { it.isNotEmpty() }?.let { provider?.unbind(*it.toTypedArray()) }
            recognizer.close()
            executor.shutdown()
            session.clear()
        }
    }
    Box(modifier) {
        AndroidView(factory = { view }, modifier = Modifier.matchParentSize())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
