package it.unical.ea_project.controller;

import it.unical.ea_project.domain.Stage;
import it.unical.ea_project.domain.Trip;
import it.unical.ea_project.dto.StageDTO;
import it.unical.ea_project.dto.TripDTO;
import it.unical.ea_project.dto.home.TripHomeDTO;
import it.unical.ea_project.dto.home.TripSuggestionDTO;
import it.unical.ea_project.repository.UserRepository;
import it.unical.ea_project.security.AuthUser;
import it.unical.ea_project.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
    public ResponseEntity<List<TripDTO>> getTripsByOrganizer(@PathVariable Long creatorId, @AuthenticationPrincipal AuthUser user) {
        if (user == null || !user.id().equals(creatorId)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return userRepository.findById(creatorId)
                .map(creator -> ResponseEntity.ok(tripService.getTripDtosByUser(creator)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/available")
    public ResponseEntity<List<TripDTO>> getAvailableTrips() {
        return ResponseEntity.ok(tripService.getAvailableTripDtos());
    }

    @GetMapping("/suggest")
    public ResponseEntity<List<TripSuggestionDTO>> suggestTrips(@RequestParam String query, @RequestParam(defaultValue = "6") int limit) {
        return ResponseEntity.ok(tripService.getSuggestions(query, limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripDTO> getTripById(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return tripService.getTripDtoById(id)
                .filter(dto -> !"DRAFT".equals(dto.getStatus()) || user != null && java.util.Objects.equals(dto.getCreatedByUserId(), user.id()))
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
        return ResponseEntity.ok(tripService.searchTripHomeDtos(query, date, page, size));
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
        if (existing.isEmpty()) return ResponseEntity.notFound().build();
        if (!isOwner(existing.get(), user)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(tripService.updateTripDto(id, details));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrip(@PathVariable Long id,
                                           @AuthenticationPrincipal AuthUser user) {
        var existing = tripService.getTripById(id);
        if (existing.isEmpty()) return ResponseEntity.notFound().build();
        if (!isOwner(existing.get(), user)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        tripService.deleteTrip(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isOwner(Trip trip, AuthUser user) {
        return trip.getCreatedBy() != null && trip.getCreatedBy().getId().equals(user.id());
    }

    private TripDTO toDtoDetail(Trip trip) {
        TripDTO dto = toDto(trip);
        dto.setStages(toStageDtoList(trip.getStages()));

        return dto;
    }

    private List<StageDTO> toStageDtoList(List<Stage> stages) {
        return stages.stream()
                .map(StageDTO::toDto)
                .collect(Collectors.toList());
    }


    // DEBUG
    private TripDTO toDto(Trip trip) {
        Long creatorId = trip.getCreatedBy() != null ? trip.getCreatedBy().getId() : null;

        return TripDTO.builder()
                .tripId(trip.getTripId())
                .createdByUserId(creatorId)
                .title(trip.getTitle())
                .description(trip.getDescription())
                .destinationCountry(trip.getDestinationCountry())
                .destinationCity(trip.getDestinationCity())
                .startDate(trip.getStartDate())
                .endDate(trip.getEndDate())
                .totalPrice(trip.getTotalPrice())
                .maxSeats(trip.getMaxSeats())
                .availableSeats(trip.getAvailableSeats())
                .status(trip.getStatus() != null ? trip.getStatus().name() : null)
                .icsUid(trip.getIcsUid())
                .averageRating(trip.getAverageRating())
                .coverPhotoUrl(trip.getCoverPhotoUrl())
                .createdAt(trip.getCreatedAt())
                .updatedAt(trip.getUpdatedAt())
                .deletedAt(trip.getDeletedAt())
                .stages(Collections.emptyList())
                .build();
    }

}