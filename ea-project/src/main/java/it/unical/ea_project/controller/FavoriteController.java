package it.unical.ea_project.controller;

import it.unical.ea_project.domain.Activity;
import it.unical.ea_project.domain.Favorite;
import it.unical.ea_project.domain.Trip;
import it.unical.ea_project.dto.FavoriteDTO;
import it.unical.ea_project.repository.ActivityRepository;
import it.unical.ea_project.repository.TripRepository;
import it.unical.ea_project.security.AuthUser;
import it.unical.ea_project.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final ActivityRepository activities;
    private final TripRepository trips;

    @GetMapping
    public ResponseEntity<List<FavoriteDTO>> getMyFavorites(@AuthenticationPrincipal AuthUser user) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(favoriteService.getFavoritesByUser(user.id()));
    }

    @GetMapping("/activities/{activityId}")
    public ResponseEntity<Map<String, Boolean>> isActivityFavorite(@PathVariable Long activityId,
                                                                   @AuthenticationPrincipal AuthUser user) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(Map.of("favorite", favoriteService.isActivityFavorite(user.id(), activityId)));
    }

    @PostMapping("/activities/{activityId}")
    public ResponseEntity<?> addActivity(@PathVariable Long activityId,
                                         @AuthenticationPrincipal AuthUser user) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            favoriteService.addActivity(user.id(), activityId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            //doppio click: un'altra richiesta l'ha già inserito, il risultato è comunque corretto
        }
        return ResponseEntity.ok(Map.of("favorite", true));
    }

    @DeleteMapping("/activities/{activityId}")
    public ResponseEntity<?> removeActivity(@PathVariable Long activityId,
                                            @AuthenticationPrincipal AuthUser user) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        favoriteService.removeActivity(user.id(), activityId);
        return ResponseEntity.ok(Map.of("favorite", false));
    }


    @GetMapping("/trips/{tripId}")
    public ResponseEntity<Map<String, Boolean>> isTripFavorite(@PathVariable Long tripId,
                                                               @AuthenticationPrincipal AuthUser user) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(Map.of("favorite", favoriteService.isTripFavorite(user.id(), tripId)));
    }

    @PostMapping("/trips/{tripId}")
    public ResponseEntity<?> addTrip(@PathVariable Long tripId,
                                     @AuthenticationPrincipal AuthUser user) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            favoriteService.addTrip(user.id(), tripId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            //doppio click: già inserito da un'altra richiesta
        }
        return ResponseEntity.ok(Map.of("favorite", true));
    }

    @DeleteMapping("/trips/{tripId}")
    public ResponseEntity<?> removeTrip(@PathVariable Long tripId,
                                        @AuthenticationPrincipal AuthUser user) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        favoriteService.removeTrip(user.id(), tripId);
        return ResponseEntity.ok(Map.of("favorite", false));
    }

}