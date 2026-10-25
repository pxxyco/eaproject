package com.example.application.screens.favorite

import androidx.compose.runtime.mutableStateListOf
import java.time.LocalDate

enum class BookingStatus(val label: String) {
    UPCOMING("In arrivo"),
    COMPLETED("Concluse"),
    CANCELLED("Annullate")
}

/** Modello solo UI: quando ci sarà il backend verrà mappato da un DTO. */
data class BookedActivity(
    val bookingId: Long,
    val title: String,
    val city: String,
    val category: String,
    val date: LocalDate,
    val seats: Int,
    val totalPrice: Double,
    val imageUrl: String = "",
    val cancelled: Boolean = false
) {
    // Lo stato è derivato: annullata > data passata > in arrivo
    val status: BookingStatus
        get() = when {
            cancelled -> BookingStatus.CANCELLED
            date.isBefore(LocalDate.now()) -> BookingStatus.COMPLETED
            else -> BookingStatus.UPCOMING
        }
}

/** Repository locale in memoria. Da sostituire con le chiamate a ApiService. */
object MyTripRepository {
    private val _bookings = mutableStateListOf<BookedActivity>().apply { addAll(sampleBookings()) }

    val bookings: List<BookedActivity> get() = _bookings

    fun cancel(bookingId: Long) {
        val index = _bookings.indexOfFirst { it.bookingId == bookingId }
        if (index >= 0 && _bookings[index].status == BookingStatus.UPCOMING) {
            _bookings[index] = _bookings[index].copy(cancelled = true)
        }
    }

    private fun sampleBookings(): List<BookedActivity> {
        val today = LocalDate.now()
        return listOf(
            BookedActivity(1, "Trekking nella Sila", "Camigliatello Silano", "Escursione", today.plusDays(5), 2, 60.0),
            BookedActivity(2, "Tour dei Bronzi di Riace", "Reggio Calabria", "Visita", today.plusDays(21), 1, 18.5),
            BookedActivity(3, "Cena degustazione a Tropea", "Tropea", "Ristorazione", today.minusDays(30), 2, 95.0),
            BookedActivity(4, "Weekend in hotel sul mare", "Scilla", "Hotel", today.minusDays(90), 2, 240.0),
            BookedActivity(5, "Transfer aeroporto", "Lamezia Terme", "Trasporto", today.plusDays(10), 3, 45.0, cancelled = true)
        )
    }
}