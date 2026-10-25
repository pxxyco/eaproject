package it.unical.ea_project.service;

import it.unical.ea_project.domain.ListActivities;
import java.util.List;

public interface ListActivitiesService {

    ListActivities addActivityToList(Long listId, Long activityId);

    List<ListActivities> getActivitiesByListId(Long listId);

    void removeActivityFromList(Long listId, Long activityId);

}