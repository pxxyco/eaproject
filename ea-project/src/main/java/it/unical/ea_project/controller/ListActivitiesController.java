package it.unical.ea_project.controller;

import it.unical.ea_project.domain.Activity;
import it.unical.ea_project.domain.FavoriteList;
import it.unical.ea_project.domain.ListActivities;
import it.unical.ea_project.domain.ListSharing;
import it.unical.ea_project.dto.ListActivitiesDTO;
import it.unical.ea_project.repository.ActivityRepository;
import it.unical.ea_project.repository.FavoriteListRepository;
import it.unical.ea_project.repository.ListActivitiesRepository;
import it.unical.ea_project.repository.ListSharingRepository;
import it.unical.ea_project.security.AuthUser;
import it.unical.ea_project.service.ListActivitiesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/list-activities")
@RequiredArgsConstructor
public class ListActivitiesController {
    private final ListActivitiesService service;
    private final FavoriteListRepository lists;
    private final ListSharingRepository sharings;
    private final ActivityRepository activities;
    private final ListActivitiesRepository associations;

    public record AddActivityRequest(Long listId, Long activityId) {}

    @PostMapping
    public ResponseEntity<?> addActivity(@RequestBody AddActivityRequest request,
                                         @AuthenticationPrincipal AuthUser user) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (request == null || request.listId() == null || request.activityId() == null)
            return ResponseEntity.badRequest().body("listId e activityId sono obbligatori.");
        if (!canEdit(request.listId(), user.id()))
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
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!canView(listId, user.id()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Non hai accesso a questa lista.");
        return ResponseEntity.ok(service.getActivitiesByListId(listId).stream().map(this::toDTO).toList());
    }

    @DeleteMapping("/list/{listId}/activity/{activityId}")
    public ResponseEntity<?> removeActivity(@PathVariable Long listId, @PathVariable Long activityId,
                                            @AuthenticationPrincipal AuthUser user) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!canEdit(listId, user.id()))
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
                .title(activity == null ? "Attività #" + association.getActivityId() : activity.getTitle())
                .city(activity == null ? "" : activity.getCity())
                .category(activity == null || activity.getCategory() == null ? "" : activity.getCategory().name())
                .addedAt(association.getAddedAt()).build();
    }

    private boolean canView(Long listId, Long userId) {
        FavoriteList list = activeList(listId);
        return list != null && (list.getUser().getId().equals(userId)
                || list.getVisibility() == FavoriteList.Visibility.SHARED
                && sharings.existsByFavoriteList_IdAndUser_Id(listId, userId));
    }
    private boolean canEdit(Long listId, Long userId) {
        FavoriteList list = activeList(listId);
        if (list == null) return false;
        if (list.getUser().getId().equals(userId)) return true;
        if (list.getVisibility() != FavoriteList.Visibility.SHARED) return false;
        return sharings.findByFavoriteListId(listId).stream().anyMatch(s ->
                s.getUser().getId().equals(userId) && s.getPermission() == ListSharing.SharingPermission.EDIT);
    }
    private FavoriteList activeList(Long id) {
        if (id == null) return null;
        return lists.findById(id).filter(l -> !l.isDeleted()).orElse(null);
    }
}