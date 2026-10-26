package it.unical.ea_project.controller.favorites;

import it.unical.ea_project.domain.favorites.FavoriteList;
import it.unical.ea_project.domain.favorites.ListSharing;
import it.unical.ea_project.domain.favorites.ListTrips;
import it.unical.ea_project.domain.notification.Notification;
import it.unical.ea_project.domain.trip.Stage;
import it.unical.ea_project.domain.user.User;
import it.unical.ea_project.repository.favorites.FavoriteListRepository;
import it.unical.ea_project.repository.favorites.ListSharingRepository;
import it.unical.ea_project.repository.favorites.ListTripsRepository;
import it.unical.ea_project.repository.trip.TripRepository;
import it.unical.ea_project.repository.user.UserRepository;
import it.unical.ea_project.security.AuthUser;
import it.unical.ea_project.service.favorites.ListSharingService;
import it.unical.ea_project.service.notification.NotificationService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/list-sharings")
@RequiredArgsConstructor
public class ListSharingController {

    private final ListSharingService listSharingService;
    private final FavoriteListRepository lists;
    private final ListSharingRepository sharings;
    private final UserRepository users;
    private final NotificationService notifications;
    private final ListTripsRepository listTrips;
    private final TripRepository trips;

    // Per compatibilità accetta identifier (nuovo client), username (client
    // precedente) oppure email.
    // userId resta interno per modifiche/revoche di condivisioni già accettate.
    public record ShareListRequest(Long listId, String identifier, String username, String email,
            Long userId, ListSharing.SharingPermission permission) {
    }

    public record UserSearchResponse(String username, String email) {
    }

    public record SharingResponse(Long listId, String listName, Long userId, String username, String email,
            ListSharing.SharingPermission permission, LocalDateTime sharedAt) {
        static SharingResponse from(ListSharing sharing) {
            return new SharingResponse(sharing.getFavoriteList().getId(), sharing.getFavoriteList().getName(),
                    sharing.getUser().getId(), sharing.getUser().getUsername(), sharing.getUser().getEmail(),
                    sharing.getPermission(), sharing.getSharedAt());
        }
    }

    public record SharedStageResponse(String title, String category, String locationName, String city,
            String country, Integer day, Integer orderInDay,
            LocalTime startTime, LocalTime endTime, String description) {
    }

    public record SharedTripResponse(Long tripId, String title, String destinationCity,
            String destinationCountry, LocalDate startDate, LocalDate endDate,
            String organizerUsername, List<SharedStageResponse> stages) {
    }

