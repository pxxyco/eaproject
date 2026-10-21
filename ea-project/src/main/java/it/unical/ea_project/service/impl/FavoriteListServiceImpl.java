package it.unical.ea_project.service.impl;

import it.unical.ea_project.domain.FavoriteList;
import it.unical.ea_project.domain.User;
import it.unical.ea_project.repository.FavoriteListRepository;
import it.unical.ea_project.repository.UserRepository;
import it.unical.ea_project.service.FavoriteListService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteListServiceImpl implements FavoriteListService {

    private final FavoriteListRepository favoriteListRepository;
    private final UserRepository userRepository;

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
                .orElseThrow(() -> new IllegalArgumentException("Lista dei preferiti non trovata o eliminata con ID: " + id));

    }

    @Override
    @Transactional
    public FavoriteList updateFavoriteList(Long id, 
                                           Long userId,
                                           String name,
                                           FavoriteList.Visibility visibility) {

        FavoriteList list = favoriteListRepository.findById(id)
                .filter(existing -> !existing.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Lista non trovata o eliminata: " + id));

        if (list.getUser() == null || !list.getUser().getId().equals(userId)) {
            throw new AccessDeniedException(
                    "Non sei autorizzato a modificare questa lista."
            );
        }

        if (name == null || name.isBlank() || name.trim().length() > 80) {
            throw new IllegalArgumentException("Il nome deve contenere da 1 a 80 caratteri.");
        }

        if (visibility == null) {
            throw new IllegalArgumentException("La visibilità è obbligatoria.");
        }

        list.setName(name.trim());
        list.setVisibility(visibility);
        return favoriteListRepository.save(list);
    }

    @Override
    @Transactional
    public void softDeleteList(Long id) {

        FavoriteList favoriteList = favoriteListRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lista dei preferiti non trovata con ID: " + id));
        favoriteList.setDeleted(true);
        favoriteListRepository.save(favoriteList);

    }
}