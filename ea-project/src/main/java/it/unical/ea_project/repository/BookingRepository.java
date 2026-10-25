package it.unical.ea_project.repository;

import it.unical.ea_project.domain.Booking;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
  @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "trip", "activity" })
  @Query("select b from Booking b where b.user.id=:user and b.deletedAt is null order by b.createdAt desc")
  List<Booking> recommendationHistory(@Param("user") Long user, org.springframework.data.domain.Pageable page);

  List<Booking> findByUserId(Long userId);

  @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "trip", "activity", "user" })
  List<Booking> findByUserIdAndBookingIdInAndDeletedAtIsNull(Long userId, List<Long> ids);

  /**
   * Serializes cancellation of the same booking so concurrent requests cannot
   * restore seats twice.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT b FROM Booking b WHERE b.bookingId = :bookingId")
  Optional<Booking> findByBookingIdForUpdate(@Param("bookingId") Long bookingId);

  List<Booking> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

  List<Booking> findByBookingStatus(Booking.BookingStatus status);

  List<Booking> findByBookingTypeAndBookingStatus(Booking.BookingType type, Booking.BookingStatus status);

  @Query("""
      SELECT b
      FROM Booking b
      LEFT JOIN b.trip t
      LEFT JOIN b.activity a
      WHERE b.deletedAt IS NULL
      AND ((t.createdBy.id = :organizerId AND t.deletedAt IS NULL)
        OR (a.createdBy.id = :organizerId AND a.deletedAt IS NULL))
      ORDER BY b.createdAt DESC
      """)
  List<Booking> findActiveBookingsForOrganizer(@Param("organizerId") Long organizerId);

  // soft delete tramite campo deleted_at
  @Modifying
  @Transactional
  @Query("UPDATE Booking b SET b.deletedAt = CURRENT_TIMESTAMP WHERE b.bookingId = :id")
  void softDeleteById(Long id);

}
