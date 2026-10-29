package it.unical.ea_project.service.booking;

import it.unical.ea_project.domain.activity.Activity;
import it.unical.ea_project.domain.booking.Booking;
import it.unical.ea_project.domain.trip.Trip;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BookingLifecycleTest {
    private final LocalDate today = LocalDate.of(2026, 10, 29);

    private Booking activity() {
        Activity a = new Activity();
        a.setStartDate(today.atStartOfDay());
        a.setEndDate(today.atTime(1, 0));
        a.setStatus(Activity.Status.PUBLISHED);
        Booking b = new Booking();
        b.setActivity(a);
        b.setBookingStatus(Booking.BookingStatus.CONFIRMED);
        return b;
    }

    @Test
    void anActivityRemainsOpenThrough2359AndExpiresAtMidnight() {
        Booking b = activity();
        assertFalse(BookingLifecycle.bookingClosed(b, today.atTime(LocalTime.MAX)));
        assertFalse(BookingLifecycle.ended(b, today.atTime(LocalTime.MAX)));
        assertTrue(BookingLifecycle.bookingClosed(b, today.plusDays(1).atStartOfDay()));
        assertTrue(BookingLifecycle.ended(b, today.plusDays(1).atStartOfDay()));
        assertEquals(today.atTime(1, 0), b.getActivity().getEndDate(), "Program times are preserved");
    }

    @Test
    void multiDayTripKeepsItsFinalDateAndBookingClosesAfterItsStartingDay() {
        Trip t = new Trip();
        t.setStartDate(today);
        t.setEndDate(today.plusDays(2));
        t.setStatus(Trip.TripStatus.PUBLISHED);
        Booking b = new Booking();
        b.setTrip(t);
        b.setBookingStatus(Booking.BookingStatus.CONFIRMED);
        assertFalse(BookingLifecycle.bookingClosed(b, today.atTime(LocalTime.MAX)));
        assertTrue(BookingLifecycle.bookingClosed(b, today.plusDays(1).atStartOfDay()));
        assertFalse(BookingLifecycle.ended(b, today.plusDays(2).atTime(LocalTime.MAX)));
        assertTrue(BookingLifecycle.ended(b, today.plusDays(3).atStartOfDay()));
    }

    @Test
    void durationFallbackHandlesAnOvernightActivityAndCancellationIsStillExplicit() {
        Booking b = activity();
        b.getActivity().setEndDate(null);
        b.getActivity().setStartDate(today.atTime(23, 0));
        b.getActivity().setDurationMinutes(120);
        assertEquals(today.plusDays(1), BookingLifecycle.end(b).toLocalDate());
        b.setBookingStatus(Booking.BookingStatus.CANCELLED);
        assertTrue(BookingLifecycle.cancelled(b));
        assertFalse(BookingLifecycle.reviewable(b, today.atTime(23, 30)));
    }
}
