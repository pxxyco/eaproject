package it.unical.ea_project.repository;

import it.unical.ea_project.domain.Trip;
import it.unical.ea_project.domain.Trip.TripStatus;
import it.unical.ea_project.domain.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Trip e WHERE e.tripId = :id AND e.deletedAt IS NULL")
    Optional<Trip> findActiveForUpdate(@Param("id") Long id);


    List<Trip> findByCreatedByAndDeletedAtIsNull(User createdBy);
    List<Trip> findByStatusAndDeletedAtIsNull(TripStatus status);
    List<Trip> findByDestinationCountryContainingIgnoreCaseAndDeletedAtIsNull(String country);
    List<Trip> findByDestinationCityContainingIgnoreCaseAndDeletedAtIsNull(String city);

    // Trova i viaggi disponibili in un determinato intervallo di date
    List<Trip> findByStartDateGreaterThanEqualAndEndDateLessThanEqualAndDeletedAtIsNull(LocalDate startDate, LocalDate endDate);

    // Trova i viaggi che hanno ancora posti disponibili e sono pubblicati
    List<Trip> findByStatusAndAvailableSeatsGreaterThanAndDeletedAtIsNull(TripStatus status, Integer minSeats);

    // Serve per recuperare un viaggio specifico solo se attivo (usiamo questo metodo quando usiamo il soft delete "deletedAt")
    @Query("SELECT t FROM Trip t LEFT JOIN FETCH t.stages WHERE t.tripId = :tripId AND t.deletedAt IS NULL")
    Optional<Trip> findByTripIdAndDeletedAtIsNull(Long tripId);

    Boolean existsByTripIdAndDeletedAtIsNull(Long tripId);




    @Query("""
        SELECT t
        FROM Trip t
        WHERE t.deletedAt IS NULL
        AND t.status = :status
        ORDER BY COALESCE(t.averageRating, 0) DESC, t.tripId DESC
        """)
    List<Trip> findTopTrips(
            @Param("status") TripStatus status,
            Pageable pageable
    );

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
        AND (
            LOWER(t.title) LIKE LOWER(CONCAT(:query, '%'))
            OR LOWER(t.destinationCity) LIKE LOWER(CONCAT(:query, '%'))
            OR LOWER(t.destinationCountry) LIKE LOWER(CONCAT(:query, '%'))
        )
        ORDER BY COALESCE(t.averageRating, 0) DESC, t.tripId DESC
        """)
    List<it.unical.ea_project.dto.home.TripSuggestionDTO> findSuggestions(@Param("query") String query, @Param("status") TripStatus status, Pageable pageable);

    @Query("""
        SELECT t
        FROM Trip t
        WHERE t.deletedAt IS NULL
        AND t.status = :status
        AND (
            :query = ''
            OR LOWER(t.title) LIKE CONCAT('%', LOWER(:query), '%')
            OR LOWER(t.destinationCity) LIKE CONCAT('%', LOWER(:query), '%')
            OR LOWER(t.destinationCountry) LIKE CONCAT('%', LOWER(:query), '%')
        )
        ORDER BY COALESCE(t.averageRating, 0) DESC, t.tripId DESC
        """)
    List<Trip> searchPublished(@Param("query") String query, @Param("status") TripStatus status, Pageable pageable);

    @Query("""
        SELECT t
        FROM Trip t
        WHERE t.deletedAt IS NULL
        AND t.status = :status
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
    List<Trip> searchPublishedFromDate(@Param("query") String query, @Param("status") TripStatus status, @Param("date") LocalDate date, Pageable pageable);
}