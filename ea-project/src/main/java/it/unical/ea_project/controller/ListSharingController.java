package it.unical.ea_project.controller;

import it.unical.ea_project.domain.ListSharing;
import it.unical.ea_project.repository.FavoriteListRepository;
import it.unical.ea_project.security.AuthUser;
import it.unical.ea_project.service.ListSharingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/list-sharings")
@RequiredArgsConstructor
public class ListSharingController {

    private final ListSharingService listSharingService;
    private final FavoriteListRepository lists;

    public record ShareListRequest(Long listId, Long userId, ListSharing.SharingPermission permission) {}

    @PostMapping
    public ResponseEntity<?> shareList(@RequestBody ShareListRequest request,
                                       @AuthenticationPrincipal AuthUser user) {

        if (!owns(request.listId(), user.id())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(
                listSharingService.shareList(request.listId(), request.userId(), request.permission())
        );

    }

    @GetMapping("/list/{listId}")
    public ResponseEntity<?> getSharingsByList(@PathVariable Long listId,
                                               @AuthenticationPrincipal AuthUser user) {

        if (!owns(listId, user.id())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(listSharingService.getSharingsByListId(listId));

    }

    @GetMapping("/mine")
    public ResponseEntity<List<ListSharing>> getMySharedLists(@AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(listSharingService.getSharedListsForUser(user.id()));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getSharedListsForUser(@PathVariable Long userId,
                                                   @AuthenticationPrincipal AuthUser user) {

        if (!user.id().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(listSharingService.getSharedListsForUser(userId));
    }

    @PutMapping("/permission")
    public ResponseEntity<Void> updatePermission(@RequestBody ShareListRequest request, 
                                                 @AuthenticationPrincipal AuthUser user) {

        if (!owns(request.listId(), user.id())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        listSharingService.updatePermission(request.listId(), request.userId(), request.permission());
        return ResponseEntity.ok().build();

    }

    @DeleteMapping("/list/{listId}/user/{userId}")
    public ResponseEntity<Void> revokeSharing(@PathVariable Long listId, 
                                              @PathVariable Long userId,
                                              @AuthenticationPrincipal AuthUser user) {

        if (!owns(listId, user.id())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        listSharingService.revokeSharing(listId, userId);
        return ResponseEntity.noContent().build();

    }

    private boolean owns(Long listId, Long userId) {
        return lists.findById(listId).filter(l -> !l.isDeleted())
                .map(l -> l.getUser().getId().equals(userId)).orElse(false);
    }

}