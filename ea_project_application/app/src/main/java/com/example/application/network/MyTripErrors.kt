package com.example.application.network

internal fun myTripHttpError(code: Int): String = when (code) {
    401 -> "Sessione scaduta: accedi nuovamente."
    403 -> "Non hai i permessi per questa operazione."
    404 -> "L’elemento non è più disponibile."
    400, 409 -> "Operazione non consentita nello stato attuale dell’elemento."
    else -> "Servizio temporaneamente non disponibile. Riprova."
}
