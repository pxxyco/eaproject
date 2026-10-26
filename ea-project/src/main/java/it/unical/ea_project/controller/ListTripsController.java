package it.unical.ea_project.controller;

import it.unical.ea_project.domain.ListTrips;
import it.unical.ea_project.domain.Trip;
import it.unical.ea_project.dto.ListTripsDTO;
import it.unical.ea_project.repository.TripRepository;
import it.unical.ea_project.security.AuthUser;
import it.unical.ea_project.service.FavoriteListAccess;
import it.unical.ea_project.service.ListTripsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/list-trips")
@RequiredArgsConstructor
public class ListTripsController {

    private final ListTripsService listTripsService;
    private final FavoriteListAccess access;
    private final TripRepository trips;

    public record AddTripRequest(Long listId, Long tripId) {}

    @PostMapping
    public ResponseEntity<?> addTripToList(@RequestBody AddTripRequest request,
                                           @AuthenticationPrincipal AuthUser user) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .build();
        }

        if (request == null || request.listId() == null || request.tripId() == null) {
            return ResponseEntity.badRequest()
                .body("listId e tripId sono obbligatori.");
        }

        if (!access.canEdit(request.listId(), user.id())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Non hai i permessi per modificare questa lista.");
        }

        try {

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(toDTO(listTripsService.addTripToList(request.listId(), request.tripId())));

        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest()
                .body(ex.getMessage());
        }

    }

    @GetMapping("/list/{listId}")
    public ResponseEntity<?> getTripsByList(@PathVariable Long listId,
                                            @AuthenticationPrincipal AuthUser user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .build();
        }

        if (!access.canView(listId, user.id())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Non hai accesso a questa lista.");
        }

        return ResponseEntity.ok(listTripsService.getTripsByListId(listId).stream().map(this::toDTO).toList());

    }

    @DeleteMapping("/list/{listId}/trip/{tripId}")
    public ResponseEntity<?> removeTripFromList(@PathVariable Long listId,
                                                @PathVariable Long tripId,
                                                @AuthenticationPrincipal AuthUser user) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .build();
        }

        if (!access.canEdit(listId, user.id())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Non hai i permessi per modificare questa lista.");
        }

        try {

            listTripsService.removeTripFromList(listId, tripId);
            return ResponseEntity.noContent()
                .build();

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }

    }

    private ListTripsDTO toDTO(ListTrips association) {

        Trip trip = trips.findByTripIdAndDeletedAtIsNull(association.getTripId()).orElse(null);
        String location = trip == null ? "" :
                String.join(", ", java.util.stream.Stream.of(trip.getDestinationCity(), trip.getDestinationCountry())
                        .filter(s -> s != null && !s.isBlank()).toList());
        return ListTripsDTO.builder()
                .listId(association.getFavoriteList().getId())
                .tripId(association.getTripId())
                .title(trip == null ? "Viaggio non disponibile" : trip.getTitle())
                .location(location)
                .addedAt(association.getAddedAt())
                .build();

    }

}
