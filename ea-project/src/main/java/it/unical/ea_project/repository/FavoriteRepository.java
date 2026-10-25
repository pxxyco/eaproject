package it.unical.ea_project.repository;

import it.unical.ea_project.domain.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByUser_IdAndActivityId(long user_Id, long activity_Id);
    boolean existsByUser_IdAndTripId(long user_Id, long trip_Id);

    Optional<Favorite> findByUser_IdAndActivityId(Long userId, Long activityId);
    Optional<Favorite> findByUser_IdAndTripId(Long userId, Long tripId);

    List<Favorite> findByUser_IdOrderByAddedAtDesc(Long userId);
}
