package it.unical.ea_project.service.trip;

import it.unical.ea_project.domain.trip.Stage.StageCategory;
import it.unical.ea_project.domain.trip.Stage;
import it.unical.ea_project.domain.trip.Trip;
import java.util.List;

public interface StageService {

    List<Stage> getItineraryForTrip(Trip trip);

    List<Stage> getStagesByTripAndCategory(Trip trip, StageCategory category);

    List<Stage> getStagesForDay(Trip trip, Integer day);

    Stage addStageToTrip(Trip trip, Stage stage);

    void removeStageFromTrip(Trip trip, Stage stage);

}
