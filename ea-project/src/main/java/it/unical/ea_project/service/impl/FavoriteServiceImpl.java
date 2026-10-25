package it.unical.ea_project.service.impl;

import it.unical.ea_project.domain.Activity;
import it.unical.ea_project.domain.Favorite;
import it.unical.ea_project.domain.Trip;
import it.unical.ea_project.domain.User;
import it.unical.ea_project.dto.FavoriteDTO;
import it.unical.ea_project.mapper.FavoriteMapper;
import it.unical.ea_project.repository.ActivityRepository;
import it.unical.ea_project.repository.FavoriteRepository;
import it.unical.ea_project.repository.TripRepository;
import it.unical.ea_project.repository.UserRepository;
import it.unical.ea_project.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final TripRepository tripRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isActivityFavorite(Long userId, Long activityId) {
        return favoriteRepository.existsByUser_IdAndActivityId(userId, activityId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTripFavorite(Long userId, Long tripId) {
        return favoriteRepository.existsByUser_IdAndTripId(userId, tripId);
    }

    @Override
    @Transactional
    public void addActivity(Long userId, Long activityId) {

        Activity activity = activityRepository.findByActivityIdAndDeletedAtIsNull(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Attività non trovata o non disponibile."));
        if (activity.getStatus() != Activity.Status.PUBLISHED) {
            throw new IllegalArgumentException("È possibile aggiungere ai preferiti solo attività pubblicate.");
        }

        if (favoriteRepository.existsByUser_IdAndActivityId(userId, activityId)) {
            return;
        }

        Favorite favorite = new Favorite();
        favorite.setUser(getUser(userId));
        favorite.setActivityId(activityId);
        favorite.setAddedAt(LocalDateTime.now());
        favoriteRepository.save(favorite);

    }

    @Override
    @Transactional
    public void addTrip(Long userId, Long tripId) {

        Trip trip = tripRepository.findByTripIdAndDeletedAtIsNull(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Viaggio non trovato o non disponibile."));
        if (trip.getStatus() != Trip.TripStatus.PUBLISHED) {
            throw new IllegalArgumentException("È possibile aggiungere ai preferiti solo viaggi pubblicati.");
        }

        if (favoriteRepository.existsByUser_IdAndTripId(userId, tripId)) {
            return;
        }

        Favorite favorite = new Favorite();
        favorite.setUser(getUser(userId));
        favorite.setTripId(tripId);
        favorite.setAddedAt(LocalDateTime.now());
        favoriteRepository.save(favorite);

    }

    @Override
    @Transactional
    public void removeActivity(Long userId, Long activityId) {
        favoriteRepository.findByUser_IdAndActivityId(userId, activityId)
                .ifPresent(favoriteRepository::delete);
    }

    @Override
    @Transactional
    public void removeTrip(Long userId, Long tripId) {
        favoriteRepository.findByUser_IdAndTripId(userId, tripId)
                .ifPresent(favoriteRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FavoriteDTO> getFavoritesByUser(Long userId) {
        return favoriteRepository.findByUser_IdOrderByAddedAtDesc(userId).stream()
                .map(this::toDTO).toList();
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato con ID: " + userId));
    }

    private FavoriteDTO toDTO(Favorite favorite) {
        if (favorite.getActivityId() != null) {
            Activity activity = activityRepository.findByActivityIdAndDeletedAtIsNull(favorite.getActivityId()).orElse(null);
            return FavoriteMapper.toActivityDTO(favorite, activity);
        }
        Trip trip = tripRepository.findByTripIdAndDeletedAtIsNull(favorite.getTripId()).orElse(null);
        return FavoriteMapper.toTripDTO(favorite, trip);
    }

}