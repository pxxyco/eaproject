package it.unical.ea_project.service.favorites;

import it.unical.ea_project.domain.favorites.ListTrips;
import java.util.List;

public interface ListTripsService {

    ListTrips addTripToList(Long listId, Long tripId);

    List<ListTrips> getTripsByListId(Long listId);

    void removeTripFromList(Long listId, Long tripId);

}
