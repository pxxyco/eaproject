package it.unical.ea_project.controller;

import it.unical.ea_project.domain.FavoriteList;
import it.unical.ea_project.domain.ListSharing;
import it.unical.ea_project.domain.ListTrips;
import it.unical.ea_project.domain.Trip;
import it.unical.ea_project.dto.ListTripsDTO;
import it.unical.ea_project.repository.FavoriteListRepository;
import it.unical.ea_project.repository.ListSharingRepository;
import it.unical.ea_project.repository.TripRepository;
import it.unical.ea_project.security.AuthUser;
import it.unical.ea_project.service.ListTripsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/list-trips")
@RequiredArgsConstructor
public class ListTripsController {

    private final ListTripsService listTripsService;
    private final FavoriteListRepository lists;
    private final ListSharingRepository sharings;
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
        
        if (!canEdit(request.listId(), user.id())) {
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

        if (!canView(listId, user.id())) {
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

        if (!canEdit(listId, user.id())) {
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
                .title(trip == null ? "Viaggio #" + association.getTripId() : trip.getTitle())
                .location(location)
                .addedAt(association.getAddedAt())
                .build();

    }

    private FavoriteList activeList(Long id) {

        if (id == null) {
            return null;
        }

        return lists.findById(id)
            .filter(l -> !l.isDeleted()).orElse(null);

    }

    private boolean canView(Long listId, Long userId) {

        FavoriteList list = activeList(listId);
        return list != null && (list.getUser().getId().equals(userId)
                || list.getVisibility() == FavoriteList.Visibility.SHARED
                && sharings.existsByFavoriteList_IdAndUser_Id(listId, userId));

    }

    private boolean canEdit(Long listId, Long userId) {

        FavoriteList list = activeList(listId);
        if (list == null) {
            return false;
        }

        if (list.getUser().getId().equals(userId)) {
            return true;
        }

        if (list.getVisibility() != FavoriteList.Visibility.SHARED) return false;
        return sharings.findByFavoriteListId(listId)
            .stream().anyMatch(s ->
                s.getUser().getId().equals(userId) && s.getPermission() == ListSharing.SharingPermission.EDIT
            );

    }
}