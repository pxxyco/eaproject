package it.unical.ea_project.service.impl;

import it.unical.ea_project.domain.FavoriteList;
import it.unical.ea_project.domain.ListSharing;
import it.unical.ea_project.domain.User;
import it.unical.ea_project.repository.FavoriteListRepository;
import it.unical.ea_project.repository.ListSharingRepository;
import it.unical.ea_project.repository.UserRepository;
import it.unical.ea_project.service.ListSharingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ListSharingServiceImpl implements ListSharingService {

    private final ListSharingRepository listSharingRepository;
    private final FavoriteListRepository favoriteListRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ListSharing shareList(Long listId, Long userId, ListSharing.SharingPermission permission) {

        if (listId == null || userId == null) {
            throw new IllegalArgumentException("ID della lista e dell'utente obbligatori.");
        }

        FavoriteList favoriteList = favoriteListRepository.lockActive(listId)
                .filter(list -> !list.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Lista non trovata o eliminata con ID: " + listId));

        if (favoriteList.getVisibility() != FavoriteList.Visibility.SHARED) {
            throw new IllegalArgumentException("La lista non è condivisa. L'invito non è più valido.");
        }

        User user = userRepository.findById(userId)
                .filter(candidate -> candidate.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato con ID: " + userId));

        if (favoriteList.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Non è necessario condividere una lista con il suo proprietario.");
        }

        if (listSharingRepository.existsByFavoriteList_IdAndUser_Id(listId, userId)) {
            throw new IllegalStateException(
                    "La lista è già condivisa con questo utente. Modifica il permesso esistente.");
        }

        ListSharing sharing = new ListSharing();
        sharing.setFavoriteList(favoriteList);
        sharing.setUser(user);
        sharing.setPermission(permission != null ? permission : ListSharing.SharingPermission.VIEW);
        sharing.setSharedAt(LocalDateTime.now());
        return listSharingRepository.save(sharing);

    }

    @Override
    @Transactional(readOnly = true)
    public List<ListSharing> getSharingsByListId(Long listId) {

        FavoriteList list = favoriteListRepository.findById(listId)
                .filter(candidate -> !candidate.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Lista non trovata o eliminata con ID: " + listId));
        return listSharingRepository.findByFavoriteListId(list.getId());

    }

    @Override
    @Transactional(readOnly = true)
    public List<ListSharing> getSharedListsForUser(Long userId) {

        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("Utente non trovato con ID: " + userId);
        }

        return listSharingRepository.findByUser_Id(userId)
                .stream()
                .filter(sharing -> !sharing.getFavoriteList().isDeleted()
                        && sharing.getFavoriteList().getVisibility() == FavoriteList.Visibility.SHARED)
                .toList();

    }

    @Override
    @Transactional
    public void updatePermission(Long listId, Long userId, ListSharing.SharingPermission newPermission) {
        favoriteListRepository.lockActive(listId)
                .orElseThrow(() -> new IllegalArgumentException("Lista non disponibile."));

        if (newPermission == null) {
            throw new IllegalArgumentException("Il permesso è obbligatorio.");
        }

        ListSharing.ListSharingId id = new ListSharing.ListSharingId(listId, userId);
        ListSharing sharing = listSharingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Nessuna condivisione esistente per questa coppia lista-utente."));
        sharing.setPermission(newPermission);
        listSharingRepository.save(sharing);

    }

    @Override
    @Transactional
    public void revokeSharing(Long listId, Long userId) {
        favoriteListRepository.lockActive(listId)
                .orElseThrow(() -> new IllegalArgumentException("Lista non disponibile."));

        ListSharing.ListSharingId id = new ListSharing.ListSharingId(listId, userId);
        ListSharing sharing = listSharingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nessuna condivisione esistente da revocare."));
        listSharingRepository.delete(sharing);

    }
}