package it.unical.ea_project.util;

import it.unical.ea_project.domain.Activity;
import it.unical.ea_project.domain.Trip;
import java.math.BigDecimal;

/** Validation shared by all clients: capacity cannot overwrite already reserved seats. */
public final class ExperienceValidation {
    private ExperienceValidation() { }
    public static void trip(Trip trip) {
        common(trip.getTitle(), trip.getMaxSeats(), trip.getTotalPrice());
        if (trip.getStartDate() == null || trip.getEndDate() == null
                || trip.getEndDate().isBefore(trip.getStartDate()))
            throw new IllegalArgumentException("Le date del viaggio non sono valide.");
        if (trip.getDestinationCity() == null || trip.getDestinationCity().isBlank())
            throw new IllegalArgumentException("La città di destinazione è obbligatoria.");
    }
    public static void activity(Activity activity) {
        if (activity.getPrice() == null || !Double.isFinite(activity.getPrice()))
            throw new IllegalArgumentException("Il prezzo non è valido.");
        common(activity.getTitle(), activity.getMaxSeats(), BigDecimal.valueOf(activity.getPrice()));
        if (activity.getCategory() == null || activity.getStartDate() == null || activity.getEndDate() == null
                || !activity.getEndDate().isAfter(activity.getStartDate()))
            throw new IllegalArgumentException("Categoria e periodo dell'attività devono essere validi.");
        if (activity.getCity() == null || activity.getCity().isBlank())
            throw new IllegalArgumentException("La città è obbligatoria.");
    }
    private static void common(String title, Integer capacity, BigDecimal price) {
        if (title == null || title.isBlank() || title.trim().length() > 255)
            throw new IllegalArgumentException("Il titolo deve contenere da 1 a 255 caratteri.");
        if (capacity == null || capacity < 1 || price == null || price.signum() < 0 || price.compareTo(new BigDecimal("99999999.99")) > 0)
            throw new IllegalArgumentException("Prezzo e numero di posti non sono validi.");
    }
    public static int available(Integer oldCapacity, Integer oldAvailable, int newCapacity) {
        int reserved = Math.max(0, (oldCapacity == null ? 0 : oldCapacity) - (oldAvailable == null ? 0 : oldAvailable));
        if (newCapacity < reserved) throw new IllegalArgumentException("La capacità non può essere inferiore ai posti già prenotati.");
        return newCapacity - reserved;
    }
}
