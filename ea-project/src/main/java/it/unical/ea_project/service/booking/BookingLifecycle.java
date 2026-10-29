package it.unical.ea_project.service.booking;

import it.unical.ea_project.domain.booking.Booking;
import it.unical.ea_project.domain.activity.Activity;
import it.unical.ea_project.domain.trip.Trip;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;

public final class BookingLifecycle {
    private BookingLifecycle() {
    }

    public static LocalDateTime end(Booking b) {
        if (b.getTrip() != null) {
            var trip = b.getTrip();
            var date = trip.getEndDate() != null ? trip.getEndDate() : trip.getStartDate();
            return date == null ? null : date.atTime(LocalTime.MAX);
        }
        var activity = b.getActivity();
        if (activity == null)
            return null;
        if (activity.getEndDate() != null)
            return activity.getEndDate().toLocalDate().atTime(LocalTime.MAX);
        if (activity.getStartDate() == null)
            return null;
        var duration = activity.getDurationMinutes();
        return activity.getStartDate().plusMinutes(duration == null || duration < 1 ? 60 : duration)
                .toLocalDate().atTime(LocalTime.MAX);
    }

    /**
     * Booking, payment and cancellation remain available through the entire
     * starting day.
     */
    public static boolean bookingClosed(Booking b, LocalDateTime now) {
        LocalDate day = b.getTrip() != null ? b.getTrip().getStartDate()
                : b.getActivity() == null || b.getActivity().getStartDate() == null ? null
                        : b.getActivity().getStartDate().toLocalDate();
        return day == null || day.isBefore(now.toLocalDate());
    }

    public static boolean cancelled(Booking b) {
        return b.getBookingStatus() == Booking.BookingStatus.CANCELLED
                || b.getTrip() != null && b.getTrip().getStatus() == Trip.TripStatus.CANCELLED
                || b.getActivity() != null && b.getActivity().getStatus() == Activity.Status.CANCELLED;
    }

    public static boolean ended(Booking b, LocalDateTime now) {
        return b.getBookingStatus() == Booking.BookingStatus.COMPLETED
                || b.getTrip() != null && b.getTrip().getStatus() == Trip.TripStatus.COMPLETED
                || b.getActivity() != null && b.getActivity().getStatus() == Activity.Status.COMPLETED
                || end(b) != null && end(b).toLocalDate().isBefore(now.toLocalDate());
    }

    public static boolean reviewable(Booking b, LocalDateTime now) {
        return b.getDeletedAt() == null && !cancelled(b)
                && (b.getBookingStatus() == Booking.BookingStatus.CONFIRMED
                        || b.getBookingStatus() == Booking.BookingStatus.COMPLETED);
    }
}
