package it.unical.ea_project.repository.booking;

import it.unical.ea_project.domain.booking.Booking;
import it.unical.ea_project.domain.trip.Trip;
import it.unical.ea_project.domain.activity.Activity;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {
  @Query("select count(b) from Booking b left join b.trip t left join b.activity a where b.user.id=:traveler and b.deletedAt is null and (t.createdBy.id=:organizer or a.createdBy.id=:organizer)")
  long countContactBookings(@Param("traveler") Long traveler, @Param("organizer") Long organizer);

  @EntityGraph(attributePaths = { "trip", "activity" })
  @Query("select b from Booking b where b.user.id=:user and b.deletedAt is null order by b.createdAt desc")
  List<Booking> recommendationHistory(@Param("user") Long user, Pageable page);

  @EntityGraph(attributePaths = { "trip", "activity", "user" })
  List<Booking> findByUserIdAndBookingIdInAndDeletedAtIsNull(Long userId, List<Long> ids);

  /**
   * Serializes cancellation of the same booking so concurrent requests cannot
   * restore seats twice.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT b FROM Booking b WHERE b.bookingId = :bookingId")
  Optional<Booking> findByBookingIdForUpdate(@Param("bookingId") Long bookingId);

  List<Booking> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

  @Query("""
      SELECT b
      FROM Booking b
      LEFT JOIN b.trip t
      LEFT JOIN b.activity a
      WHERE b.deletedAt IS NULL
      AND ((t.createdBy.id = :organizerId)
        OR (a.createdBy.id = :organizerId))
      ORDER BY b.createdAt DESC
      """)
  List<Booking> findActiveBookingsForOrganizer(@Param("organizerId") Long organizerId);

  @Query("select count(b),coalesce(sum(b.seats),0),coalesce(sum(b.price),0) from Booking b left join b.trip t left join b.activity a where b.deletedAt is null and b.bookingStatus<>:cancelled and ((t.createdBy.id=:organizer and t.deletedAt is null and t.status<>:tripCancelled) or (a.createdBy.id=:organizer and a.deletedAt is null and a.status<>:activityCancelled))")
  List<Object[]> organizerReservationTotals(@Param("organizer") Long organizer,
      @Param("cancelled") Booking.BookingStatus cancelled,
      @Param("tripCancelled") Trip.TripStatus tripCancelled,
      @Param("activityCancelled") Activity.Status activityCancelled);

  // soft delete tramite campo deleted_at
  @Modifying
  @Transactional
  @Query("UPDATE Booking b SET b.deletedAt = CURRENT_TIMESTAMP WHERE b.bookingId = :id")
  void softDeleteById(Long id);

}
