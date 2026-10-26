package it.unical.ea_project.service.favorites;

import it.unical.ea_project.domain.favorites.ListActivities;
import java.util.List;

public interface ListActivitiesService {

    ListActivities addActivityToList(Long listId, Long activityId);

    List<ListActivities> getActivitiesByListId(Long listId);

    void removeActivityFromList(Long listId, Long activityId);

}
