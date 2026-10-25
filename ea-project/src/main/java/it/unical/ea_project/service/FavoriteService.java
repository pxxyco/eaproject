package it.unical.ea_project.service;

import it.unical.ea_project.dto.FavoriteDTO;

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