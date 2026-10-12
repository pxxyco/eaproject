package it.unical.ea_project_javafx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;


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

    private String itemLocation;

    private Integer seats;

    // Booking.BookingType: TRIP, ACTIVITY
    private String bookingType;

    // Booking.BookingStatus: PENDING, COMPLETED, CONFIRMED, CANCELLED
    private String bookingStatus;

    private BigDecimal price;

    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;
}
