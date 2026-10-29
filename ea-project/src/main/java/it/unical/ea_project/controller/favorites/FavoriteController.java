package it.unical.ea_project.controller.favorites;

import it.unical.ea_project.dto.favorites.FavoriteDTO;
import it.unical.ea_project.security.AuthUser;
import it.unical.ea_project.service.favorites.FavoriteService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

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
            if (!favoriteService.isActivityFavorite(user.id(), activityId)) throw ex;
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
            if (!favoriteService.isTripFavorite(user.id(), tripId)) throw ex;
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
