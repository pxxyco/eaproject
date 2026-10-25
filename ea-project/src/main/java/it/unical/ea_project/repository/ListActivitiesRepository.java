package it.unical.ea_project.repository;

import it.unical.ea_project.domain.ListActivities;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ListActivitiesRepository extends JpaRepository<ListActivities, ListActivities.ListActivityId> {

    List<ListActivities> findByFavoriteListId(Long listId);

    boolean existsByFavoriteListIdAndActivityId(Long listId, Long activityId);

}