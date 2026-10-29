package com.example.application.screens.account

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.provider.Settings
import android.util.Size
import android.view.Surface as AndroidSurface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** CameraX captures to memory; no gallery item or temporary image file is created. */
@Composable
internal fun ProfileCameraDialog(onDismiss: () -> Unit, onPhoto: (ByteArray) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    fun granted() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    var authorized by remember { mutableStateOf(granted()) }
    var requested by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { authorized = it }
    val cameraExists = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    LaunchedEffect(Unit) {
        if (cameraExists && !authorized && !requested) { requested = true; permission.launch(Manifest.permission.CAMERA) }
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) authorized = granted() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    if (!cameraExists || !authorized) {
        AlertDialog(onDismissRequest = onDismiss,
            title = { Text("Foto profilo") },
            text = { Text(if (!cameraExists) "Questo dispositivo non dispone di una fotocamera. Puoi scegliere una foto dalla galleria."
                else "Consenti l’uso della fotocamera per scattare la foto profilo. Puoi anche continuare scegliendo una foto dalla galleria.") },
            confirmButton = { if (cameraExists) TextButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
            }) { Text("Apri impostazioni") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } })
        return
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            ProfileCameraPreview(onDismiss, onPhoto)
        }
    }
}

@Composable
private fun ProfileCameraPreview(onDismiss: () -> Unit, onPhoto: (ByteArray) -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val view = remember { PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE } }
    var front by rememberSaveable { mutableStateOf(true) }
    var capture by remember { mutableStateOf<ImageCapture?>(null) }
    var busy by remember { mutableStateOf(false) }
    var canSwitch by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val deliver by rememberUpdatedState(onPhoto)
    val active = remember { AtomicBoolean(true) }
    val executor = remember { Executors.newSingleThreadExecutor { Thread(it, "profile-photo") } }
    DisposableEffect(Unit) { onDispose { active.set(false); executor.shutdown() } }
    DisposableEffect(owner, view, front) {
        val disposed = AtomicBoolean(false)
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        var preview: Preview? = null
        var photo: ImageCapture? = null
        capture = null
        future.addListener({
            if (!disposed.get()) try {
                val camera = future.get(); provider = camera
                val hasFront = camera.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                val hasBack = camera.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                canSwitch = hasFront && hasBack
                val selector = if (front && hasFront || !hasBack) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                val p = Preview.Builder().build().apply { setSurfaceProvider(view.surfaceProvider) }; preview = p
                val c = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(
                        ResolutionStrategy(Size(1280, 1280), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)).build())
                    .build(); photo = c
                camera.bindToLifecycle(owner, selector, p, c)
                capture = c; error = null
            } catch (_: Exception) { error = "Fotocamera non disponibile. Chiudi e riprova oppure scegli una foto dalla galleria." }
        }, ContextCompat.getMainExecutor(context))
        onDispose { disposed.set(true); preview?.let { provider?.unbind(it) }; photo?.let { provider?.unbind(it) } }
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("La tua foto profilo", style = MaterialTheme.typography.titleLarge)
        Text("Scatta la foto, poi conferma l’anteprima nel profilo.", style = MaterialTheme.typography.bodyMedium)
        AndroidView(factory = { view }, modifier = Modifier.fillMaxWidth().weight(1f))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(enabled = capture != null && !busy, onClick = {
            val camera = capture ?: return@Button
            busy = true; error = null
            camera.targetRotation = view.display?.rotation ?: AndroidSurface.ROTATION_0
            try { camera.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    if (!active.get()) { image.close(); return }
                    var original: Bitmap? = null; var oriented: Bitmap? = null
                    try {
                        val source = image.toBitmap(); original = source
                        val scale = minOf(1f, 1600f / maxOf(source.width, source.height))
                        val matrix = Matrix().apply { postRotate(image.imageInfo.rotationDegrees.toFloat()); postScale(scale, scale) }
                        val result = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true); oriented = result
                        val bytes = ByteArrayOutputStream().use { stream ->
                            check(result.compress(Bitmap.CompressFormat.JPEG, 90, stream))
                            stream.toByteArray()
                        }
                        check(bytes.size <= 5 * 1024 * 1024)
                        ContextCompat.getMainExecutor(context).execute { if (active.get()) { busy = false; deliver(bytes) } }
                    } catch (_: Exception) {
                        ContextCompat.getMainExecutor(context).execute { if (active.get()) { busy = false; error = "Impossibile elaborare la foto. Riprova." } }
                    } finally {
                        image.close()
                        if (oriented !== original) oriented?.recycle()
                        original?.recycle()
                    }
                }
                override fun onError(exception: ImageCaptureException) {
                    ContextCompat.getMainExecutor(context).execute { if (active.get()) { busy = false; error = "Scatto non riuscito. Riprova." } }
                }
            }) } catch (_: Exception) { busy = false; error = "Fotocamera non pronta. Riprova." }
        }, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Elaborazione…" else "Scatta foto") }
        if (canSwitch) OutlinedButton(onClick = { front = !front }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Cambia fotocamera") }
        OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Annulla") }
    }
}
