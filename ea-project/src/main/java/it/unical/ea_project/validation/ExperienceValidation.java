package it.unical.ea_project.validation;

import it.unical.ea_project.domain.activity.Activity;
import it.unical.ea_project.domain.trip.Stage;
import it.unical.ea_project.domain.trip.Trip;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** One validation policy for offers, itinerary stages and reserved capacity. */
public final class ExperienceValidation {
        private ExperienceValidation() {
        }

        private static void require(boolean valid, String message) {
                if (!valid)
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }

        public static void trip(Trip trip) {
                common(trip.getTitle(), trip.getDestinationCity(), trip.getMaxSeats(), trip.getTotalPrice());
                require(trip.getDestinationCountry() == null || (!trip.getDestinationCountry().isBlank()
                                && trip.getDestinationCountry().length() <= 100),
                                "Paese non valido (massimo 100 caratteri).");
                require(trip.getStartDate() != null && trip.getEndDate() != null
                                && !trip.getEndDate().isBefore(trip.getStartDate()),
                                "Le date del viaggio non sono valide.");
                if (trip.isStagesProvided() && trip.getStages() != null)
                        for (Stage stage : trip.getStages())
                                stage(trip, stage);
        }

        public static void activity(Activity activity) {
                require(activity.getPrice() != null && Double.isFinite(activity.getPrice()), "Il prezzo non è valido.");
                common(activity.getTitle(), activity.getCity(), activity.getMaxSeats(),
                                BigDecimal.valueOf(activity.getPrice()));
                require(activity.getCategory() != null && activity.getStartDate() != null
                                && activity.getEndDate() != null
                                && activity.getEndDate().isAfter(activity.getStartDate()),
                                "Categoria e periodo dell'attività devono essere validi.");
                require(activity.getDurationMinutes() == null || activity.getDurationMinutes() > 0,
                                "La durata deve essere positiva.");
        }

        private static void common(String title, String city, Integer capacity, BigDecimal price) {
                require(title != null && !title.isBlank() && title.length() <= 255,
                                "Titolo obbligatorio (massimo 255 caratteri).");
                require(city != null && !city.isBlank() && city.length() <= 100,
                                "Città obbligatoria (massimo 100 caratteri).");
                require(capacity != null && capacity > 0, "Il numero di posti deve essere positivo.");
                require(price != null && price.signum() >= 0 && price.compareTo(new BigDecimal("99999999.99")) <= 0,
                                "Il prezzo non è valido.");
        }

        public static void stage(Trip trip, Stage stage) {
                require(stage != null && stage.getTitle() != null && !stage.getTitle().isBlank()
                                && stage.getCategory() != null && stage.getDay() != null && stage.getDay() >= 1
                                && stage.getOrderInDay() != null && stage.getOrderInDay() >= 1,
                                "Dati della tappa non validi.");
                stageTimes(stage.getStartTime(), stage.getEndTime());
                require(trip.getStartDate() != null && trip.getEndDate() != null
                                && stage.getDay() <= ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate())
                                                + 1,
                                "Il giorno della tappa deve rientrare nelle date del viaggio.");
        }

        public static void stageTimes(java.time.LocalTime start, java.time.LocalTime end) {
                require(start == null || end == null || end.isAfter(start),
                                "L'orario finale della tappa deve essere successivo a quello iniziale nello stesso giorno.");
        }

        public static int available(Integer oldCapacity, Integer oldAvailable, int newCapacity) {
                require(newCapacity > 0, "Il numero di posti deve essere positivo.");
                if (oldCapacity == null || oldAvailable == null || oldAvailable < 0 || oldAvailable > oldCapacity)
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                        "Disponibilità dei posti incoerente. Aggiorna l'offerta.");
                int reserved = oldCapacity - oldAvailable;
                require(newCapacity >= reserved, "I posti totali non possono essere inferiori ai posti già prenotati.");
                return newCapacity - reserved;
        }
}
