package it.unical.ea_project.util;

import it.unical.ea_project.domain.Activity;
import it.unical.ea_project.domain.Trip;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class OfferValidation {
    private OfferValidation() {}

    private static void require(boolean valid, String message) {
        if (!valid) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
    private static void common(String title, String city, Integer seats) {
        require(title != null && !title.isBlank() && title.length() <= 255, "Titolo obbligatorio (massimo 255 caratteri).");
        require(city != null && !city.isBlank() && city.length() <= 100, "Città obbligatoria (massimo 100 caratteri).");
        require(seats != null && seats > 0, "Il numero di posti deve essere positivo.");
    }
    public static void validate(Trip trip) {
        common(trip.getTitle(), trip.getDestinationCity(), trip.getMaxSeats());
        require(trip.getDestinationCountry() != null && !trip.getDestinationCountry().isBlank(), "Paese obbligatorio.");
        require(trip.getTotalPrice() != null && trip.getTotalPrice().signum() >= 0, "Prezzo non valido.");
        require(trip.getStartDate() != null && trip.getEndDate() != null && !trip.getEndDate().isBefore(trip.getStartDate()), "Intervallo delle date non valido.");
        if (trip.getStages() != null) for (var stage : trip.getStages()) {
            require(stage != null && stage.getTitle() != null && !stage.getTitle().isBlank()
                    && stage.getCategory() != null && stage.getDay() != null && stage.getDay() >= 1
                    && stage.getDay() <= java.time.temporal.ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate()) + 1
                    && stage.getOrderInDay() != null && stage.getOrderInDay() >= 1, "Dati della tappa non validi.");
        }
    }
    public static void validate(Activity activity) {
        common(activity.getTitle(), activity.getCity(), activity.getMaxSeats());
        require(activity.getCategory() != null, "Categoria obbligatoria.");
        require(activity.getPrice() != null && Double.isFinite(activity.getPrice()) && activity.getPrice() >= 0, "Prezzo non valido.");
        require(activity.getDurationMinutes() != null && activity.getDurationMinutes() > 0, "Durata non valida.");
        require(activity.getStartDate() != null && activity.getEndDate() != null && !activity.getEndDate().isBefore(activity.getStartDate()), "Intervallo delle date non valido.");
    }
}
