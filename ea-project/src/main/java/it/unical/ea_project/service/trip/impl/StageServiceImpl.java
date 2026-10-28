package it.unical.ea_project.service.trip.impl;

import it.unical.ea_project.domain.trip.Stage.StageCategory;
import it.unical.ea_project.domain.trip.Stage;
import it.unical.ea_project.domain.trip.Trip;
import it.unical.ea_project.repository.trip.StageRepository;
import it.unical.ea_project.service.trip.StageService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StageServiceImpl implements StageService {

    private final StageRepository stageRepository;

    public StageServiceImpl(StageRepository stageRepository) {
        this.stageRepository = stageRepository;
    }

    @Override
    public List<Stage> getItineraryForTrip(Trip trip) {
        return stageRepository.findByTripOrderByDayAscOrderInDayAsc(trip);
    }

    @Override
    public List<Stage> getStagesByTripAndCategory(Trip trip, StageCategory category) {
        return stageRepository.findByTripAndCategory(trip, category);
    }

    @Override
    public List<Stage> getStagesForDay(Trip trip, Integer day) {
        return stageRepository.findByTripAndDayOrderByOrderInDayAsc(trip, day);
    }

    @Override
    @Transactional
    public Stage addStageToTrip(Trip trip, Stage stage) {
        trip.addStage(stage);
        return stageRepository.save(stage);
    }

    @Override
    @Transactional
    public void removeStageFromTrip(Trip trip, Stage stage) {
        trip.removeStage(stage);
        stageRepository.delete(stage);
    }
}
