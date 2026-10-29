package it.unical.ea_project.repository.favorites;

import it.unical.ea_project.domain.favorites.ListTrips;
import it.unical.ea_project.domain.trip.Trip;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ListTripsRepository extends JpaRepository<ListTrips, ListTrips.FavoriteListTripId> {
    @Query("select distinct t from ListTrips l, Trip t where l.tripId=t.tripId and l.favoriteList.user.id=:user and l.favoriteList.deleted=false and t.deletedAt is null")
    List<Trip> preferenceTrips(
            @Param("user") Long user,
            Pageable page);

    // recupera tutti i viaggi associati a una specifica lista
    List<ListTrips> findByFavoriteListId(Long listId);

    // controlla se un viaggio è già presente in una specifica lista
    boolean existsByFavoriteListIdAndTripId(Long listId, Long tripId);
}
