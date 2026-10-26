package it.unical.ea_project.dto.payment;

import java.math.BigDecimal;
import java.util.List;

public record OrganizerStatisticsDTO(List<Month> months, BigDecimal periodPaid, long paymentCount,
        BigDecimal averagePayment, long activeBookings, long reservedSeats, BigDecimal reservedValue) {
    public record Month(String month, BigDecimal trips, BigDecimal activities) {
    }
}
