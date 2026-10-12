package it.unical.ea_project.service;

import it.unical.ea_project.domain.Activity;
import it.unical.ea_project.dto.ActivityDTO;
import it.unical.ea_project.dto.home.ActivityHomeDTO;
import it.unical.ea_project.dto.home.ActivitySuggestionDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ActivityService {

    List<Activity> getAllActivities();
    Optional<Activity> getActivityById(Long id);
    Activity createActivity(Activity activity, Long creatorId);
    Activity updateActivity(Long id, Activity details);
    void deleteActivity(Long id);
    List<Activity> getActivitiesByCategory(Activity.Category category);
    List<Activity> getTopActivities(int page, int size);

    Optional<ActivityDTO> getActivityDtoById(Long id);
    List<ActivityDTO> getTopActivityDtos(int page, int size);
    List<ActivityDTO> getActivityDtosByCategory(Activity.Category category);
    List<ActivityDTO> getActivityDtosByCreator(Long creatorId);

    List<ActivityHomeDTO> getTopActivityHomeDtos(int page, int size);

    List<ActivityHomeDTO> searchActivityHomeDtos(
            String query,
            Activity.Category category,
            LocalDate date,
            int page,
            int size
    );

    List<ActivitySuggestionDTO> getSuggestions(String query, int limit);
}