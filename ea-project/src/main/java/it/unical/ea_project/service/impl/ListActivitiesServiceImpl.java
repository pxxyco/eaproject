package it.unical.ea_project.service.impl;

import it.unical.ea_project.domain.Activity;
import it.unical.ea_project.domain.FavoriteList;
import it.unical.ea_project.domain.ListActivities;
import it.unical.ea_project.repository.ActivityRepository;
import it.unical.ea_project.repository.FavoriteListRepository;
import it.unical.ea_project.repository.ListActivitiesRepository;
import it.unical.ea_project.service.ListActivitiesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ListActivitiesServiceImpl implements ListActivitiesService {

    private final ListActivitiesRepository listActivitiesRepository;
    private final FavoriteListRepository favoriteListRepository;
    private final ActivityRepository activityRepository;

    @Override
    @Transactional
    public ListActivities addActivityToList(Long listId, Long activityId) {

        FavoriteList list = favoriteListRepository.findById(listId)
                .filter(l -> !l.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Lista non trovata o eliminata."));
        Activity activity = activityRepository.findByActivityIdAndDeletedAtIsNull(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Attività non trovata o non disponibile."));
        if (activity.getStatus() != Activity.Status.PUBLISHED) {
            throw new IllegalArgumentException("È possibile aggiungere solo attività pubblicate.");
        }

        if (listActivitiesRepository.existsByFavoriteListIdAndActivityId(listId, activityId)) {
            throw new IllegalStateException("L'attività è già presente nella lista.");
        }

        ListActivities association = new ListActivities();
        association.setFavoriteList(list);
        association.setActivityId(activityId);
        association.setAddedAt(LocalDateTime.now());
        return listActivitiesRepository.save(association);

    }

    @Override
    @Transactional(readOnly = true)
    public List<ListActivities> getActivitiesByListId(Long listId) {

        if (!favoriteListRepository.existsById(listId)) {
            throw new IllegalArgumentException("Lista non trovata.");
        }

        return listActivitiesRepository.findByFavoriteListId(listId);

    }

    @Override
    @Transactional
    public void removeActivityFromList(Long listId, Long activityId) {

        ListActivities.ListActivityId key = new ListActivities.ListActivityId(listId, activityId);
        ListActivities association = listActivitiesRepository.findById(key)
                .orElseThrow(() -> new IllegalArgumentException("L'attività non è presente in questa lista."));
        listActivitiesRepository.delete(association);

    }

}