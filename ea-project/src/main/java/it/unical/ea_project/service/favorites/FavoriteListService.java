package it.unical.ea_project.service.favorites;

import it.unical.ea_project.domain.favorites.FavoriteList;
import java.util.List;

public interface FavoriteListService {

    FavoriteList createFavoriteList(Long userId, String name, FavoriteList.Visibility visibility);

    List<FavoriteList> getActiveListsByUser(Long userId);

    FavoriteList getListById(Long id);

    FavoriteList updateFavoriteList(Long id, Long userId, String name, FavoriteList.Visibility visibility);

    void softDeleteList(Long id);

}
