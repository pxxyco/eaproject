package it.unical.ea_project.service.trip;

import it.unical.ea_project.domain.trip.Trip;
import it.unical.ea_project.domain.user.User;
import it.unical.ea_project.dto.home.TripHomeDTO;
import it.unical.ea_project.dto.home.TripSuggestionDTO;
import it.unical.ea_project.dto.trip.TripDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TripService {

    List<Trip> getTripsByUser(User user);

    List<TripDTO> getTripDtosByUser(User user);

    List<Trip> getPublishedTrips();

    List<Trip> searchByCountry(String country);

    List<Trip> searchByCity(String city);

    List<Trip> getTripsInDateRange(LocalDate start, LocalDate end);

    List<Trip> getAvailableTrips();

    Optional<Trip> getTripById(Long id);

    Trip createTrip(Trip trip, Long creatorId);

    Trip updateTrip(Long id, Trip details);

    Trip saveTrip(Trip trip);

    void deleteTripLogically(Trip trip);

    String exportTripToIcs(Long tripId, Long requester);

    void deleteTrip(Long id);

    List<Trip> getTopTrips(int page, int size);

    Optional<TripDTO> getTripDtoById(Long id);

    List<TripDTO> getPublishedTripDtos();

    List<TripDTO> getAvailableTripDtos();

    List<TripDTO> searchByCountryDtos(String country);

    List<TripDTO> searchByCityDtos(String city);

    List<TripDTO> getTripsInDateRangeDtos(LocalDate start, LocalDate end);

    TripDTO createTripDto(Trip trip, Long creatorId);

    TripDTO updateTripDto(Long id, Trip details);

    List<TripDTO> getTopTripDtos(int page, int size);

    List<TripHomeDTO> searchTripHomeDtos(String query, LocalDate date, int page, int size);

    long countHomeTrips(String query, LocalDate date);

    List<TripSuggestionDTO> getSuggestions(String query, int limit);

}
