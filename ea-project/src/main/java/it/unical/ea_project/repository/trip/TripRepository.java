package it.unical.ea_project.repository.trip;

import it.unical.ea_project.domain.trip.Trip.TripStatus;
import it.unical.ea_project.domain.trip.Trip;
import it.unical.ea_project.domain.user.User;
import it.unical.ea_project.dto.home.TripSuggestionDTO;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripRepository extends JpaRepository<Trip, Long> {
        @Query("select t from Trip t where t.deletedAt is null and t.status=it.unical.ea_project.domain.trip.Trip.TripStatus.PUBLISHED and t.startDate>=:today and t.availableSeats>0 and not exists (select b.bookingId from Booking b where b.user.id=:user and b.trip=t and b.bookingStatus<>it.unical.ea_project.domain.booking.Booking.BookingStatus.CANCELLED) order by t.startDate,t.tripId")
        List<Trip> recommendationCandidates(@Param("today") LocalDate today, @Param("user") Long user,
                        Pageable page);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT e FROM Trip e WHERE e.tripId = :id AND e.deletedAt IS NULL")
        Optional<Trip> findActiveForUpdate(@Param("id") Long id);

        List<Trip> findByCreatedByAndDeletedAtIsNull(User createdBy);

        List<Trip> findByStatusAndDeletedAtIsNull(TripStatus status);

        List<Trip> findByDestinationCountryContainingIgnoreCaseAndDeletedAtIsNull(String country);

        List<Trip> findByDestinationCityContainingIgnoreCaseAndDeletedAtIsNull(String city);

        // Trova i viaggi disponibili in un determinato intervallo di date
        List<Trip> findByStartDateGreaterThanEqualAndEndDateLessThanEqualAndDeletedAtIsNull(LocalDate startDate,
                        LocalDate endDate);

        // Trova i viaggi che hanno ancora posti disponibili e sono pubblicati
        List<Trip> findByStatusAndAvailableSeatsGreaterThanAndDeletedAtIsNull(TripStatus status, Integer minSeats);

        // Serve per recuperare un viaggio specifico solo se attivo (usiamo questo
        // metodo quando usiamo il soft delete "deletedAt")
        @Query("SELECT t FROM Trip t LEFT JOIN FETCH t.stages WHERE t.tripId = :tripId AND t.deletedAt IS NULL")
        Optional<Trip> findByTripIdAndDeletedAtIsNull(Long tripId);

        Boolean existsByTripIdAndDeletedAtIsNull(Long tripId);

        @Query("""
                        SELECT t
                        FROM Trip t
                        WHERE t.deletedAt IS NULL
                        AND t.status = :status
                        AND t.startDate >= :today
                        AND t.availableSeats > 0
                        ORDER BY COALESCE(t.averageRating, 0) DESC, t.tripId DESC
                        """)
        List<Trip> findTopTrips(
                        @Param("status") TripStatus status,
                        @Param("today") LocalDate today, Pageable pageable);

        @Query("""
                        SELECT new it.unical.ea_project.dto.home.TripSuggestionDTO(
                            t.tripId,
                            t.title,
                            CONCAT(
                                COALESCE(t.destinationCity, ''),
                                CASE
                                    WHEN t.destinationCity IS NOT NULL
                                         AND t.destinationCountry IS NOT NULL
                                    THEN ', '
                                    ELSE ''
                                END,
                                COALESCE(t.destinationCountry, '')
                            )
                        )
                        FROM Trip t
                        WHERE t.deletedAt IS NULL
                        AND t.status = :status
                        AND t.startDate >= :today
                        AND t.availableSeats > 0
                        AND (
                            LOWER(t.title) LIKE LOWER(CONCAT(:query, '%'))
                            OR LOWER(t.destinationCity) LIKE LOWER(CONCAT(:query, '%'))
                            OR LOWER(t.destinationCountry) LIKE LOWER(CONCAT(:query, '%'))
                        )
                        ORDER BY COALESCE(t.averageRating, 0) DESC, t.tripId DESC
                        """)
        List<TripSuggestionDTO> findSuggestions(@Param("query") String query,
                        @Param("status") TripStatus status, @Param("today") LocalDate today, Pageable pageable);

        @Query("""
                        SELECT t
                        FROM Trip t
                        WHERE t.deletedAt IS NULL
                        AND t.status = :status
                        AND t.startDate >= :today
                        AND t.availableSeats > 0
                        AND (
                            :query = ''
                            OR LOWER(t.title) LIKE CONCAT('%', LOWER(:query), '%')
                            OR LOWER(t.destinationCity) LIKE CONCAT('%', LOWER(:query), '%')
                            OR LOWER(t.destinationCountry) LIKE CONCAT('%', LOWER(:query), '%')
                        )
                        ORDER BY COALESCE(t.averageRating, 0) DESC, t.tripId DESC
                        """)
        List<Trip> searchPublished(@Param("query") String query, @Param("status") TripStatus status,
                        @Param("today") LocalDate today, Pageable pageable);

        @Query("""
                        SELECT t
                        FROM Trip t
                        WHERE t.deletedAt IS NULL
                        AND t.status = :status
                        AND t.startDate >= :today
                        AND t.availableSeats > 0
                        AND (
                            :query = ''
                            OR LOWER(t.title) LIKE CONCAT('%', LOWER(:query), '%')
                            OR LOWER(t.destinationCity) LIKE CONCAT('%', LOWER(:query), '%')
                            OR LOWER(t.destinationCountry) LIKE CONCAT('%', LOWER(:query), '%')
                        )
                        AND t.startDate <= :date
                        AND t.endDate >= :date
                        ORDER BY COALESCE(t.averageRating, 0) DESC, t.tripId DESC
                        """)
        List<Trip> searchPublishedFromDate(@Param("query") String query, @Param("status") TripStatus status,
                        @Param("date") LocalDate date, @Param("today") LocalDate today, Pageable pageable);

        @Query("""
                                SELECT COUNT(t) FROM Trip t
                                WHERE t.deletedAt IS NULL
                                AND t.status = :status
                                AND t.startDate >= :today
                                AND t.availableSeats > 0
                                AND (
                                    :query = ''
                                    OR LOWER(t.title) LIKE CONCAT('%', LOWER(:query), '%')
                                    OR LOWER(t.destinationCity) LIKE CONCAT('%', LOWER(:query), '%')
                                    OR LOWER(t.destinationCountry) LIKE CONCAT('%', LOWER(:query), '%')
                                )
                        """)
        long countSearchPublished(@Param("query") String query, @Param("status") TripStatus status,
                        @Param("today") LocalDate today);

        @Query("""
                                SELECT COUNT(t) FROM Trip t
                                WHERE t.deletedAt IS NULL
                                AND t.status = :status
                                AND t.startDate >= :today
                                AND t.availableSeats > 0
                                AND (
                                    :query = ''
                                    OR LOWER(t.title) LIKE CONCAT('%', LOWER(:query), '%')
                                    OR LOWER(t.destinationCity) LIKE CONCAT('%', LOWER(:query), '%')
                                    OR LOWER(t.destinationCountry) LIKE CONCAT('%', LOWER(:query), '%')
                                )
                                AND t.startDate <= :date
                                AND t.endDate >= :date
                        """)
        long countSearchPublishedFromDate(@Param("query") String query, @Param("status") TripStatus status,
                        @Param("date") LocalDate date, @Param("today") LocalDate today);
}
