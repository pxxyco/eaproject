package it.unical.ea_project.repository;

import it.unical.ea_project.domain.Activity;
import it.unical.ea_project.domain.User;
import it.unical.ea_project.dto.home.ActivitySuggestionDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityRepository
        extends JpaRepository<Activity, Long> {
    @Query("select a from Activity a where a.deletedAt is null and a.status=it.unical.ea_project.domain.Activity.Status.PUBLISHED and a.startDate>:now and a.availableSeats>0 and not exists (select b.bookingId from Booking b where b.user.id=:user and b.activity=a and b.bookingStatus<>it.unical.ea_project.domain.Booking.BookingStatus.CANCELLED) order by a.startDate,a.activityId")
    List<Activity> recommendationCandidates(@Param("now") java.time.LocalDateTime now, @Param("user") Long user,
            org.springframework.data.domain.Pageable page);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Activity e WHERE e.activityId = :id AND e.deletedAt IS NULL")
    Optional<Activity> findActiveForUpdate(@Param("id") Long id);

    List<Activity> findByCreatedBy(User createdBy);

    List<Activity> findByCreatedByIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long creatorId);

    List<Activity> findByDeletedAtIsNull();

    Optional<Activity> findByActivityIdAndDeletedAtIsNull(
            Long activityId);

    @Query("select a from Activity a where a.deletedAt is null and a.status=it.unical.ea_project.domain.Activity.Status.PUBLISHED and a.startDate>:now and a.availableSeats>0 order by a.averageRating desc, a.activityId desc")
    List<Activity> findBookableOrderByRating(
            @Param("now") LocalDateTime now, Pageable pageable);

    List<Activity> findByDeletedAtIsNullAndCategory(
            Activity.Category category);

    @Query("""
        SELECT new it.unical.ea_project.dto.home.ActivitySuggestionDTO(
            a.activityId,
            a.title,
            a.city
        )
        FROM Activity a
        WHERE a.deletedAt IS NULL
    AND a.status = it.unical.ea_project.domain.Activity.Status.PUBLISHED
    AND a.startDate > :now
    AND a.availableSeats > 0
        AND (
            LOWER(a.title) LIKE LOWER(CONCAT(:query, '%'))
            OR LOWER(a.city) LIKE LOWER(CONCAT(:query, '%'))
        )
        """)
    List<ActivitySuggestionDTO> findSuggestions(
            @Param("query") String query,
            @Param("now") LocalDateTime now, Pageable pageable
    );

    @Query("""
    SELECT a
    FROM Activity a
    WHERE a.deletedAt IS NULL
    AND a.status = it.unical.ea_project.domain.Activity.Status.PUBLISHED
    AND a.startDate > :now
    AND a.availableSeats > 0
    AND (
        cast(:category as String) IS NULL
        OR a.category = :category
    )
    AND (
        :query = ''
        OR LOWER(a.title) LIKE CONCAT('%', LOWER(:query), '%')
        OR LOWER(a.city) LIKE CONCAT('%', LOWER(:query), '%')
    )
    AND (
        cast(:dayStart as LocalDateTime) IS NULL
        OR (a.startDate < :nextDayStart AND a.endDate >= :dayStart)
    )
    ORDER BY a.averageRating DESC, a.activityId DESC
    """)
    List<Activity> search(
            @Param("query") String query,
            @Param("category") Activity.Category category,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("nextDayStart") LocalDateTime nextDayStart,
            @Param("now") LocalDateTime now, Pageable pageable
    );

    @Query("""
    SELECT a
    FROM Activity a
    WHERE a.deletedAt IS NULL
    AND a.status = it.unical.ea_project.domain.Activity.Status.PUBLISHED
    AND a.startDate > :now
    AND a.availableSeats > 0
    AND (
        cast(:category as String) IS NULL
        OR a.category = :category
    )
    AND (
        :query = ''
        OR LOWER(a.title) LIKE CONCAT('%', LOWER(:query), '%')
        OR LOWER(a.city) LIKE CONCAT('%', LOWER(:query), '%')
    )
    ORDER BY a.averageRating DESC, a.activityId DESC
    """)
    List<Activity> searchWithoutDate(
            @Param("query") String query,
            @Param("category") Activity.Category category,
            @Param("now") LocalDateTime now, Pageable pageable
    );

    @Query("""
    SELECT COUNT(a) FROM Activity a
    WHERE a.deletedAt IS NULL
    AND a.status = it.unical.ea_project.domain.Activity.Status.PUBLISHED
    AND a.startDate > :now
    AND a.availableSeats > 0
    AND (
        cast(:category as String) IS NULL
        OR a.category = :category
    )
    AND (
        :query = ''
        OR LOWER(a.title) LIKE CONCAT('%', LOWER(:query), '%')
        OR LOWER(a.city) LIKE CONCAT('%', LOWER(:query), '%')
    )
    AND (
        cast(:dayStart as LocalDateTime) IS NULL
        OR (a.startDate < :nextDayStart AND a.endDate >= :dayStart)
    )
    """)
    long countSearch(
            @Param("query") String query,
            @Param("category") Activity.Category category,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("nextDayStart") LocalDateTime nextDayStart, @Param("now") LocalDateTime now);

    @Query("""
    SELECT COUNT(a) FROM Activity a
    WHERE a.deletedAt IS NULL
    AND a.status = it.unical.ea_project.domain.Activity.Status.PUBLISHED
    AND a.startDate > :now
    AND a.availableSeats > 0
    AND (
        cast(:category as String) IS NULL
        OR a.category = :category
    )
    AND (
        :query = ''
        OR LOWER(a.title) LIKE CONCAT('%', LOWER(:query), '%')
        OR LOWER(a.city) LIKE CONCAT('%', LOWER(:query), '%')
    )
    """)
    long countSearchWithoutDate(
            @Param("query") String query,
            @Param("category") Activity.Category category, @Param("now") LocalDateTime now);

    @Query("select count(a) from Activity a where a.deletedAt is null and a.status=it.unical.ea_project.domain.Activity.Status.PUBLISHED and a.startDate>:now and a.availableSeats>0")
    long countBookable(@Param("now") LocalDateTime now);
}
