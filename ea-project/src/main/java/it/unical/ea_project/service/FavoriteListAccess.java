package it.unical.ea_project.service;

import it.unical.ea_project.domain.FavoriteList;
import it.unical.ea_project.domain.ListSharing;
import it.unical.ea_project.repository.FavoriteListRepository;
import it.unical.ea_project.repository.ListSharingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FavoriteListAccess {
    private final FavoriteListRepository lists;
    private final ListSharingRepository shares;

    public FavoriteList active(Long id) {
        return id == null ? null : lists.findById(id).filter(list -> !list.isDeleted()).orElse(null);
    }

    public boolean canView(Long id, Long user) {
        return canView(active(id), user);
    }

    public boolean canView(FavoriteList list, Long user) {
        return list != null && !list.isDeleted() && user != null && (list.getUser().getId().equals(user)
                || list.getVisibility() == FavoriteList.Visibility.SHARED
                        && shares.existsByFavoriteList_IdAndUser_Id(list.getId(), user));
    }

    public boolean canEdit(Long id, Long user) {
        return canEdit(active(id), user);
    }

    public boolean canEdit(FavoriteList list, Long user) {
        return list != null && !list.isDeleted() && user != null && (list.getUser().getId().equals(user)
                || list.getVisibility() == FavoriteList.Visibility.SHARED
                        && shares.existsByFavoriteList_IdAndUser_IdAndPermission(list.getId(), user,
                                ListSharing.SharingPermission.EDIT));
    }
}
