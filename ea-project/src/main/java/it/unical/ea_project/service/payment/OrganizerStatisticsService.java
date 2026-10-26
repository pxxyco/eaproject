package it.unical.ea_project.service.payment;

import it.unical.ea_project.domain.activity.Activity;
import it.unical.ea_project.domain.trip.Trip;
import it.unical.ea_project.domain.booking.Booking;
import it.unical.ea_project.domain.payment.Payment;
import it.unical.ea_project.dto.payment.OrganizerStatisticsDTO;
import it.unical.ea_project.repository.booking.BookingRepository;
import it.unical.ea_project.repository.payment.PaymentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizerStatisticsService {
    private final PaymentRepository payments;
    private final BookingRepository bookings;

    public OrganizerStatisticsService(PaymentRepository payments, BookingRepository bookings) {
        this.payments = payments;
        this.bookings = bookings;
    }

    @Transactional(readOnly = true)
    public OrganizerStatisticsDTO overview(Long organizer) {
        YearMonth first = YearMonth.now().minusMonths(11);
        var monthly = payments.organizerMonthly(organizer, first.atDay(1).atStartOfDay(),
                List.of(Payment.PaymentStatus.COMPLETED, Payment.PaymentStatus.CONFIRMED));
        var indexed = new HashMap<YearMonth, Object[]>();
        monthly.forEach(
                row -> indexed.put(YearMonth.of(((Number) row[0]).intValue(), ((Number) row[1]).intValue()), row));
        var series = new ArrayList<OrganizerStatisticsDTO.Month>();
        BigDecimal total = BigDecimal.ZERO;
        long count = 0;
        for (int i = 0; i < 12; i++) {
            YearMonth month = first.plusMonths(i);
            Object[] row = indexed.get(month);
            BigDecimal trips = row == null ? BigDecimal.ZERO : (BigDecimal) row[2];
            BigDecimal activities = row == null ? BigDecimal.ZERO : (BigDecimal) row[3];
            count += row == null ? 0 : ((Number) row[4]).longValue();
            total = total.add(trips).add(activities);
            series.add(new OrganizerStatisticsDTO.Month(month.toString(), trips, activities));
        }
        Object[] reserved = bookings.organizerReservationTotals(organizer, Booking.BookingStatus.CANCELLED,
                Trip.TripStatus.CANCELLED, Activity.Status.CANCELLED).getFirst();
        return new OrganizerStatisticsDTO(series, total, count,
                count == 0 ? BigDecimal.ZERO : total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP),
                ((Number) reserved[0]).longValue(), ((Number) reserved[1]).longValue(), (BigDecimal) reserved[2]);
    }
}
