package it.unical.ea_project.controller;

import it.unical.ea_project.domain.FavoriteList;
import it.unical.ea_project.dto.FavoriteListDTO;
import it.unical.ea_project.repository.ListSharingRepository;
import it.unical.ea_project.security.AuthUser;
import it.unical.ea_project.service.FavoriteListService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorite-lists")
@RequiredArgsConstructor
public class FavoriteListController {

    private final FavoriteListService favoriteListService;
    private final ListSharingRepository sharingRepository;

    public record FavoriteListRequest(String name, FavoriteList.Visibility visibility) {}

    @PostMapping
    public ResponseEntity<FavoriteListDTO> createList(
            @AuthenticationPrincipal AuthUser user,
            @RequestBody FavoriteListRequest request) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (request == null
                || request.name() == null
                || request.name().isBlank()
                || request.name().trim().length() > 80) {
            return ResponseEntity.badRequest().build();
        }

        FavoriteList created = favoriteListService.createFavoriteList(
                user.id(),
                request.name(),
                request.visibility()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toDTO(created));

    }

    @GetMapping("/mine")
    public ResponseEntity<List<FavoriteListDTO>> getMyLists(
            @AuthenticationPrincipal AuthUser user) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<FavoriteListDTO> lists =
                favoriteListService.getActiveListsByUser(user.id())
                        .stream()
                        .map(this::toDTO)
                        .toList();

        return ResponseEntity.ok(lists);

    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<FavoriteListDTO>> getListsByUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal AuthUser user) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!user.id().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<FavoriteListDTO> lists =
                favoriteListService.getActiveListsByUser(userId)
                        .stream()
                        .map(this::toDTO)
                        .toList();

        return ResponseEntity.ok(lists);

    }

    @GetMapping("/{id}")
    public ResponseEntity<FavoriteListDTO> getListById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser user) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        FavoriteList list = favoriteListService.getListById(id);

        if (!canView(list, user.id())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(toDTO(list));

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteList(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser user) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        FavoriteList list = favoriteListService.getListById(id);

        if (!list.getUser().getId().equals(user.id())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        favoriteListService.softDeleteList(id);

        return ResponseEntity.noContent().build();

    }

    private FavoriteListDTO toDTO(FavoriteList list) {

        return FavoriteListDTO.builder()
                .id(list.getId())
                .userId(list.getUser().getId())
                .name(list.getName())
                .visibility(
                        list.getVisibility() != null
                                ? list.getVisibility().name()
                                : "PRIVATE"
                )
                .deleted(list.isDeleted())
                .build();

    }

    private boolean canView(FavoriteList list, Long userId) {

        if (list.getUser().getId().equals(userId)) {
            return true;
        }

        return sharingRepository.existsByFavoriteList_IdAndUser_Id(
                list.getId(),
                userId
        );

    }
}
