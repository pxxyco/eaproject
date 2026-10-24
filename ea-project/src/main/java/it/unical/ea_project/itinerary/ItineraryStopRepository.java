package it.unical.ea_project.itinerary;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ItineraryStopRepository extends JpaRepository<ItineraryStop, Long> {
    List<ItineraryStop> findByListIdOrderByDayAscPositionAscIdAsc(Long list);

    long countByListId(Long list);
}
