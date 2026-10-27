package com.example.application.device

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract

/** System pickers grant access to selected data; no broad storage/calendar permission is needed. */
internal fun launchPhotoPicker(launch: () -> Unit): String? = try { launch(); null }
catch (_: ActivityNotFoundException) { "Nessun selettore foto disponibile sul dispositivo." }
catch (_: SecurityException) { "Il selettore foto non è accessibile. Controlla le impostazioni del dispositivo." }

internal fun calendarIntent(event: CalendarEvent): Intent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
    .putExtra(CalendarContract.Events.TITLE, event.title)
    .putExtra(CalendarContract.Events.EVENT_LOCATION, event.location)
    .putExtra(CalendarContract.Events.DESCRIPTION, "Esperienza salvata da EA Project")
    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.start)
    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.end)
    .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, event.allDay)

internal fun Context.openCalendar(event: CalendarEvent): String? = try { startActivity(calendarIntent(event)); null }
catch (_: ActivityNotFoundException) { "Non è installata un’app calendario compatibile." }
catch (_: SecurityException) { "Impossibile aprire il calendario. Controlla le impostazioni del dispositivo." }
