package it.unical.ea_project_javafx.ui;

import it.unical.ea_project_javafx.dto.booking.BookingDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Supplier;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableRow;

/**
 * One interpretation of the booking data supplied by the backend, shared by
 * traveler views.
 */
public final class BookingPresentation {
    private BookingPresentation() {
    }

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static int compareBookings(BookingDTO left, BookingDTO right, LocalDateTime evaluationTime) {
        boolean leftCancelled = isCancelled(left);
        boolean rightCancelled = isCancelled(right);
        if (leftCancelled != rightCancelled)
            return leftCancelled ? 1 : -1;

        boolean leftHistory = isHistorical(left, evaluationTime);
        boolean rightHistory = isHistorical(right, evaluationTime);
        if (leftHistory != rightHistory)
            return leftHistory ? 1 : -1;

        LocalDateTime leftDate = leftHistory ? eventEndDateTime(left) : eventStartDateTime(left);
        LocalDateTime rightDate = rightHistory ? eventEndDateTime(right) : eventStartDateTime(right);
        if (leftDate != null && rightDate != null) {
            int dateComparison = leftHistory ? rightDate.compareTo(leftDate) : leftDate.compareTo(rightDate);
            if (dateComparison != 0)
                return dateComparison;
        } else if (leftDate != null) {
            return -1;
        } else if (rightDate != null) {
            return 1;
        }

        LocalDateTime leftCreated = left.getCreatedAt();
        LocalDateTime rightCreated = right.getCreatedAt();
        if (leftCreated != null && rightCreated != null)
            return rightCreated.compareTo(leftCreated);
        if (leftCreated != null)
            return -1;
        if (rightCreated != null)
            return 1;
        Long leftId = left.getBookingId();
        Long rightId = right.getBookingId();
        return Long.compare(rightId == null ? 0 : rightId, leftId == null ? 0 : leftId);
    }

    public static boolean isHistorical(BookingDTO booking, LocalDateTime now) {
        if (booking == null)
            return false;
        return isCancelled(booking)
                || "PENDING".equalsIgnoreCase(booking.getBookingStatus()) && hasEventStarted(booking, now)
                || "COMPLETED".equalsIgnoreCase(booking.getBookingStatus())
                || "COMPLETED".equalsIgnoreCase(booking.getItemStatus())
                || isEventEnded(booking, now);
    }

    public static boolean isEventEnded(BookingDTO booking, LocalDateTime now) {
        LocalDateTime end = eventEndDateTime(booking);
        return end != null && !end.isAfter(now);
    }

    public static boolean hasEventStarted(BookingDTO booking, LocalDateTime now) {
        LocalDateTime start = eventStartDateTime(booking);
        return start != null && !start.isAfter(now);
    }

    public static boolean isCancelled(BookingDTO booking) {
        return booking != null && ("CANCELLED".equalsIgnoreCase(booking.getBookingStatus())
                || "CANCELLED".equalsIgnoreCase(booking.getItemStatus()));
    }

