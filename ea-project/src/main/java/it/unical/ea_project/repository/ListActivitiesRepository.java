package it.unical.ea_project.repository;

import it.unical.ea_project.domain.ListActivities;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ListActivitiesRepository extends JpaRepository<ListActivities, ListActivities.ListActivityId> {
    @org.springframework.data.jpa.repository.Query("select distinct a from ListActivities l, Activity a where l.activityId=a.activityId and l.favoriteList.user.id=:user and l.favoriteList.deleted=false and a.deletedAt is null")
    List<it.unical.ea_project.domain.Activity> preferenceActivities(
            @org.springframework.data.repository.query.Param("user") Long user,
            org.springframework.data.domain.Pageable page);

    List<ListActivities> findByFavoriteListId(Long listId);

    boolean existsByFavoriteListIdAndActivityId(Long listId, Long activityId);

}