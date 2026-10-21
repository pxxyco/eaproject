package it.unical.ea_project.service.impl;

import it.unical.ea_project.domain.FavoriteList;
import it.unical.ea_project.domain.ListTrips;
import it.unical.ea_project.repository.FavoriteListRepository;
import it.unical.ea_project.repository.ListTripsRepository;
import it.unical.ea_project.repository.TripRepository;
import it.unical.ea_project.domain.Trip;
import it.unical.ea_project.service.ListTripsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ListTripsServiceImpl implements ListTripsService {

    private final ListTripsRepository listTripsRepository;
    private final FavoriteListRepository favoriteListRepository;
    private final TripRepository tripRepository;

    @Override
    @Transactional
    public ListTrips addTripToList(Long listId, Long tripId) {
        FavoriteList favoriteList = favoriteListRepository.findById(listId)
                .filter(list -> !list.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Lista dei preferiti non trovata o eliminata con ID: " + listId));

        Trip trip = tripRepository.findByTripIdAndDeletedAtIsNull(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Viaggio non trovato o non disponibile."));
        if (trip.getStatus() != Trip.TripStatus.PUBLISHED) {
            throw new IllegalArgumentException("È possibile aggiungere solo viaggi pubblicati.");
        }

        if (listTripsRepository.existsByFavoriteListIdAndTripId(listId, tripId)) {
            throw new IllegalStateException("Il viaggio con ID " + tripId + " è già presente in questa lista.");
        }

        ListTrips listTrips = new ListTrips();
        listTrips.setFavoriteList(favoriteList);
        listTrips.setTripId(tripId);
        listTrips.setAddedAt(LocalDateTime.now());

        return listTripsRepository.save(listTrips);

    }

    @Override
    @Transactional(readOnly = true)
    public List<ListTrips> getTripsByListId(Long listId) {

        if (!favoriteListRepository.existsById(listId)) {
            throw new IllegalArgumentException("Lista dei preferiti non trovata con ID: " + listId);
        }

        return listTripsRepository.findByFavoriteListId(listId);
    
    }

    @Override
    @Transactional
    public void removeTripFromList(Long listId, Long tripId) {

        ListTrips.FavoriteListTripId compositeId = new ListTrips.FavoriteListTripId(listId, tripId);

        ListTrips listTrips = listTripsRepository.findById(compositeId)
                .orElseThrow(() -> new IllegalArgumentException("Associazione non trovata tra la lista " + listId + " e il viaggio " + tripId));

        listTripsRepository.delete(listTrips);

    }

}