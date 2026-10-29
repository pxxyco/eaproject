package it.unical.ea_project.repository.review;

import it.unical.ea_project.domain.review.Review;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
  @EntityGraph(attributePaths = { "trip", "activity" })
  @Query("select r from Review r where r.user.id=:user and r.deleted=false order by r.createdAt desc")
  List<Review> preferenceReviews(@Param("user") Long user, Pageable page);

  @EntityGraph(attributePaths = { "user", "trip", "activity" })
  @Query("""
      SELECT DISTINCT r FROM Review r, Booking b
      WHERE r.user.id=:user AND b.user.id=:user AND r.deleted=false
        AND b.bookingId IN :ids AND (r.trip=b.trip OR r.activity=b.activity)
      """)
  List<Review> findForBookings(@Param("user") Long user, @Param("ids") List<Long> ids);

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
