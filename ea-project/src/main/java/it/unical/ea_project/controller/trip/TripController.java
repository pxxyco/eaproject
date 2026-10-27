package it.unical.ea_project.controller.trip;

import it.unical.ea_project.domain.trip.Trip;
import it.unical.ea_project.dto.home.TripHomeDTO;
import it.unical.ea_project.dto.home.TripSuggestionDTO;
import it.unical.ea_project.dto.trip.TripDTO;
import it.unical.ea_project.repository.user.UserRepository;
import it.unical.ea_project.security.AuthUser;
import it.unical.ea_project.service.trip.TripService;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;
    private final UserRepository userRepository;

    @GetMapping("/published")
    public ResponseEntity<List<TripDTO>> getPublishedTrips() {
        return ResponseEntity.ok(tripService.getPublishedTripDtos());
    }

    @GetMapping("/organizer/{creatorId}")
    public ResponseEntity<List<TripDTO>> getTripsByOrganizer(@PathVariable Long creatorId,
            @AuthenticationPrincipal AuthUser user) {
        if (user == null || !user.id().equals(creatorId))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return userRepository.findById(creatorId)
                .map(creator -> ResponseEntity.ok(tripService.getTripDtosByUser(creator)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/available")
    public ResponseEntity<List<TripDTO>> getAvailableTrips() {
        return ResponseEntity.ok(tripService.getAvailableTripDtos());
    }

    @GetMapping("/suggest")
    public ResponseEntity<List<TripSuggestionDTO>> suggestTrips(@RequestParam String query,
            @RequestParam(defaultValue = "6") int limit) {
        return ResponseEntity.ok(tripService.getSuggestions(query, limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripDTO> getTripById(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return tripService.getTripDtoById(id)
                .filter(dto -> !"DRAFT".equals(dto.getStatus())
                        || user != null && Objects.equals(dto.getCreatedByUserId(), user.id()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search/country")
    public ResponseEntity<List<TripDTO>> searchByCountry(@RequestParam String country) {
        return ResponseEntity.ok(tripService.searchByCountryDtos(country));
    }

    @GetMapping("/search/city")
    public ResponseEntity<List<TripDTO>> searchByCity(@RequestParam String city) {
        return ResponseEntity.ok(tripService.searchByCityDtos(city));
    }

    @GetMapping("/search/date-range")
    public ResponseEntity<List<TripDTO>> getTripsInDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(tripService.getTripsInDateRangeDtos(start, end));
    }

    @GetMapping("/search")
    public ResponseEntity<List<TripHomeDTO>> searchTrips(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size) {
        return ResponseEntity.ok().header("X-Total-Count", Long.toString(tripService.countHomeTrips(query, date)))
                .body(tripService.searchTripHomeDtos(query, date, page, size));
    }

    @GetMapping("/top")
    public ResponseEntity<List<TripDTO>> getTopTrips(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(tripService.getTopTripDtos(page, size));
    }

    @PostMapping
    public ResponseEntity<TripDTO> createTrip(@RequestBody Trip trip,
            @AuthenticationPrincipal AuthUser user) {
        trip.setTripId(null);
        trip.setCoverPhotoUrl(null);
        return ResponseEntity.ok(tripService.createTripDto(trip, user.id()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TripDTO> updateTrip(@PathVariable Long id,
            @RequestBody Trip details,
            @AuthenticationPrincipal AuthUser user) {
        var existing = tripService.getTripById(id);
        if (existing.isEmpty())
            return ResponseEntity.notFound().build();
        if (!isOwner(existing.get(), user))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(tripService.updateTripDto(id, details));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrip(@PathVariable Long id,
            @AuthenticationPrincipal AuthUser user) {
        var existing = tripService.getTripById(id);
        if (existing.isEmpty())
            return ResponseEntity.notFound().build();
        if (!isOwner(existing.get(), user))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        tripService.deleteTrip(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isOwner(Trip trip, AuthUser user) {
        return trip.getCreatedBy() != null && trip.getCreatedBy().getId().equals(user.id());
    }

}
