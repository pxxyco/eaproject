package it.unical.ea_project.controller.favorites;

import it.unical.ea_project.domain.activity.Activity;
import it.unical.ea_project.domain.favorites.ListActivities;
import it.unical.ea_project.dto.favorites.ListActivitiesDTO;
import it.unical.ea_project.repository.activity.ActivityRepository;
import it.unical.ea_project.security.AuthUser;
import it.unical.ea_project.service.favorites.FavoriteListAccess;
import it.unical.ea_project.service.favorites.ListActivitiesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/list-activities")
@RequiredArgsConstructor
public class ListActivitiesController {
    private final ListActivitiesService service;
    private final FavoriteListAccess access;
    private final ActivityRepository activities;

    public record AddActivityRequest(Long listId, Long activityId) {
    }

    @PostMapping
    public ResponseEntity<?> addActivity(@RequestBody AddActivityRequest request,
            @AuthenticationPrincipal AuthUser user) {
        if (user == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (request == null || request.listId() == null || request.activityId() == null)
            return ResponseEntity.badRequest().body("listId e activityId sono obbligatori.");
        if (!access.canEdit(request.listId(), user.id()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Non hai i permessi per modificare questa lista.");
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(toDTO(service.addActivityToList(request.listId(), request.activityId())));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @GetMapping("/list/{listId}")
    public ResponseEntity<?> getActivities(@PathVariable Long listId,
            @AuthenticationPrincipal AuthUser user) {
        if (user == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!access.canView(listId, user.id()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Non hai accesso a questa lista.");
        return ResponseEntity.ok(service.getActivitiesByListId(listId).stream().map(this::toDTO).toList());
    }

    @DeleteMapping("/list/{listId}/activity/{activityId}")
    public ResponseEntity<?> removeActivity(@PathVariable Long listId, @PathVariable Long activityId,
            @AuthenticationPrincipal AuthUser user) {
        if (user == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!access.canEdit(listId, user.id()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Non hai i permessi per modificare questa lista.");
        try {
            service.removeActivityFromList(listId, activityId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
    }

    private ListActivitiesDTO toDTO(ListActivities association) {
        Activity activity = activities.findByActivityIdAndDeletedAtIsNull(association.getActivityId()).orElse(null);
        return ListActivitiesDTO.builder()
                .listId(association.getFavoriteList().getId())
                .activityId(association.getActivityId())
                .title(activity == null ? "Attività non disponibile" : activity.getTitle())
                .city(activity == null ? "" : activity.getCity())
                .category(activity == null || activity.getCategory() == null ? "" : activity.getCategory().name())
                .addedAt(association.getAddedAt()).build();
    }

}
