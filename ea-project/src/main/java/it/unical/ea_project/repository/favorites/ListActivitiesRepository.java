package it.unical.ea_project.repository.favorites;

import it.unical.ea_project.domain.activity.Activity;
import it.unical.ea_project.domain.favorites.ListActivities;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ListActivitiesRepository extends JpaRepository<ListActivities, ListActivities.ListActivityId> {
    @Query("select distinct a from ListActivities l, Activity a where l.activityId=a.activityId and l.favoriteList.user.id=:user and l.favoriteList.deleted=false and a.deletedAt is null")
    List<Activity> preferenceActivities(
            @Param("user") Long user,
            Pageable page);

    List<ListActivities> findByFavoriteListId(Long listId);

    boolean existsByFavoriteListIdAndActivityId(Long listId, Long activityId);

}
