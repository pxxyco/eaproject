package it.unical.ea_project.controller;

import it.unical.ea_project.domain.Activity;
import it.unical.ea_project.dto.ActivityDTO;
import it.unical.ea_project.security.AuthUser;
import it.unical.ea_project.dto.home.ActivityHomeDTO;
import it.unical.ea_project.dto.home.ActivitySuggestionDTO;
import it.unical.ea_project.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @PostMapping
    public ResponseEntity<ActivityDTO> createActivity(
            @RequestBody ActivityDTO request,
            @AuthenticationPrincipal AuthUser user
    ) {
        Activity activity = new Activity();
        activity.setTitle(request.getTitle());
        activity.setDescription(request.getDescription());
        activity.setCategory(request.getCategory() == null ? null : Activity.Category.valueOf(request.getCategory()));
        activity.setLatitude(request.getLatitude());
        activity.setLongitude(request.getLongitude());
        activity.setPlaceName(request.getPlaceName());
        activity.setCity(request.getCity());
        activity.setStartDate(request.getStartDate());
        activity.setEndDate(request.getEndDate());
        activity.setDurationMinutes(request.getDurationMinutes());
        activity.setPrice(request.getPrice());
        activity.setMaxSeats(request.getMaxSeats());
        activity.setAvailableSeats(request.getMaxSeats());
        activity.setStatus(Activity.Status.PUBLISHED);
        activity.setNotes(request.getNotes());

        Activity created = activityService.createActivity(activity, user.id());
        return activityService.getActivityDtoById(created.getActivityId())
                .map(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto))
                .orElseThrow(() -> new IllegalStateException("L'attività creata non è stata recuperata."));
    }

    @GetMapping("/organizer/mine")
    public ResponseEntity<List<ActivityDTO>> getOrganizerActivities(@AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(activityService.getActivityDtosByCreator(user.id()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteActivity(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser user
    ) {
        ActivityDTO activity = activityService.getActivityDtoById(id)
                .orElse(null);
        if (activity == null) {
            return ResponseEntity.notFound().build();
        }
        if (activity.getCreatedByUserId() == null || !activity.getCreatedByUserId().equals(user.id())) {
            throw new AccessDeniedException("L'attività non appartiene all'organizzatore autenticato.");
        }
        activityService.deleteActivity(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ActivityDTO> updateActivity(
            @PathVariable Long id,
            @RequestBody ActivityDTO request,
            @AuthenticationPrincipal AuthUser user
    ) {
        Activity existing = activityService.getActivityById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        if (existing.getCreatedBy() == null || !existing.getCreatedBy().getId().equals(user.id())) {
            throw new AccessDeniedException("L'attività non appartiene all'organizzatore autenticato.");
        }

        Activity details = new Activity();
        details.setTitle(request.getTitle());
        details.setDescription(request.getDescription());
        details.setCategory(request.getCategory() == null ? null : Activity.Category.valueOf(request.getCategory()));
        details.setLatitude(request.getLatitude());
        details.setLongitude(request.getLongitude());
        details.setPlaceName(request.getPlaceName());
        details.setCity(request.getCity());
        details.setStartDate(request.getStartDate());
        details.setEndDate(request.getEndDate());
        details.setDurationMinutes(request.getDurationMinutes());
        details.setPrice(request.getPrice());
        details.setMaxSeats(request.getMaxSeats());
        details.setAvailableSeats(request.getAvailableSeats());
        details.setStatus(request.getStatus() == null ? existing.getStatus() : Activity.Status.valueOf(request.getStatus()));
        details.setNotes(request.getNotes());

        Activity updated = activityService.updateActivity(id, details);
        return activityService.getActivityDtoById(updated.getActivityId())
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new IllegalStateException("L'attività aggiornata non è stata recuperata."));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActivityDTO> getActivityById(
            @PathVariable Long id
    ) {
        return activityService.getActivityDtoById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/top")
    public ResponseEntity<List<ActivityDTO>> getTopActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size
    ) {
        return ResponseEntity.ok(
                activityService.getTopActivityDtos(
                        page,
                        size
                )
        );
    }

    @GetMapping("/search")
    public ResponseEntity<List<ActivityHomeDTO>> searchActivities(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Activity.Category category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size
    ) {
        return ResponseEntity.ok(
                activityService.searchActivityHomeDtos(
                        query,
                        category,
                        date,
                        page,
                        size
                )
        );
    }

    @GetMapping("/suggest")
    public ResponseEntity<List<ActivitySuggestionDTO>> suggestActivities(
            @RequestParam String query,
            @RequestParam(defaultValue = "6") int limit
    ) {
        return ResponseEntity.ok(
                activityService.getSuggestions(
                        query,
                        limit
                )
        );
    }

    @GetMapping("/top/cards")
    public ResponseEntity<List<ActivityHomeDTO>> getTopActivityCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size
    ) {
        return ResponseEntity.ok(
                activityService.getTopActivityHomeDtos(
                        page,
                        size
                )
        );
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<ActivityDTO>> getActivitiesByCategory(
            @PathVariable Activity.Category category
    ) {
        return ResponseEntity.ok(
                activityService.getActivityDtosByCategory(
                        category
                )
        );
    }
}