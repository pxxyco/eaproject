package it.unical.ea_project.controller;

import it.unical.ea_project.domain.FavoriteList;
import it.unical.ea_project.domain.ListSharing;
import it.unical.ea_project.repository.FavoriteListRepository;
import it.unical.ea_project.repository.ListSharingRepository;
import it.unical.ea_project.security.AuthUser;
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
    private final FavoriteListRepository lists;
    private final ListSharingRepository sharings;

    public record AddTripRequest(Long listId, Long tripId) {}

    @PostMapping
    public ResponseEntity<?> addTripToList(@RequestBody AddTripRequest request,
                                           @AuthenticationPrincipal AuthUser user) {
    
        if (!canEdit(request.listId(), user.id())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(listTripsService.addTripToList(request.listId(), request.tripId()));
    
    }

    @GetMapping("/list/{listId}")
    public ResponseEntity<?> getTripsByList(@PathVariable Long listId,
                                            @AuthenticationPrincipal AuthUser user) {
        
        if (!canView(listId, user.id())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(listTripsService.getTripsByListId(listId));
    
    }

    @DeleteMapping("/list/{listId}/trip/{tripId}")
    public ResponseEntity<Void> removeTripFromList(@PathVariable Long listId, 
                                                   @PathVariable Long tripId,
                                                   @AuthenticationPrincipal AuthUser user) {

        if (!canEdit(listId, user.id())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        listTripsService.removeTripFromList(listId, tripId);
        return ResponseEntity.noContent().build();
    
    }

    private FavoriteList activeList(Long id) {
        return lists.findById(id).filter(l -> !l.isDeleted()).orElse(null);
    }

    private boolean canView(Long listId, Long userId) {

        FavoriteList list = activeList(listId);
        return list != null 
                && (list.getUser().getId().equals(userId)
                || sharings.existsByFavoriteList_IdAndUser_Id(listId, userId));

    }

    private boolean canEdit(Long listId, Long userId) {

        FavoriteList list = activeList(listId);
        if (list == null) {
            return false;
        }

        if (list.getUser().getId().equals(userId)) {
            return true;
        }

        return sharings.findByFavoriteListId(listId)
            .stream()
            .anyMatch(s ->
                s.getUser().getId().equals(userId) && s.getPermission() == ListSharing.SharingPermission.EDIT
            );

    }

}