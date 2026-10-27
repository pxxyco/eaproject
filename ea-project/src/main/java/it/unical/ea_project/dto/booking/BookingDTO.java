package it.unical.ea_project.dto.booking;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingDTO {

    private Long bookingId;

    private Long userId;

    private Long tripId;

    private Long activityId;

    private String participantName;

    private String participantEmail;

    private String itemTitle;

    private String itemDate;

    /**
     * Data/ora di fine dell’esperienza; usata per classificare correttamente lo
     * storico.
     */
    private String itemEndDate;

    /**
     * Stato corrente del viaggio/attività, distinto dallo stato della prenotazione.
     */
    private String itemStatus;

    private String itemLocation;

    private Integer seats;

    // Booking.BookingType: TRIP, ACTIVITY
    private String bookingType;

    // Booking.BookingStatus: PENDING, COMPLETED, CONFIRMED, CANCELLED
    private String bookingStatus;

    private BigDecimal price;

    /** Stato dell'eventuale pagamento associato alla prenotazione. */
    private String paymentStatus;

    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;
}
