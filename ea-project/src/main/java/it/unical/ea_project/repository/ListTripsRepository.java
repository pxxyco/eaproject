package it.unical.ea_project.repository;

import it.unical.ea_project.domain.ListTrips;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListTripsRepository extends JpaRepository<ListTrips, ListTrips.FavoriteListTripId> {
    @org.springframework.data.jpa.repository.Query("select distinct t from ListTrips l, Trip t where l.tripId=t.tripId and l.favoriteList.user.id=:user and l.favoriteList.deleted=false and t.deletedAt is null")
    List<it.unical.ea_project.domain.Trip> preferenceTrips(
            @org.springframework.data.repository.query.Param("user") Long user,
            org.springframework.data.domain.Pageable page);

    // recupera tutti i viaggi associati a una specifica lista
    List<ListTrips> findByFavoriteListId(Long listId);

    // controlla se un viaggio è già presente in una specifica lista
    boolean existsByFavoriteListIdAndTripId(Long listId, Long tripId);
}