    public static LocalDateTime eventStartDateTime(BookingDTO booking) {
        if (booking == null || booking.getItemDate() == null || booking.getItemDate().isBlank())
            return null;
        String value = booking.getItemDate().trim();
        try {
            if (isTrip(booking) || !value.contains("T"))
                return LocalDate.parse(value).atStartOfDay();
            return LocalDateTime.parse(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static LocalDateTime eventEndDateTime(BookingDTO booking) {
        if (booking == null)
            return null;
        String value = booking.getItemEndDate();
        if (value == null || value.isBlank()) {
            // Graceful fallback for older backend responses without itemEndDate.
            LocalDateTime start = eventStartDateTime(booking);
            if (start == null)
                return null;
            return isTrip(booking) ? start.toLocalDate().atTime(LocalTime.MAX) : start.plusHours(1);
        }
        try {
            if (isTrip(booking) || !value.contains("T"))
                return LocalDate.parse(value).atTime(LocalTime.MAX);
            return LocalDateTime.parse(value);
        } catch (RuntimeException ignored) {
            LocalDateTime start = eventStartDateTime(booking);
            return start == null ? null
                    : (isTrip(booking)
                            ? start.toLocalDate().atTime(LocalTime.MAX)
                            : start.plusHours(1));
        }
    }

    public static boolean isTrip(BookingDTO booking) {
        return booking != null && "TRIP".equalsIgnoreCase(booking.getBookingType());
    }

    public static String formatEventPeriod(BookingDTO booking) {
        LocalDateTime start = eventStartDateTime(booking);
        LocalDateTime end = eventEndDateTime(booking);
        if (start == null)
            return "Data non disponibile";
        if (isTrip(booking)) {
            String formattedStart = start.toLocalDate().format(DATE_FORMAT);
            if (end == null || end.toLocalDate().equals(start.toLocalDate()))
                return formattedStart;
            return formattedStart + " – " + end.toLocalDate().format(DATE_FORMAT);
        }
        String formattedStart = start.format(DATE_TIME_FORMAT);
        if (end == null || end.equals(start))
            return formattedStart;
        if (end.toLocalDate().equals(start.toLocalDate())) {
            return formattedStart + " – " + end.format(TIME_FORMAT);
        }
        return formattedStart + " – " + end.format(DATE_TIME_FORMAT);
    }

    public static String formatBookedAt(BookingDTO booking) {
        return booking == null || booking.getCreatedAt() == null
                ? "—"
                : booking.getCreatedAt().format(DATE_TIME_FORMAT);
    }

    public static String displayStatus(BookingDTO booking, LocalDateTime evaluationTime) {
        if (booking == null)
            return "—";
        String status = booking.getBookingStatus() == null ? "" : booking.getBookingStatus().toUpperCase(Locale.ROOT);
        if ("CANCELLED".equals(status))
            return "Prenotazione cancellata";
        if ("CANCELLED".equalsIgnoreCase(booking.getItemStatus()))
            return "Esperienza annullata";
        boolean pending = "PENDING".equals(status);
        if (isHistorical(booking, evaluationTime))
            return pending ? "Scaduta" : "Conclusa";
        if (hasEventStarted(booking, evaluationTime))
            return pending ? "Scaduta" : "In corso";
        if (pending || booking.getPrice() != null && booking.getPrice().signum() > 0
                && !"COMPLETED".equalsIgnoreCase(booking.getPaymentStatus())
                && !"CONFIRMED".equalsIgnoreCase(booking.getPaymentStatus()))
            return "Da pagare";
        if ("CONFIRMED".equals(status))
            return "Confermata";
        return status.isBlank() ? "Stato non disponibile" : status;
    }

    public static boolean canCancel(BookingDTO booking, LocalDateTime now) {
        return booking != null && booking.getBookingId() != null && !isHistorical(booking, now)
                && !hasEventStarted(booking, now)
                && ("PENDING".equalsIgnoreCase(booking.getBookingStatus())
                    || "CONFIRMED".equalsIgnoreCase(booking.getBookingStatus()));
    }

    public static boolean canReview(BookingDTO booking, LocalDateTime now) {
        return booking != null && !isCancelled(booking)
                && ("CONFIRMED".equalsIgnoreCase(booking.getBookingStatus())
                    || "COMPLETED".equalsIgnoreCase(booking.getBookingStatus()));
    }

    public static String paymentStatus(String status) {
        if (status == null || status.isBlank())
            return "Non registrato";
        return switch (status) {
            case "COMPLETED", "CONFIRMED" -> "Pagato";
            case "PENDING" -> "In attesa";
            case "CANCELLED" -> "Annullato";
            default -> status;
        };
    }

    public static String statusStyleClass(String status) {
        return switch (status) {
            case "Cancellata", "Prenotazione cancellata", "Esperienza annullata" -> "booking-status-cancelled";
            case "Conclusa" -> "booking-status-completed";
            case "Confermata", "In corso" -> "booking-status-confirmed";
            case "In attesa", "Da pagare", "Scaduta" -> "booking-status-pending";
            default -> "booking-status-neutral";
        };
    }

    public static TableCell<BookingDTO, String> statusCell() {
        return new TableCell<>() {
            private final Label badge = new Label();

            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                setText(null);
                setGraphic(null);
                if (empty || status == null || status.isBlank())
                    return;
                badge.setText(status);
                badge.getStyleClass().setAll("label", "booking-status-pill", statusStyleClass(status));
                setGraphic(badge);
            }
        };
    }

    public static TableRow<BookingDTO> bookingRow(Supplier<LocalDateTime> now) {
        return new TableRow<>() {
            @Override
            protected void updateItem(BookingDTO booking, boolean empty) {
                super.updateItem(booking, empty);
                getStyleClass().removeAll("cancelled-booking-row", "historical-booking-row");
                if (empty || booking == null)
                    return;
                if (isCancelled(booking))
                    getStyleClass().add("cancelled-booking-row");
                else if (isHistorical(booking, now.get()))
                    getStyleClass().add("historical-booking-row");
            }
        };
    }
}
