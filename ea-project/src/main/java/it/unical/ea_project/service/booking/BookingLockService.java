package it.unical.ea_project.service.booking;

import it.unical.ea_project.domain.booking.Booking;
import it.unical.ea_project.repository.booking.BookingRepository;
import it.unical.ea_project.repository.trip.TripRepository;
import it.unical.ea_project.repository.activity.ActivityRepository;
import jakarta.persistence.EntityManager;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class BookingLockService {
    private final BookingRepository bookings;
    private final TripRepository trips;
    private final ActivityRepository activities;
    private final EntityManager entities;

    @Transactional(propagation = Propagation.MANDATORY)
    public Booking owned(Long id, Long user) {
        Booking snapshot = bookings.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prenotazione non disponibile."));
        if (snapshot.getUser() == null || !Objects.equals(snapshot.getUser().getId(), user))
            throw new AccessDeniedException("Puoi gestire soltanto le tue prenotazioni.");
        if (snapshot.getDeletedAt() != null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prenotazione non disponibile.");
        if (snapshot.getBookingStatus() == Booking.BookingStatus.CANCELLED) return snapshot;
        Object offer;
        if (snapshot.getTrip() != null) offer = trips.findActiveForUpdate(snapshot.getTrip().getTripId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Viaggio non disponibile."));
        else offer = activities.findActiveForUpdate(snapshot.getActivity().getActivityId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Attività non disponibile."));
        entities.refresh(offer);
        Booking locked = bookings.findByBookingIdForUpdate(id).orElseThrow();
        entities.refresh(locked);
        return locked;
    }
}
