package it.unical.ea_project_javafx.controller.organizer;

import it.unical.ea_project_javafx.dto.booking.BookingDTO;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OfferScheduleTest {
    private final LocalDateTime now = LocalDateTime.of(2027,6,1,12,0);
    @Test void sameDayActivityUsesEnteredHours() {
        var date = now.toLocalDate();
        var period = OfferSchedule.activityPeriod(date,"14:00",date,"16:00");
        assertEquals(date.atTime(14,0),period[0]);
        assertEquals(date.atTime(16,0),period[1]);
        assertThrows(IllegalArgumentException.class, () -> OfferSchedule.activityPeriod(date,"16:00",date,"14:00"));
        assertThrows(IllegalArgumentException.class, () -> OfferSchedule.activityPeriod(date,"14:00",date,"14:00"));
    }
    @Test void overnightActivityUsesExplicitNextDate() {
        var date = now.toLocalDate();
        assertEquals(date.plusDays(1).atTime(1,0), OfferSchedule.activityPeriod(date,"23:00",date.plusDays(1),"01:00")[1]);
    }
    @Test void optionalStageTimesRemainOptionalButInvalidIntervalsAreRejected() {
        assertNull(OfferSchedule.time("", false));
        assertThrows(IllegalArgumentException.class, () -> OfferSchedule.time("25:00", false));
        assertThrows(IllegalArgumentException.class, () -> OfferSchedule.time("", true));
        assertThrows(IllegalArgumentException.class, () -> OfferSchedule.stageTimes(LocalTime.of(18,0),LocalTime.of(10,0)));
        assertDoesNotThrow(() -> OfferSchedule.stageTimes(null,null));
    }
    @Test void todaysTripIsUpcomingWhileStartedActivityIsNot() {
        assertTrue(OfferSchedule.upcoming(true, now.toLocalDate().atStartOfDay(), now));
        assertFalse(OfferSchedule.upcoming(true, now.minusDays(1), now));
        assertFalse(OfferSchedule.upcoming(false, now.minusHours(1), now));
        assertTrue(OfferSchedule.upcoming(false, now.plusHours(1), now));
    }
    private BookingDTO booking(String status, String end, int seats) {
        BookingDTO b = new BookingDTO(); b.setBookingStatus(status); b.setBookingType("ACTIVITY");
        b.setItemDate("2027-05-31T12:00:00"); b.setItemEndDate(end); b.setSeats(seats); return b;
    }
    @Test void activeSeatsExcludesCompletedCancelledAndEndedExperiences() {
        var active = booking("CONFIRMED","2027-06-02T12:00:00",3);
        var completed = booking("COMPLETED","2027-06-02T12:00:00",4);
        var cancelled = booking("CANCELLED","2027-06-02T12:00:00",5);
        var ended = booking("CONFIRMED","2027-05-31T16:00:00",6);
        assertEquals(3, OfferSchedule.activeSeats(List.of(active,completed,cancelled,ended),now));
    }
}
