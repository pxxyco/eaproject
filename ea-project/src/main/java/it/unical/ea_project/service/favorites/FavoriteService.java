package it.unical.ea_project.service.favorites;

import it.unical.ea_project.dto.favorites.FavoriteDTO;
import java.util.List;

public interface FavoriteService {

    boolean isActivityFavorite(Long userId, Long activityId);
    boolean isTripFavorite(Long userId, Long tripId);

    void addActivity(Long userId, Long activityId);
    void addTrip(Long userId, Long tripId);

    void removeActivity(Long userId, Long activityId);
    void removeTrip(Long userId, Long tripId);

    List<FavoriteDTO> getFavoritesByUser(Long userId);
}
