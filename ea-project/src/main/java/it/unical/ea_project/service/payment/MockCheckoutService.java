package it.unical.ea_project.service.payment;

import it.unical.ea_project.domain.booking.Booking;
import it.unical.ea_project.service.booking.BookingLockService;
import it.unical.ea_project.domain.trip.Trip;
import it.unical.ea_project.domain.activity.Activity;
import it.unical.ea_project.domain.payment.Payment;
import it.unical.ea_project.repository.booking.BookingRepository;
import it.unical.ea_project.repository.payment.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * A mock settlement uses the stored booking price and serializes retries with
 * cancellation.
 */
@Service
public class MockCheckoutService {
    public record Receipt(String title, BigDecimal amount, String reference, LocalDateTime paidAt) {
    }

    private final BookingRepository bookings;
    private final BookingLockService locks;
    private final PaymentRepository payments;

    public MockCheckoutService(BookingRepository bookings, PaymentRepository payments, BookingLockService locks) {
        this.bookings = bookings;
        this.locks = locks;
        this.payments = payments;
    }

    @Transactional
    public Receipt pay(Long bookingId, Long traveler) {
        Booking booking = locks.owned(bookingId, traveler);
        if (!booking.getUser().getId().equals(traveler))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Puoi pagare soltanto le tue prenotazioni.");
        if (booking.getDeletedAt() != null || booking.getBookingStatus() == Booking.BookingStatus.CANCELLED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La prenotazione è stata annullata.");
        var previous = payments.findBookingPayments(bookingId);
        var paid = previous.stream().filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED
                || p.getStatus() == Payment.PaymentStatus.CONFIRMED).findFirst();
        if (paid.isPresent())
            return receipt(paid.get());
        var others = bookings.findForExperience(traveler,
                booking.getTrip() == null ? null : booking.getTrip().getTripId(),
                booking.getActivity() == null ? null : booking.getActivity().getActivityId()).stream()
                .filter(b -> b.getBookingId() != bookingId && b.getBookingStatus() != Booking.BookingStatus.CANCELLED)
                .map(Booking::getBookingId).toList();
        if (!others.isEmpty() && payments.findBookingPaymentsBatch(others).stream()
                .anyMatch(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED || p.getStatus() == Payment.PaymentStatus.CONFIRMED))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hai già una prenotazione pagata per questa esperienza. Gestisci prima le prenotazioni precedenti.");
        LocalDateTime start;
        if (booking.getTrip() != null) {
            var trip = booking.getTrip();
            if (trip.getDeletedAt() != null || trip.getStatus() != Trip.TripStatus.PUBLISHED)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Il viaggio non è più prenotabile.");
            start = trip.getStartDate() == null ? null : trip.getStartDate().atStartOfDay();
        } else {
            var activity = booking.getActivity();
            if (activity.getDeletedAt() != null || activity.getStatus() != Activity.Status.PUBLISHED)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "L'attività non è più prenotabile.");
            start = activity.getStartDate();
        }
        if (start == null || !start.isAfter(LocalDateTime.now())
                || booking.getBookingStatus() == Booking.BookingStatus.COMPLETED)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Non puoi pagare un'esperienza già iniziata o conclusa.");
        if (booking.getPrice() == null || booking.getPrice().signum() < 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Importo della prenotazione non valido.");
        Payment payment = previous.stream().filter(p -> p.getStatus() == Payment.PaymentStatus.PENDING).findFirst()
                .orElseGet(Payment::new);
        payment.setBooking(booking);
        payment.setAmount(booking.getPrice());
        payment.setStatus(Payment.PaymentStatus.CONFIRMED);
        payment.setPaidAt(LocalDateTime.now());
        payment.setTransactionId("MOCK-" + UUID.randomUUID());
        booking.setBookingStatus(Booking.BookingStatus.CONFIRMED);
        return receipt(payments.saveAndFlush(payment));
    }

    private Receipt receipt(Payment payment) {
        Booking b = payment.getBooking();
        return new Receipt(b.getTrip() != null ? b.getTrip().getTitle() : b.getActivity().getTitle(),
                payment.getAmount(), payment.getTransactionId(),
                payment.getPaidAt() == null ? payment.getCreatedAt() : payment.getPaidAt());
    }
}
