package it.unical.ea_project.repository;

import it.unical.ea_project.domain.Booking;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.bookingId = :id AND b.deletedAt IS NULL")
    java.util.Optional<Booking> findForUpdate(@Param("id") Long id);

    List<Booking> findByUserId(Long userId);

    List<Booking> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

    List<Booking> findByBookingStatus(Booking.BookingStatus status);

    List<Booking> findByBookingTypeAndBookingStatus(Booking.BookingType type, Booking.BookingStatus status);

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

    // soft delete tramite campo deleted_at
    @Modifying
    @Transactional
    @Query("UPDATE Booking b SET b.deletedAt = CURRENT_TIMESTAMP WHERE b.bookingId = :id")
    void softDeleteById(Long id);

}
