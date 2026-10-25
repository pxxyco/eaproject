package it.unical.ea_project.mapper;

import it.unical.ea_project.domain.Activity;
import it.unical.ea_project.domain.Favorite;
import it.unical.ea_project.domain.Trip;
import it.unical.ea_project.dto.FavoriteDTO;

public class FavoriteMapper {

    public static FavoriteDTO toActivityDTO(Favorite favorite, Activity activity) {
        return FavoriteDTO.builder()
                .id(favorite.getId())
                .type("ACTIVITY")
                .itemId(favorite.getActivityId())
                .title(activity != null ? activity.getTitle() : "Attività non più disponibile")
                .city(activity != null ? activity.getCity() : "")
                .addedAt(favorite.getAddedAt())
                .build();
    }

    public static FavoriteDTO toTripDTO(Favorite favorite, Trip trip) {
        return FavoriteDTO.builder()
                .id(favorite.getId())
                .type("TRIP")
                .itemId(favorite.getTripId())
                .title(trip != null ? trip.getTitle() : "Viaggio non più disponibile")
                .city(trip != null ? trip.getDestinationCity() : "")
                .addedAt(favorite.getAddedAt())
                .build();
    }

}