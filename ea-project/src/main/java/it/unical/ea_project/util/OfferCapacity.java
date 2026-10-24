package it.unical.ea_project.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Capacity is recalculated from persisted availability, never from a client's stale form. */
public final class OfferCapacity {
    private OfferCapacity() {}

    public static int resize(Integer previousCapacity, Integer availableSeats, Integer newCapacity) {
        if (newCapacity == null || newCapacity < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Il numero di posti deve essere positivo.");
        }
        if (previousCapacity == null || availableSeats == null || availableSeats < 0 || availableSeats > previousCapacity) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Disponibilità dei posti incoerente. Aggiorna l'offerta.");
        }
        int booked = previousCapacity - availableSeats;
        if (newCapacity < booked) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "I posti totali non possono essere inferiori ai posti già prenotati.");
        }
        return newCapacity - booked;
    }
}
