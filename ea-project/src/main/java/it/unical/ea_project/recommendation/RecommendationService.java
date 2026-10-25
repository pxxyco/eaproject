package it.unical.ea_project.recommendation;

import it.unical.ea_project.domain.*;
import it.unical.ea_project.dto.home.*;
import it.unical.ea_project.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.text.Normalizer;
import java.util.*;

@Service
@lombok.RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {
    public record TripPick(TripHomeDTO item, String reason) {
    }

    public record ActivityPick(ActivityHomeDTO item, String reason) {
    }

    public record Picks(boolean personalized, List<TripPick> trips, List<ActivityPick> activities) {
    }

    private final UserRepository users;
    private final it.unical.ea_project.account.AccountSettingsRepository preferences;
    private final TripRepository trips;
    private final ActivityRepository activities;
    private final BookingRepository bookings;
    private final ListTripsRepository favoriteTrips;
    private final ListActivitiesRepository favoriteActivities;
    private final ActivityImageRepository images;
    private final ReviewRepository reviews;

    private record Candidate<T>(T item, double score, String reason, String destination) {
    }

    public Picks recommend(Long userId, int requested) {
        var user = users.findById(userId).filter(u -> u.getDeletedAt() == null && u.getRole() == User.Role.TRAVELER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Consigli disponibili per i viaggiatori."));
        int limit = Math.max(1, Math.min(24, requested));
        Map<String, Double> affinity = new HashMap<>();
        Set<Long> bookedTrips = new HashSet<>(), bookedActivities = new HashSet<>();
        var history = bookings.recommendationHistory(userId, PageRequest.of(0, 200));
        for (var b : history) {
            if (b.getBookingStatus() == Booking.BookingStatus.CANCELLED)
                continue;
            double days = b.getCreatedAt() == null ? 0
                    : Math.max(0, Duration.between(b.getCreatedAt(), LocalDateTime.now()).toDays());
            double weight = 3 * Math.pow(.5, days / 180.0);
            if (b.getTrip() != null) {
                bookedTrips.add(b.getTrip().getTripId());
                signal(affinity, "city", b.getTrip().getDestinationCity(), weight);
                signal(affinity, "country", b.getTrip().getDestinationCountry(), weight);
            }
            if (b.getActivity() != null) {
                bookedActivities.add(b.getActivity().getActivityId());
                signal(affinity, "city", b.getActivity().getCity(), weight);
                signal(affinity, "category",
                        b.getActivity().getCategory() == null ? null : b.getActivity().getCategory().name(), weight);
            }
        }
        for (var t : favoriteTrips.preferenceTrips(userId, PageRequest.of(0, 200))) {
            signal(affinity, "city", t.getDestinationCity(), 2);
            signal(affinity, "country", t.getDestinationCountry(), 2);
        }
        for (var a : favoriteActivities.preferenceActivities(userId, PageRequest.of(0, 200))) {
            signal(affinity, "city", a.getCity(), 2);
            signal(affinity, "category", a.getCategory() == null ? null : a.getCategory().name(), 2);
        }
        for (var r : reviews.preferenceReviews(userId, PageRequest.of(0, 100))) {
            if (r.getRating() == null)
                continue;
            double weight = r.getRating() - 3d;
            if (r.getTrip() != null) {
                signal(affinity, "city", r.getTrip().getDestinationCity(), weight);
                signal(affinity, "country", r.getTrip().getDestinationCountry(), weight);
            }
            if (r.getActivity() != null) {
                signal(affinity, "city", r.getActivity().getCity(), weight);
                signal(affinity, "category",
                        r.getActivity().getCategory() == null ? null : r.getActivity().getCategory().name(), weight);
            }
        }
        if (!preferences.findById(userId).map(it.unical.ea_project.account.AccountSettings::isPersonalized)
                .orElse(true))
            affinity.clear();
        List<Candidate<Trip>> tripCandidates = new ArrayList<>();
        for (var t : trips.recommendationCandidates(LocalDate.now(), userId, PageRequest.of(0, 200))) {
            if (bookedTrips.contains(t.getTripId()))
                continue;
            double city = score(affinity, "city", t.getDestinationCity()),
                    country = score(affinity, "country", t.getDestinationCountry());
            double quality = t.getAverageRating() == null ? 0 : t.getAverageRating().doubleValue() / 5;
            tripCandidates.add(new Candidate<>(t, city + country * .7 + quality,
                    city > 0 ? "Una destinazione in linea con le tue esperienze"
                            : country > 0 ? "Un paese che ti interessa" : "Un viaggio disponibile scelto per te",
                    normalize(t.getDestinationCity())));
        }
        var activityCandidates = new ArrayList<Candidate<Activity>>();
        for (var a : activities.recommendationCandidates(LocalDateTime.now(), userId, PageRequest.of(0, 200))) {
            if (bookedActivities.contains(a.getActivityId()))
                continue;
            double city = score(affinity, "city", a.getCity()),
                    category = score(affinity, "category", a.getCategory() == null ? null : a.getCategory().name());
            double quality = a.getAverageRating() == null ? 0 : a.getAverageRating() / 5;
            activityCandidates.add(new Candidate<>(a, city + category + quality,
                    category > 0 ? "Una categoria vicina ai tuoi interessi"
                            : city > 0 ? "Una città tra le tue preferenze" : "Un'attività disponibile da scoprire",
                    normalize(a.getCity())));
        }
        var chosenTrips = diverse(tripCandidates, limit);
        var chosenActivities = diverse(activityCandidates, limit);
        Map<Long, String> covers = new HashMap<>();
        var ids = chosenActivities.stream().map(c -> c.item().getActivityId()).toList();
        if (!ids.isEmpty())
            images.findByActivityIds(ids).stream()
                    .sorted(Comparator.comparing(ActivityImage::getOrderIndex,
                            Comparator.nullsLast(Integer::compareTo)))
                    .forEach(i -> covers.putIfAbsent(i.getActivity().getActivityId(), i.getImageUrl()));
        return new Picks(!affinity.isEmpty(), chosenTrips.stream().map(c -> {
            var t = c.item();
            return new TripPick(new TripHomeDTO(t.getTripId(), t.getTitle(),
                    String.join(", ",
                            java.util.stream.Stream.of(t.getDestinationCity(), t.getDestinationCountry())
                                    .filter(Objects::nonNull).toList()),
                    t.getTotalPrice(), t.getAverageRating(), t.getCoverPhotoUrl(), t.getStartDate(), t.getEndDate()),
                    c.reason());
        }).toList(),
                chosenActivities.stream().map(c -> {
                    var a = c.item();
                    return new ActivityPick(new ActivityHomeDTO(a.getActivityId(), a.getTitle(), a.getCity(),
                            a.getCategory().name(), a.getPrice(), a.getAverageRating(), covers.get(a.getActivityId()),
                            a.getStartDate(), a.getEndDate()), c.reason());
                }).toList());
    }

    private static String normalize(String value) {
        return value == null ? ""
                : Normalizer.normalize(value.strip().toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}",
                        "");
    }

    private static void signal(Map<String, Double> profile, String kind, String value, double weight) {
        if (value != null && !value.isBlank())
            profile.merge(kind + ":" + normalize(value), weight, Double::sum);
    }

    private static double score(Map<String, Double> profile, String kind, String value) {
        double n = profile.getOrDefault(kind + ":" + normalize(value), 0d);
        return Math.signum(n) * Math.log1p(Math.abs(n));
    }

    private static <T> List<Candidate<T>> diverse(List<Candidate<T>> candidates, int limit) {
        var result = new ArrayList<Candidate<T>>();
        Map<String, Integer> used = new HashMap<>();
        while (!candidates.isEmpty() && result.size() < limit) {
            var best = candidates.stream()
                    .max(Comparator.comparingDouble(c -> c.score() - used.getOrDefault(c.destination(), 0) * .65))
                    .orElseThrow();
            candidates.remove(best);
            result.add(best);
            used.merge(best.destination(), 1, Integer::sum);
        }
        return result;
    }
}
