package com.example.application.device

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

internal object NotificationChannels {
    const val BOOKINGS = "bookings"
    const val MESSAGES = "messages"
    fun create(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(
            NotificationChannel(BOOKINGS, "Prenotazioni", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(MESSAGES, "Messaggi", NotificationManager.IMPORTANCE_DEFAULT)))
    }
    fun allowed(context: Context, channel: String): Boolean {
        val permission = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context,
            Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return permission && NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            context.getSystemService(NotificationManager::class.java).getNotificationChannel(channel)?.importance?.let { it != NotificationManager.IMPORTANCE_NONE } == true
    }
    fun settings(context: Context, channel: String? = null) {
        val intent = Intent(if (channel == null) Settings.ACTION_APP_NOTIFICATION_SETTINGS else Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        channel?.let { intent.putExtra(Settings.EXTRA_CHANNEL_ID, it) }
        context.startActivity(intent)
    }
}

internal class NotificationAccess {
    var revision by mutableIntStateOf(0)
    var message by mutableStateOf<String?>(null)
    lateinit var request: (String, () -> Unit) -> Unit
}

@Composable
internal fun rememberNotificationAccess(): NotificationAccess {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val access = remember { NotificationAccess() }
    var pending by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        access.revision++
        pending?.let { (channel, action) ->
            if (granted && NotificationChannels.allowed(context, channel)) { access.message = null; action() }
            else access.message = "Notifiche non autorizzate. Puoi abilitarle nelle impostazioni del telefono."
        }
        pending = null
    }
    DisposableEffect(lifecycle) {
        NotificationChannels.create(context)
        access.revision++
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) access.revision++ }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    access.request = { channel, action ->
        NotificationChannels.create(context)
        access.revision++
        when {
            NotificationChannels.allowed(context, channel) -> { access.message = null; action() }
            Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED -> {
                pending = channel to action
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            else -> { access.message = "Le notifiche sono bloccate nelle impostazioni del telefono."; NotificationChannels.settings(context, channel) }
        }
    }
    return access
}
