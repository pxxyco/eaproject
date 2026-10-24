package it.unical.ea_project.controller;

import it.unical.ea_project.domain.ListSharing;
import it.unical.ea_project.domain.FavoriteList;
import it.unical.ea_project.domain.ListTrips;
import it.unical.ea_project.domain.Stage;
import it.unical.ea_project.domain.Trip;
import it.unical.ea_project.domain.Notification;
import it.unical.ea_project.dto.NotificationDTO;
import it.unical.ea_project.repository.UserRepository;
import it.unical.ea_project.repository.FavoriteListRepository;
import it.unical.ea_project.repository.ListTripsRepository;
import it.unical.ea_project.repository.TripRepository;
import it.unical.ea_project.security.AuthUser;
import it.unical.ea_project.service.ListSharingService;
import it.unical.ea_project.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository users;
    private final ListSharingService listSharingService;
    private final FavoriteListRepository lists;
    private final ListTripsRepository listTrips;
    private final TripRepository trips;

    @GetMapping
    @Transactional(readOnly = true)
    public List<NotificationDTO> getMine(@AuthenticationPrincipal AuthUser auth) {
        var user = users.findById(auth.id()).orElseThrow();
        return notificationService.getAllNotifications(user).stream().map(n -> toDto(n, auth.id())).toList();
    }

    @GetMapping("/unread")
    @Transactional(readOnly = true)
    public List<NotificationDTO> getUnread(@AuthenticationPrincipal AuthUser auth) {
        var user = users.findById(auth.id()).orElseThrow();
        return notificationService.getUnreadNotifications(user).stream().map(n -> toDto(n, auth.id())).toList();
    }

    @GetMapping("/unread/count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal AuthUser auth) {
        var user = users.findById(auth.id()).orElseThrow();
        return Map.of("count", notificationService.countUnread(user));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> readAll(@AuthenticationPrincipal AuthUser auth) {
        var user = users.findById(auth.id()).orElseThrow();
        notificationService.markAllAsRead(user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long notificationId, @AuthenticationPrincipal AuthUser auth) {
        try {
            notificationService.markNotificationAsRead(notificationId, auth.id());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    /** Permette al destinatario di vedere nome, viaggi e tappe della lista prima di accettare l'invito. */
    @GetMapping("/{notificationId}/invitation-preview")
    @Transactional(readOnly = true)
    public ResponseEntity<?> previewInvitation(@PathVariable Long notificationId,
                                               @AuthenticationPrincipal AuthUser auth) {
        Notification notification = notificationService.getNotificationForUser(notificationId, auth.id()).orElse(null);
        if (notification == null) return ResponseEntity.notFound().build();
        if (notification.getType() != Notification.NotificationType.LIST_INVITATION
                || notification.getRelatedEntityId() == null) {
            return ResponseEntity.badRequest().body("Questa notifica non contiene un invito attivo.");
        }
        FavoriteList favoriteList = lists.findById(notification.getRelatedEntityId())
                .filter(list -> !list.isDeleted()).orElse(null);
        if (favoriteList == null || favoriteList.getVisibility() != FavoriteList.Visibility.SHARED) return ResponseEntity.notFound().build();
        List<ListSharingController.SharedTripResponse> sharedTrips = listTrips.findByFavoriteListId(favoriteList.getId())
                .stream().map(ListTrips::getTripId)
                .map(tripId -> trips.findByTripIdAndDeletedAtIsNull(tripId).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(trip -> new ListSharingController.SharedTripResponse(
                        trip.getTripId(), trip.getTitle(), trip.getDestinationCity(), trip.getDestinationCountry(),
                        trip.getStartDate(), trip.getEndDate(),
                        trip.getCreatedBy() == null ? null : trip.getCreatedBy().getUsername(),
                        trip.getStages().stream()
                                .sorted(Comparator.comparing(Stage::getDay, Comparator.nullsLast(Comparator.naturalOrder()))
                                        .thenComparing(Stage::getOrderInDay, Comparator.nullsLast(Comparator.naturalOrder())))
                                .map(stage -> new ListSharingController.SharedStageResponse(
                                        stage.getTitle(), stage.getCategory() == null ? null : stage.getCategory().name(),
                                        stage.getLocationName(), stage.getCity(), stage.getCountry(), stage.getDay(),
                                        stage.getOrderInDay(), stage.getStartTime(), stage.getEndTime(), stage.getDescription()))
                                .toList()))
                .toList();
        ListSharing.SharingPermission permission;
        try {
            permission = ListSharing.SharingPermission.valueOf(
                    notification.getActionData() == null ? "VIEW" : notification.getActionData());
        } catch (IllegalArgumentException ex) {
            permission = ListSharing.SharingPermission.VIEW;
        }
        return ResponseEntity.ok(new ListSharingController.SharedListResponse(
                favoriteList.getId(), favoriteList.getName(), favoriteList.getUser().getUsername(),
                permission, null, sharedTrips));
    }

    @PostMapping("/{notificationId}/accept")
    @Transactional
    public ResponseEntity<?> acceptInvitation(@PathVariable Long notificationId, @AuthenticationPrincipal AuthUser auth) {
        Notification n = notificationService.getNotificationForUser(notificationId, auth.id()).orElse(null);
        if (n == null) return ResponseEntity.notFound().build();
        if (n.getType() != Notification.NotificationType.LIST_INVITATION || n.getRelatedEntityId() == null) {
            return ResponseEntity.badRequest().body("Questa notifica non è un invito a una lista.");
        }
        try {
            ListSharing.SharingPermission permission = ListSharing.SharingPermission.valueOf(
                    n.getActionData() == null ? "VIEW" : n.getActionData());
            listSharingService.shareList(n.getRelatedEntityId(), auth.id(), permission);
            notificationService.resolveListInvitation(notificationId, auth.id(), "Invito accettato", "Hai accettato l'invito alla lista.");
            return ResponseEntity.ok(Map.of("message", "Invito accettato."));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        }
    }

    @PostMapping("/{notificationId}/reject")
    @Transactional
    public ResponseEntity<Void> rejectInvitation(@PathVariable Long notificationId, @AuthenticationPrincipal AuthUser auth) {
        Notification n = notificationService.getNotificationForUser(notificationId, auth.id()).orElse(null);
        if (n == null) return ResponseEntity.notFound().build();
        if (n.getType() != Notification.NotificationType.LIST_INVITATION) return ResponseEntity.badRequest().build();
        notificationService.resolveListInvitation(notificationId, auth.id(), "Invito rifiutato", "Hai rifiutato l'invito alla lista.");
        return ResponseEntity.noContent().build();
    }

    private NotificationDTO toDto(Notification n, Long userId) {
        return NotificationDTO.builder()
                .notificationId(n.getNotificationId())
                .userId(userId)
                .type(n.getType().name())
                .title(n.getTitle())
                .message(n.getMessage())
                .read(Boolean.TRUE.equals(n.getRead()))
                .createdAt(n.getCreatedAt())
                .relatedEntityId(n.getRelatedEntityId())
                .relatedEntityType(n.getRelatedEntityType())
                .actionData(n.getActionData())
                .build();
    }
}
