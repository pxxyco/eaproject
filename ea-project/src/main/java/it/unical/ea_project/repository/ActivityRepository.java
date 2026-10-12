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

    List<Activity> findByCreatedBy(User createdBy);

    List<Activity> findByCreatedByIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long creatorId);

    List<Activity> findByDeletedAtIsNull();

    Optional<Activity> findByActivityIdAndDeletedAtIsNull(
            Long activityId
    );

    List<Activity> findByDeletedAtIsNullOrderByAverageRatingDesc(
            Pageable pageable
    );

    List<Activity> findByDeletedAtIsNullAndCategory(
            Activity.Category category
    );

    @Query("""
        SELECT new it.unical.ea_project.dto.home.ActivitySuggestionDTO(
            a.activityId,
            a.title,
            a.city
        )
        FROM Activity a
        WHERE a.deletedAt IS NULL
        AND (
            LOWER(a.title) LIKE LOWER(CONCAT(:query, '%'))
            OR LOWER(a.city) LIKE LOWER(CONCAT(:query, '%'))
        )
        """)
    List<ActivitySuggestionDTO> findSuggestions(
            @Param("query") String query,
            Pageable pageable
    );

    @Query("""
    SELECT a
    FROM Activity a
    WHERE a.deletedAt IS NULL
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
    ORDER BY a.averageRating DESC
    """)
    List<Activity> search(
            @Param("query") String query,
            @Param("category") Activity.Category category,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("nextDayStart") LocalDateTime nextDayStart,
            Pageable pageable
    );

    @Query("""
    SELECT a
    FROM Activity a
    WHERE a.deletedAt IS NULL
    AND (
        cast(:category as String) IS NULL
        OR a.category = :category
    )
    AND (
        :query = ''
        OR LOWER(a.title) LIKE CONCAT('%', LOWER(:query), '%')
        OR LOWER(a.city) LIKE CONCAT('%', LOWER(:query), '%')
    )
    ORDER BY a.averageRating DESC
    """)
    List<Activity> searchWithoutDate(
            @Param("query") String query,
            @Param("category") Activity.Category category,
            Pageable pageable
    );
}