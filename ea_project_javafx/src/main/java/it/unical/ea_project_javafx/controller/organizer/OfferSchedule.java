package it.unical.ea_project_javafx.controller.organizer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

final class OfferSchedule {
    private OfferSchedule() {}

    static LocalTime time(String value, boolean required) {
        if (value == null || value.isBlank()) {
            if (required) throw new IllegalArgumentException("Inserisci gli orari di inizio e fine (HH:mm).");
            return null;
        }
        try { return LocalTime.parse(value.trim()); }
        catch (java.time.DateTimeException ex) {
            throw new IllegalArgumentException("Orario non valido: usa HH:mm.");
        }
    }

    static LocalDateTime[] activityPeriod(LocalDate start, String startTime, LocalDate end, String endTime) {
        if (start == null || end == null) throw new IllegalArgumentException("Inserisci entrambe le date.");
        LocalDateTime first = start.atTime(time(startTime, true));
        LocalDateTime last = end.atTime(time(endTime, true));
        if (!last.isAfter(first)) throw new IllegalArgumentException("La fine dell’attività deve essere successiva all’inizio.");
        return new LocalDateTime[]{first, last};
    }

    static void stageTimes(LocalTime start, LocalTime end) {
        if (start != null && end != null && !end.isAfter(start))
            throw new IllegalArgumentException("La fine della tappa deve essere successiva all’inizio nello stesso giorno.");
    }

    static int activeSeats(java.util.List<it.unical.ea_project_javafx.dto.booking.BookingDTO> bookings, LocalDateTime now) {
        return bookings.stream()
                .filter(b -> !it.unical.ea_project_javafx.ui.BookingPresentation.isHistorical(b, now))
                .mapToInt(b -> b.getSeats() == null ? 0 : b.getSeats()).sum();
    }

    static boolean upcoming(boolean trip, LocalDateTime start, LocalDateTime now) {
        return start != null && (trip ? !start.toLocalDate().isBefore(now.toLocalDate()) : !start.isBefore(now));
    }
}
