package it.unical.ea_project.repository.activity;

import it.unical.ea_project.domain.activity.Activity;
import it.unical.ea_project.domain.activity.ActivityImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityImageRepository extends JpaRepository<ActivityImage, Long> {

    // trova tutte le immagini associate a una specifica attività, ordinate per
    // orderIndex
    List<ActivityImage> findByActivityOrderByOrderIndexAsc(Activity activity);

    // Recupera le immagini di PIÙ attività con una sola query
    @Query("""
            SELECT ai
            FROM ActivityImage ai
            WHERE ai.activity.activityId IN :activityIds
            ORDER BY
                ai.activity.activityId ASC,
                ai.orderIndex ASC
            """)
    List<ActivityImage> findByActivityIds(
            @Param("activityIds") List<Long> activityIds);

}
