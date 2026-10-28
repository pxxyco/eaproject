package it.unical.ea_project_javafx.ui;

import it.unical.ea_project_javafx.dto.booking.BookingDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BookingPresentationTest {
    final LocalDateTime now = LocalDateTime.of(2026, 10, 28, 12, 0);
    BookingDTO booking(String status, boolean ended) {
        BookingDTO b = new BookingDTO(); b.setBookingId(7L); b.setBookingType("ACTIVITY"); b.setBookingStatus(status);
        b.setItemStatus("PUBLISHED"); b.setPrice(BigDecimal.TEN);
        b.setItemDate(now.plusDays(ended ? -1 : 1).toString()); b.setItemEndDate(now.plusDays(ended ? -1 : 1).plusHours(1).toString());
        return b;
    }
    @Test void derivedAndExplicitCompletionHaveOneLabel() {
        assertEquals("Conclusa", BookingPresentation.displayStatus(booking("CONFIRMED", true), now));
        assertEquals("Conclusa", BookingPresentation.displayStatus(booking("COMPLETED", true), now));
    }
    @Test void unpaidPastReservationsAreExpiredAndCannotBeReviewed() {
        BookingDTO b = booking("PENDING", true);
        assertEquals("Scaduta", BookingPresentation.displayStatus(b, now));
        assertFalse(BookingPresentation.canReview(b, now)); assertFalse(BookingPresentation.canCancel(b, now));
    }
    @Test void awaitingPaymentIsDistinctFromConfirmedAndCancelled() {
        BookingDTO b = booking("PENDING", false);
        assertEquals("Da pagare", BookingPresentation.displayStatus(b, now)); assertTrue(BookingPresentation.canCancel(b, now));
        b.setBookingStatus("CONFIRMED"); b.setPaymentStatus("CONFIRMED");
        assertEquals("Confermata", BookingPresentation.displayStatus(b, now));
        b.setBookingStatus("CANCELLED"); assertFalse(BookingPresentation.canReview(b, now));
    }
    @Test void confirmedFutureBookingCanBeReviewedButPendingCannot() {
        assertTrue(BookingPresentation.canReview(booking("CONFIRMED", false), now));
        assertFalse(BookingPresentation.canReview(booking("PENDING", false), now));
        assertFalse(BookingPresentation.canReview(booking("CANCELLED", false), now));
    }

}
