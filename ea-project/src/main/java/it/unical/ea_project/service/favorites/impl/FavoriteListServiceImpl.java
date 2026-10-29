package it.unical.ea_project.service.favorites.impl;

import it.unical.ea_project.domain.favorites.FavoriteList;
import it.unical.ea_project.domain.notification.Notification;
import it.unical.ea_project.domain.user.User;
import it.unical.ea_project.repository.favorites.FavoriteListRepository;
import it.unical.ea_project.repository.favorites.ListSharingRepository;
import it.unical.ea_project.repository.notification.NotificationRepository;
import it.unical.ea_project.repository.user.UserRepository;
import it.unical.ea_project.service.favorites.FavoriteListService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FavoriteListServiceImpl implements FavoriteListService {

    private final FavoriteListRepository favoriteListRepository;
    private final UserRepository userRepository;
    private final ListSharingRepository sharingRepository;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public FavoriteList createFavoriteList(Long userId, String name, FavoriteList.Visibility visibility) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato con ID: " + userId));

        FavoriteList favoriteList = new FavoriteList();
        if (name == null || name.isBlank() || name.trim().length() > 80) {
            throw new IllegalArgumentException("Il nome della lista deve contenere da 1 a 80 caratteri.");
        }

        favoriteList.setName(name.trim());
        favoriteList.setUser(user);
        favoriteList.setVisibility(visibility != null ? visibility : FavoriteList.Visibility.PRIVATE);
        favoriteList.setDeleted(false);

        return favoriteListRepository.save(favoriteList);

    }

    @Override
    @Transactional(readOnly = true)
    public List<FavoriteList> getActiveListsByUser(Long userId) {
        return favoriteListRepository.findByUser_IdAndDeletedFalse(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public FavoriteList getListById(Long id) {

        return favoriteListRepository.findById(id)
                .filter(list -> !list.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Lista dei preferiti non trovata o eliminata con ID: " + id));

    }

    @Override
    @Transactional
    public FavoriteList updateFavoriteList(Long id,
            Long userId,
            String name,
            FavoriteList.Visibility visibility) {

        FavoriteList list = favoriteListRepository.lockActive(id)
                .filter(existing -> !existing.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Lista non trovata o eliminata: " + id));

        if (list.getUser() == null || !list.getUser().getId().equals(userId)) {
            throw new AccessDeniedException(
                    "Non sei autorizzato a modificare questa lista.");
        }

        if (name == null || name.isBlank() || name.trim().length() > 80) {
            throw new IllegalArgumentException("Il nome deve contenere da 1 a 80 caratteri.");
        }

        if (visibility == null) {
            throw new IllegalArgumentException("La visibilità è obbligatoria.");
        }

        list.setName(name.trim());
        if (visibility != FavoriteList.Visibility.SHARED) {
            sharingRepository.deleteAll(sharingRepository.findByFavoriteListId(id));
            notificationRepository.invalidateListInvitations(id,
                    Notification.NotificationType.LIST_INVITATION,
                    Notification.NotificationType.NOTICE);
        }
        list.setVisibility(visibility);
        return favoriteListRepository.save(list);
    }

    @Override
    @Transactional
    public void softDeleteList(Long id) {

        FavoriteList favoriteList = favoriteListRepository.lockActive(id)
                .orElseThrow(() -> new IllegalArgumentException("Lista dei preferiti non trovata con ID: " + id));
        favoriteList.setDeleted(true);
        favoriteListRepository.save(favoriteList);

    }
}