    public record SharedListResponse(Long listId, String listName, String organizerUsername,
            ListSharing.SharingPermission permission, LocalDateTime sharedAt,
            List<SharedTripResponse> trips) {
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> inviteToList(@RequestBody ShareListRequest request,
            @AuthenticationPrincipal AuthUser currentUser) {
        String identifier = request == null ? null
                : firstNonBlank(request.identifier(), request.username(), request.email());
        if (request == null || request.listId() == null || identifier == null) {
            return ResponseEntity.badRequest().body("Inserisci lo username o l'email dell'utente da invitare.");
        }
        FavoriteList list = lists.findById(request.listId()).filter(l -> !l.isDeleted()).orElse(null);
        if (list == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Lista non trovata.");
        if (!list.getUser().getId().equals(currentUser.id()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        if (list.getVisibility() != FavoriteList.Visibility.SHARED) {
            return ResponseEntity.badRequest().body("Rendi la lista condivisa prima di invitare altri utenti.");
        }
        String normalizedIdentifier = identifier.trim();
        User recipient = users.findByUsernameIgnoreCase(normalizedIdentifier)
                .or(() -> users.findByEmailIgnoreCase(normalizedIdentifier))
                .filter(candidate -> candidate.getDeletedAt() == null)
                .orElse(null);
        if (recipient == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Nessun utente attivo trovato con questo username o email.");
        }
        if (recipient.getId().equals(currentUser.id())) {
            return ResponseEntity.badRequest().body("Non puoi invitare te stesso.");
        }
        if (sharings.existsByFavoriteList_IdAndUser_Id(list.getId(), recipient.getId())) {
            return ResponseEntity.badRequest().body("Questo utente ha già accesso alla lista.");
        }
        if (notifications.hasPendingListInvitation(recipient.getId(), list.getId())) {
            return ResponseEntity.badRequest().body("Esiste già un invito in attesa per questo utente.");
        }

        User inviter = users.findById(currentUser.id()).orElseThrow();
        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setType(Notification.NotificationType.LIST_INVITATION);
        notification.setTitle("Invito a una lista condivisa");
        notification.setMessage("@" + inviter.getUsername() + " ti ha invitato alla lista «" + list.getName() + "».");
        notification.setRead(false);
        notification.setRelatedEntityId(list.getId());
        notification.setRelatedEntityType("FAVORITE_LIST");
        notification.setActionData(
                (request.permission() == null ? ListSharing.SharingPermission.VIEW : request.permission()).name());
        notifications.createNotification(notification);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Invito inviato a @" + recipient.getUsername(),
                "username", recipient.getUsername(),
                "email", recipient.getEmail()));
    }

    /**
     * Ricerca persone attive per username o email, limitata alla gestione di una
     * lista posseduta
     * dall'utente autenticato. Non espone un elenco globale degli utenti senza una
     * lista autorizzata.
     */
    @GetMapping("/search-users")
    @Transactional(readOnly = true)
    public ResponseEntity<?> searchUsersForSharing(@RequestParam Long listId,
            @RequestParam String q,
            @AuthenticationPrincipal AuthUser currentUser) {
        if (listId == null || !owns(listId, currentUser.id())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String query = q == null ? "" : q.trim();
        if (query.length() < 2)
            return ResponseEntity.ok(List.of());
        List<UserSearchResponse> result = users.searchActiveUsers(query, currentUser.id(), PageRequest.of(0, 10))
                .stream()
                .filter(candidate -> !sharings.existsByFavoriteList_IdAndUser_Id(listId, candidate.getId()))
                .filter(candidate -> !notifications.hasPendingListInvitation(candidate.getId(), listId))
                .map(candidate -> new UserSearchResponse(candidate.getUsername(), candidate.getEmail()))
                .toList();
        return ResponseEntity.ok(result);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank())
                return value.trim();
        }
        return null;
    }

    @GetMapping("/list/{listId}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getSharingsByList(@PathVariable Long listId,
            @AuthenticationPrincipal AuthUser user) {
        if (!owns(listId, user.id()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity
                .ok(listSharingService.getSharingsByListId(listId).stream().map(SharingResponse::from).toList());
    }

    @GetMapping("/mine")
    @Transactional(readOnly = true)
    public ResponseEntity<List<SharingResponse>> getMySharedLists(@AuthenticationPrincipal AuthUser user) {
        return ResponseEntity
                .ok(listSharingService.getSharedListsForUser(user.id()).stream().map(SharingResponse::from).toList());
    }

    /**
     * Elenca solo le liste accettate e condivise con l'utente autenticato,
     * includendo viaggi e tappe.
     */
    @GetMapping("/shared-with-me")
    @Transactional(readOnly = true)
    public ResponseEntity<List<SharedListResponse>> getSharedListsWithDetails(
            @AuthenticationPrincipal AuthUser user) {
        List<SharedListResponse> result = listSharingService.getSharedListsForUser(user.id()).stream()
                .filter(sharing -> !sharing.getFavoriteList().isDeleted())
                .map(sharing -> {
                    FavoriteList favoriteList = sharing.getFavoriteList();
                    List<SharedTripResponse> sharedTrips = listTrips.findByFavoriteListId(favoriteList.getId())
                            .stream()
                            .map(ListTrips::getTripId)
                            .map(tripId -> trips.findByTripIdAndDeletedAtIsNull(tripId).orElse(null))
                            .filter(Objects::nonNull)
                            .map(trip -> new SharedTripResponse(
                                    trip.getTripId(), trip.getTitle(), trip.getDestinationCity(),
                                    trip.getDestinationCountry(), trip.getStartDate(), trip.getEndDate(),
                                    trip.getCreatedBy() == null ? null : trip.getCreatedBy().getUsername(),
                                    trip.getStages().stream()
                                            .sorted(Comparator
                                                    .comparing(Stage::getDay, Comparator.nullsLast(Integer::compareTo))
                                                    .thenComparing(Stage::getOrderInDay,
                                                            Comparator.nullsLast(Integer::compareTo)))
                                            .map(stage -> new SharedStageResponse(
                                                    stage.getTitle(),
                                                    stage.getCategory() == null ? null : stage.getCategory().name(),
                                                    stage.getLocationName(), stage.getCity(), stage.getCountry(),
                                                    stage.getDay(), stage.getOrderInDay(), stage.getStartTime(),
                                                    stage.getEndTime(), stage.getDescription()))
                                            .toList()))
                            .toList();
                    return new SharedListResponse(favoriteList.getId(), favoriteList.getName(),
                            favoriteList.getUser() == null ? null : favoriteList.getUser().getUsername(),
                            sharing.getPermission(), sharing.getSharedAt(), sharedTrips);
                })
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/user/{userId}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getSharedListsForUser(@PathVariable Long userId, @AuthenticationPrincipal AuthUser user) {
        if (!user.id().equals(userId))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity
                .ok(listSharingService.getSharedListsForUser(userId).stream().map(SharingResponse::from).toList());
    }

    @PutMapping("/permission")
    public ResponseEntity<Void> updatePermission(@RequestBody ShareListRequest request,
            @AuthenticationPrincipal AuthUser user) {
        if (request == null || request.listId() == null || request.userId() == null)
            return ResponseEntity.badRequest().build();
        if (!owns(request.listId(), user.id()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        listSharingService.updatePermission(request.listId(), request.userId(), request.permission());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/list/{listId}/user/{userId}")
    public ResponseEntity<Void> revokeSharing(@PathVariable Long listId, @PathVariable Long userId,
            @AuthenticationPrincipal AuthUser user) {
        if (!owns(listId, user.id()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        listSharingService.revokeSharing(listId, userId);
        return ResponseEntity.noContent().build();
    }

    private boolean owns(Long listId, Long userId) {
        return lists.findById(listId).filter(l -> !l.isDeleted())
                .map(l -> l.getUser().getId().equals(userId)).orElse(false);
    }
}
