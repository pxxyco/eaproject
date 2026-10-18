package it.unical.ea_project.repository;


import it.unical.ea_project.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review,Long> {

    List<Review> findAllByUserId(Long userId);

    List<Review> findAllByTripTripIdOrderByCreatedAtDesc(Long tripId);

    List<Review> findAllByActivityActivityIdOrderByCreatedAtDesc(Long activityId);

    @Modifying
    @Transactional
    @Query("UPDATE Review r SET r.deleted = true WHERE r.reviewId = :id")
    void softDeleteById(Long id);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.activity.activityId = :activityId and r.deleted = false")
    Double averageRatingByActivity(Long activityId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.trip.tripId = :tripId and r.deleted = false")
    Double averageRatingByTrip(Long tripId);
}
