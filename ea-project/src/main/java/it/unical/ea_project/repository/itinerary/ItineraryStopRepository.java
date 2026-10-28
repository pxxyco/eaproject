package it.unical.ea_project.repository.itinerary;

import it.unical.ea_project.domain.itinerary.ItineraryStop;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItineraryStopRepository extends JpaRepository<ItineraryStop, Long> {
    List<ItineraryStop> findByListIdOrderByDayAscPositionAscIdAsc(Long list);

    long countByListId(Long list);
}
